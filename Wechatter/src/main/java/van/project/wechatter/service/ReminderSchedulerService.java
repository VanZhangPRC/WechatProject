package van.project.wechatter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import van.project.wechat.wechatPublic.services.WechatApiExecutor;
import van.project.wechat.wechatPublic.services.api.TemplateMessageSendReq;
import van.project.wechatter.entity.Reminder;
import van.project.wechatter.entity.enums.ReminderStatus;
import van.project.wechatter.mapper.ReminderMapper;
import van.project.wechatter.wechat.WechatHelper;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderSchedulerService {

    private final TaskScheduler taskScheduler;
    private final ReminderMapper reminderMapper;
    private final WechatApiExecutor wechatApiExecutor;
    private final ApplicationContext context;
    private final WechatHelper wechatHelper;

    private ChatClient chatClient;
    private final Object chatClientLock = new Object();
    private ChatClient getChatClient() {
        if (chatClient != null)
            return chatClient;

        synchronized (chatClientLock) {
            if (chatClient != null)
                return chatClient;

            chatClient = ChatClient
                    .builder(context.getBean(ChatModel.class))
                    .defaultOptions(ChatOptions.builder().temperature(0.8).build())
                    .defaultToolCallbacks(context.getBean(ToolCallbackProvider.class))
                    .defaultSystem("""
                            你是一个处理信息的智能子进程，负责获取数据并返回。
                            你的父级进程是一个智能助理，负责定时执行任务并将结果推送给用户：
                            1. 达到执行时间点
                            2. 执行任务，比如获取相关数据等
                            3. 将结果推送给用户
                            其中，2点是你需要协助父级进程完成的工作，比如利用工具获取信息并返回，
                            注意：上述步骤中你位于2点的位置，3点将结果推送给用户由父级进程完成，你不应该把
                            你需要根据用户的描述，利用目前可以使用 tool 完成获取、整理相关信息，结论长度不超过1000
                            """)
                    .build();
        }
        return chatClient;
    }

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /** 系统启动时加载所有启用中的提醒，过期的取消，未来的加入调度 */
    @PostConstruct
    public void init() {
        List<Reminder> actives = reminderMapper.selectList(
                new LambdaQueryWrapper<Reminder>().eq(Reminder::getStatus, ReminderStatus.ACTIVE));
        log.info("Loading {} active reminders on startup", actives.size());
        for (Reminder r : actives) {
            scheduleOrCancel(r);
        }
    }

    /** 新增提醒时调用，加入调度 */
    public void schedule(Reminder reminder) {
        scheduleOrCancel(reminder);
    }

    /** 取消调度（不修改数据库状态，仅从调度 Map 中移除） */
    public void cancelScheduled(Long id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }


    /** 当前已调度的提醒 ID 集合 */
    public Map<Reminder, LocalDateTime> getScheduledInfo() {
        return getScheduledInfo(null);
    }

    /** 当前已调度的提醒 ID 集合 */
    public Map<Reminder, LocalDateTime> getScheduledInfo(String openId) {
        Map<Reminder, LocalDateTime> info = new ConcurrentHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<Reminder>().eq(Reminder::getStatus, ReminderStatus.ACTIVE);
        if (openId != null) {
            wrapper.eq(Reminder::getOpenId, openId);
        }

        Map<Long, Reminder> reminderMap = reminderMapper
                .selectList(wrapper)
                .stream()
                .collect(Collectors.toMap(Reminder::getId, Function.identity()));

        scheduledTasks.forEach((id, future) -> {
            long delayMs = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
            if (delayMs > 0 && reminderMap.containsKey(id)) {
                info.put(reminderMap.get(id), now.plusNanos(delayMs * 1_000_000));
            }
        });
        return info;
    }

    // ──────────────── 内部逻辑 ────────────────

    private void scheduleOrCancel(Reminder r) {
        LocalDateTime next = calcNextTrigger(r);
        if (next == null) {
            log.info("No future trigger for reminder [{}], cancelling", r.getId());
            r.setStatus(ReminderStatus.CANCELLED);
            r.setUpdateTime(LocalDateTime.now());
            reminderMapper.updateById(r);
            cancelScheduled(r.getId());
            return;
        }
        doSchedule(r, next);
    }

    private void doSchedule(Reminder r, LocalDateTime triggerTime) {
        cancelScheduled(r.getId());
        Instant triggerInstant = triggerTime.atZone(ZoneId.systemDefault()).toInstant();
        ScheduledFuture<?> future = taskScheduler.schedule(() -> execute(r.getId()), triggerInstant);
        scheduledTasks.put(r.getId(), future);
        log.info("Reminder [{}] scheduled at {}", r.getId(), triggerTime);
    }

    private void execute(Long id) {
        Reminder r = reminderMapper.selectById(id);
        if (r == null || r.getStatus() != ReminderStatus.ACTIVE) {
            scheduledTasks.remove(id);
            return;
        }

        log.info("Executing reminder [{}]: openId={}, content={}", id, r.getOpenId(), r.getContent());
        String content = r.getContent();
        if (r.isAiAssisted()) {
            AIAssistedResult assistedResult = getChatClient().prompt()
                    .system(s -> s.text("当前用户openId[`{openId}`]").param("openId", r.getOpenId()))
                    .user(content)
                    .call()
                    .responseEntity(AIAssistedResult.class)
                    .entity();
            wechatApiExecutor.sendTemplateMessage(wechatHelper.buildNotifyTemplateMessage(r.getOpenId(), assistedResult.title, assistedResult.getContent()));
        } else {
            wechatApiExecutor.sendTemplateMessage(
                    TemplateMessageSendReq
                            .builder()
                            .touser(r.getOpenId())
                            .template_id(wechatHelper.getNotifyTemplateId())
                            .data(Collections.singletonMap("content", new TemplateMessageSendReq.DataElement(content)))
                            .build()
            );
        }

        LocalDateTime next = calcNextTrigger(r);
        if (next == null) {
            r.setStatus(ReminderStatus.COMPLETED);
            reminderMapper.updateById(r);
            scheduledTasks.remove(id);
            return;
        }
        doSchedule(r, next);
    }

    /** 计算下一次触发时间，返回 null 表示没有未来的触发点 */
    public LocalDateTime calcNextTrigger(Reminder r) {

        LocalTime time = r.getTargetTime();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime targetDateTime;
        switch (r.getReminderType()) {
            case SPECIFIC_DATE -> {
                targetDateTime = LocalDateTime.of(r.getTargetDate(), time);
                if (now.isAfter(targetDateTime)) {
                    return null;
                } else {
                    return targetDateTime;
                }
            }
            case MONTHLY_DAY -> {
                targetDateTime = LocalDateTime.of(LocalDate.now().withDayOfMonth(r.getDayOfMonth()), time);
                if (now.isAfter(targetDateTime)) {
                    return targetDateTime.plusMonths(1L);
                } else {
                    return targetDateTime;
                }
            }
            case MONTHLY_LAST_N_DAYS -> {
                targetDateTime = LocalDateTime.of(LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(r.getLastDayOfMonth()), time);
                if (now.isAfter(targetDateTime)) {
                    return targetDateTime.plusMonths(2L).minusDays(r.getLastDayOfMonth());
                } else {
                    return targetDateTime;
                }
            }
            case EVERY_DAY -> {
                targetDateTime = LocalDateTime.of(LocalDate.now(), r.getTargetTime());
                if (now.isAfter(targetDateTime)) {
                    return targetDateTime.plusDays(1);
                }
                return targetDateTime;
            }
            case EVERY_WEEK -> {
                targetDateTime = LocalDateTime.of(LocalDate.now(), r.getTargetTime());
                String planDayOfWeek = r.getDayOfWeek();

                int planDayOfWeekCalc = 0;
                for (int c = 0; c < planDayOfWeek.length(); c++) {
                    planDayOfWeekCalc |= (1 << (planDayOfWeek.charAt(c) - '0'));
                }

                for (int i = 0; i < 7; i++) {
                    int nextDayOfWeek = 1 << (targetDateTime.getDayOfWeek().getValue());
                    if ((planDayOfWeekCalc & nextDayOfWeek) > 0 && targetDateTime.isAfter(now)) {
                        return targetDateTime;
                    }
                    targetDateTime = targetDateTime.plusDays(1);
                }

                return null;
            }
            default -> {
                return null;
            }
        }
    }

    @Data
    public static class AIAssistedResult {
        private String title;
        private String content;
    }
}