import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import van.project.wechatter.WechatterApplication;
import van.project.wechatter.entity.Reminder;
import van.project.wechatter.entity.enums.ReminderType;
import van.project.wechatter.mapper.ReminderMapper;
import van.project.wechatter.service.ReminderSchedulerService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@SpringBootTest(classes = WechatterApplication.class,  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.profiles.active=test")
public class TestReminderSchedulerService {

    @Autowired
    private ReminderSchedulerService reminderSchedulerService;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private ReminderMapper reminderMapper;

    private final String OPENID = "oCdDY14-mL0Xab0KpADxtleiW44E";


//    @Test
    @DisplayName("测试根据OPENID获取进行中的提醒任务")
    public void testGetScheduledInfo() {
        Map<Reminder, LocalDateTime> scheduledInfo = reminderSchedulerService.getScheduledInfo(OPENID);
        Assertions.assertFalse(scheduledInfo.isEmpty());
    }

    @Test
    @DisplayName("测试生成AI辅助任务效果")
    public void testSubTask() throws IOException {
        chatClient.prompt()
                .user(u -> u.param("openId", OPENID).text("用户[openId:`{openId}`]：" + "每天早上11点告诉我当天黄金价格和美元汇率"))
                .advisors(o -> o.param(ChatMemory.CONVERSATION_ID, OPENID))
                .call()
                .content();
        Reminder r = reminderMapper.selectById(1);
        System.out.println("############################");
        System.out.println(r.getContent());
        System.out.println("############################");
        r.setReminderType(ReminderType.SPECIFIC_DATE);
        r.setTargetDate(LocalDate.now());
        r.setTargetTime(LocalTime.now().plusMinutes(1));

        int id = reminderMapper.updateById(r);
        reminderSchedulerService.schedule(r);
        System.in.read();
    }

}
