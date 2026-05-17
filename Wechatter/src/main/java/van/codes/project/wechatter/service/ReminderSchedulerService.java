package van.codes.project.wechatter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import van.codes.project.wechatter.entity.Reminder;
import van.codes.project.wechatter.entity.enums.ReminderStatus;
import van.codes.project.wechatter.entity.enums.ReminderType;
import van.codes.project.wechatter.mapper.ReminderMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderSchedulerService {

    private final TaskScheduler taskScheduler;
    private final ReminderMapper reminderMapper;
    private final WeChatApiService weChatApiService;

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

    /** 当前已调度的任务数 */
    public int getScheduledCount() {
        return scheduledTasks.size();
    }

    /** 当前已调度的提醒 ID 集合 */
    public Map<Long, LocalDateTime> getScheduledInfo() {
        Map<Long, LocalDateTime> info = new ConcurrentHashMap<>();
        scheduledTasks.forEach((id, future) -> {
            long delayMs = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
            if (delayMs > 0) {
                info.put(id, LocalDateTime.now().plusNanos(delayMs * 1_000_000));
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
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> execute(r.getId()), triggerInstant);
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
        weChatApiService.sendReminder(r.getOpenId(), r.getContent());

        int executed = (r.getExecutedCount() != null ? r.getExecutedCount() : 0) + 1;
        r.setExecutedCount(executed);
        r.setUpdateTime(LocalDateTime.now());

        Integer repeatCount = r.getRepeatCount();
        if (repeatCount != null && repeatCount > 0 && executed >= repeatCount) {
            r.setStatus(ReminderStatus.COMPLETED);
            reminderMapper.updateById(r);
            scheduledTasks.remove(id);
            log.info("Reminder [{}] completed after {} executions", id, executed);
            return;
        }

        reminderMapper.updateById(r);

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
    LocalDateTime calcNextTrigger(Reminder r) {
        int executed = r.getExecutedCount() != null ? r.getExecutedCount() : 0;
        Integer repeatCount = r.getRepeatCount();
        LocalTime time = r.getTargetTime();
        LocalDateTime now = LocalDateTime.now();

        if (r.getReminderType() == ReminderType.SPECIFIC_DATE) {
            // 类型1：指定日期，按天偏移
            LocalDateTime first = LocalDateTime.of(r.getTargetDate(), time);
            return findNext(first, executed, repeatCount, Duration.ofDays(1), now);
        }

        if (r.getReminderType() == ReminderType.MONTHLY_DAY) {
            // 类型2：每月几号，按月偏移
            LocalDateTime first = findMonthlyBase(r.getTargetDay(), time, now);
            return findNext(first, executed, repeatCount, null, now); // 按月偏移在 findNext 中特殊处理
        }

        if (r.getReminderType() == ReminderType.MONTHLY_LAST_N_DAYS) {
            // 类型3：每月倒数几天，按月偏移
            YearMonth ym = YearMonth.from(now);
            int day = Math.max(1, ym.lengthOfMonth() - r.getTargetDay() + 1);
            LocalDateTime first = LocalDateTime.of(ym.atDay(day), time);
            if (first.isBefore(now)) {
                first = first.plusMonths(1);
            }
            // 重算后续月的实际日期
            ym = YearMonth.from(first.toLocalDate());
            day = Math.max(1, ym.lengthOfMonth() - r.getTargetDay() + 1);
            first = LocalDateTime.of(ym.atDay(day), time);
            return findMonthlyNext(first, executed, repeatCount, now);
        }

        return null;
    }

    /** 为类型1找下一个有效触发时间（按天偏移） */
    private LocalDateTime findNext(LocalDateTime base, int executed,
                                   Integer repeatCount, Duration step, LocalDateTime now) {
        LocalDateTime next = base.plus(step.multipliedBy(executed));
        if (!next.isBefore(now)) {
            return next;
        }
        // 已过期，快进到未来
        if (repeatCount == null || repeatCount == 0) {
            while (next.isBefore(now)) {
                next = next.plus(step);
            }
            return next;
        }
        // 有次数限制：跳过已错过的，检查是否还有剩余
        long skipped = Duration.between(next, now).dividedBy(step) + 1;
        int remaining = repeatCount - executed - (int) skipped;
        if (remaining <= 0) {
            return null;
        }
        return next.plus(step.multipliedBy(skipped));
    }

    /** 为类型2找第一个基础触发点 */
    private LocalDateTime findMonthlyBase(int targetDay, LocalTime time, LocalDateTime now) {
        YearMonth ym = YearMonth.from(now);
        int day = Math.min(targetDay, ym.lengthOfMonth());
        LocalDateTime dt = LocalDateTime.of(ym.atDay(day), time);
        if (dt.isBefore(now)) {
            ym = ym.plusMonths(1);
            day = Math.min(targetDay, ym.lengthOfMonth());
            dt = LocalDateTime.of(ym.atDay(day), time);
        }
        return dt;
    }

    /** 为类型2/3按月偏移查找 */
    private LocalDateTime findMonthlyNext(LocalDateTime base, int executed,
                                          Integer repeatCount, LocalDateTime now) {
        LocalDateTime next = base.plusMonths(executed);
        if (next.isBefore(now)) {
            if (repeatCount == null || repeatCount == 0) {
                while (next.isBefore(now)) {
                    next = next.plusMonths(1);
                }
                return next;
            }
            long skipped = 0;
            while (next.isBefore(now)) {
                next = next.plusMonths(1);
                skipped++;
            }
            int remaining = repeatCount - executed - (int) skipped;
            if (remaining <= 0) {
                return null;
            }
        }
        return next;
    }
}