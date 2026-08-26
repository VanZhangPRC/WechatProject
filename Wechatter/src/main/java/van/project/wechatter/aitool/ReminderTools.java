package van.project.wechatter.aitool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import van.project.wechatter.entity.Reminder;
import van.project.wechatter.entity.enums.ReminderType;
import van.project.wechatter.service.ReminderSchedulerService;
import van.project.wechatter.service.ReminderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自托管工具中心 — 提醒相关工具
 * 通过 ToolCallbackProvider 注册到 ChatClient，AI 直接本地调用，不走 MCP HTTP 协议。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTools {

    private final ReminderService reminderService;
    private final ReminderSchedulerService reminderSchedulerService;

    @Tool(name = "addReminder", description = """
            # 新增一个提醒记录。\n
            ## 提醒类型有：\n
            SPECIFIC_DATE(指定日期) - 指定某一天的某个时分进行通知，需填 targetDate\n
            MONTHLY_DAY(每月几号) - 每月几号的某个时分进行通知，需填 dayOfMonth\n
            MONTHLY_LAST_N_DAYS(每月倒数几天) - 每月倒数几天的某个时分进行通知，需填 lastDayOfMonth\n
            EVERY_DAY(每天执行)\n
            EVERY_WEEK(每周执行) - 每周哪一天执行，需要填 dayOfWeek，传入数字代表周几(1-Monday周一，2-Tuesday周二，3-Wednesday周三，4-Thursday周四，5-Friday周五，6-Saturday周六，7-Sunday周日)。\n
            \n
            ## 是否需要AI辅助的判断：\n
            根据提醒/待办事项的内容判断是否需要AI辅助，
            如果内容只是普通、固定的文本信息则不需要(如，提醒用户某个时间做某件事)；
            如果内容不是普通、固定的文本信息，根据现有工具判断能否后续由工具辅助大模型完成(如，每天查询黄金价格并告知用户)，
            如果可以根据现有工具辅助完成，则将 content 内容填写为可以用于指导大模型完成相关信息采集处理的、简短的、指令明确的 skill 描述，skill最后的输出为需要发送给用户的提醒内容，skill内容长度不超过900，
            如果现有工具辅助不足够协助大模型完成用户的需要，则礼貌回复无法处理。
            """)
    public String addReminder(
            @ToolParam(description = "用户 openId") String openId,
            @ToolParam(description = "通知内容") String content,
            @ToolParam(description = "提醒类型：SPECIFIC_DATE(指定日期)、MONTHLY_DAY(每月几号)、MONTHLY_LAST_N_DAYS(每月倒数几天)、EVERY_DAY(每天执行)、EVERY_WEEK(每周执行)") ReminderType reminderType,
            @ToolParam(description = "SPECIFIC_DATE 类型使用，指定日期，格式 YYYY-MM-DD，如 2025-06-01") String targetDate,
            @ToolParam(description = "MONTHLY_DAY 类型使用，每月几号，填写 1-31 中的一个数字") Integer dayOfMonth,
            @ToolParam(description = "MONTHLY_LAST_N_DAYS 类型使用，每月倒数几天，填写 1-31 中的一个数字") Integer lastDayOfMonth,
            @ToolParam(description = "EVERY_WEEK 类型使用，每周的周几，填写 1-7 中的数字") List<Integer> dayOfWeek,
            @ToolParam(description = "通知时分，格式 HH:mm，如 14:30") String targetTime,
            @ToolParam(description = "是否需要AI辅助，如果提醒/待办事项内容为普通的文本信息，则为false不需要，否则认为需要AI辅助") Boolean aiAssisted) {

        Reminder reminder = new Reminder();
        reminder.setOpenId(openId);
        reminder.setContent(content);
        reminder.setReminderType(reminderType);
        reminder.setTargetTime(LocalTime.parse(targetTime));
        reminder.setAiAssisted(aiAssisted);

        try {
            switch (reminderType) {
                case SPECIFIC_DATE -> reminder.setTargetDate(LocalDate.parse(targetDate));
                case MONTHLY_DAY -> reminder.setDayOfMonth(dayOfMonth);
                case MONTHLY_LAST_N_DAYS -> reminder.setLastDayOfMonth(lastDayOfMonth);
                case EVERY_WEEK -> reminder.setDayOfWeek(dayOfWeek.stream().sorted().map(String::valueOf).collect(Collectors.joining()));
            }
        } catch (Exception e) {
            log.error("生成提醒记录错误，{} 类型关联的日期属性填写错误", reminderType, e);
            return "入参错误，" + reminderType + " 类型关联的日期属性填写错误";
        }

        reminderService.add(reminder);
        log.info("Tool addReminder: openId={}, type={}, content={}, id={}",
                openId, reminderType, content, reminder.getId());
        return "提醒已新增，ID=" + reminder.getId();
    }

    @Tool(name = "listScheduledInfo", description = "查询某个用户目前执行中的定时提醒任务，返回提醒内容和下次执行时间")
    public List<String[]> listScheduledInfo(@ToolParam(description = "用户标识openId") String openId) {
        Map<Reminder, LocalDateTime> scheduledInfo = reminderSchedulerService.getScheduledInfo(openId);
        List<String[]> result = new ArrayList<>();
        for (Map.Entry<Reminder, LocalDateTime> entry : scheduledInfo.entrySet()) {
            result.add(new String[]{entry.getKey().getContent(), entry.getValue().toString()});
        }
        return result;
    }

    @Tool(name = "cancelReminder", description = "取消指定 ID 的提醒，将状态置为已取消并移除定时调度")
    public String cancelReminder(
            @ToolParam(description = "用户 openId") String openId,
            @ToolParam(description = "提醒记录 ID") Long reminderId) {
        reminderService.cancel(reminderId);
        log.info("Tool cancelReminder: openId={}, reminderId={}", openId, reminderId);
        return "提醒已取消，ID=" + reminderId;
    }
}
