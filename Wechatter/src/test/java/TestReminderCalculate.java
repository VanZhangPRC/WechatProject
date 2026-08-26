import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import van.project.wechatter.entity.Reminder;
import van.project.wechatter.entity.enums.ReminderType;
import van.project.wechatter.service.ReminderSchedulerService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class TestReminderCalculate {


    private ReminderSchedulerService reminderSchedulerService = new ReminderSchedulerService(null, null, null, null);

    @Test
    @DisplayName("测试每周执行的日期计算逻辑")
    public void testReminderEveryWeekCalc() {
        Reminder reminder = new Reminder();
        reminder.setTargetTime(LocalTime.of(10,30));
        reminder.setReminderType(ReminderType.EVERY_WEEK);
        reminder.setDayOfWeek("246");

        LocalDateTime localDateTime = reminderSchedulerService.calcNextTrigger(reminder);
        assertEquals(LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.of(10,30)), localDateTime);

        reminder.setDayOfWeek("1357");
        localDateTime = reminderSchedulerService.calcNextTrigger(reminder);
        assertEquals(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(10,30)), localDateTime);
    }

}
