package van.codes.project.wechatter.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import van.codes.project.wechatter.entity.Reminder;
import van.codes.project.wechatter.entity.enums.ReminderType;
import van.codes.project.wechatter.service.ReminderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 自托管工具中心 — 提醒相关工具
 * 通过 ToolCallbackProvider 注册到 ChatClient，AI 直接本地调用，不走 MCP HTTP 协议。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTools {

    private final ReminderService reminderService;

    @Tool(name = "addReminder", description = """
            新增一个提醒记录。提醒有三种类型：
            指定日期 - 指定某一天的某个时分进行通知（需填 targetDate）
            每月几号 - 每月几号的某个时分进行通知（需填 targetDay）
            每月倒数几天 - 每月倒数几天的某个时分进行通知（需填 targetDay）
            重复次数 repeatCount 填 0 表示一直持续，没有提及时设为1。
            类型1按天重复（连续N天），类型2/3按月重复（连续N个月）。
            """)
    public String addReminder(
            @ToolParam(description = "用户 openId") String openId,
            @ToolParam(description = "通知内容") String content,
            @ToolParam(description = "提醒类型：指定日期、每月几号、每月倒数几天") ReminderType reminderType,
            @ToolParam(description = "指定日期，类型1使用，格式 YYYY-MM-DD，如 2025-06-01") String targetDate,
            @ToolParam(description = "每月几号(1-31)，类型2使用；或每月倒数几天(1-31)，类型3使用") Integer targetDay,
            @ToolParam(description = "通知时分，格式 HH:mm，如 14:30") String targetTime,
            @ToolParam(description = "重复次数：0=一直持续，N=重复N次") int repeatCount) {

        Reminder reminder = new Reminder();
        reminder.setOpenId(openId);
        reminder.setContent(content);
        reminder.setReminderType(reminderType);
        reminder.setTargetTime(LocalTime.parse(targetTime));

        if (reminderType == ReminderType.SPECIFIC_DATE) {
            reminder.setTargetDate(LocalDate.parse(targetDate));
        } else {
            reminder.setTargetDay(targetDay);
        }

        reminder.setRepeatCount(repeatCount == 0 ? null : repeatCount);
        reminderService.add(reminder);
        log.info("Tool addReminder: openId={}, type={}, content={}, id={}",
                openId, reminderType, content, reminder.getId());
        return "提醒已新增，ID=" + reminder.getId();
    }

    @Tool(name = "listReminders", description = "查询某用户所有启用中的提醒记录")
    public List<Reminder> listReminders(
            @ToolParam(description = "用户 openId") String openId) {
        List<Reminder> list = reminderService.listActiveByOpenId(openId);
        log.info("Tool listReminders: openId={}, count={}", openId, list.size());
        return list;
    }

    @Tool(name = "listAllReminders", description = "查询所有启用中的提醒记录")
    public List<Reminder> listAllReminders() {
        List<Reminder> list = reminderService.listAllActive();
        log.info("Tool listAllReminders: count={}", list.size());
        return list;
    }

    @Tool(name = "getCurrentDateTime", description = "查询当前日期时间，需要获取当前日期时间时使用")
    public LocalDateTime getCurrentDate() {
        return LocalDateTime.now();
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
