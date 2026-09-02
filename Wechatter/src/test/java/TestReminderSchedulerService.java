import org.junit.jupiter.api.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import van.project.wechat.wechatPublic.services.messages.receive.BaseMessage;
import van.project.wechat.wechatPublic.services.messages.receive.TextMessage;
import van.project.wechat.wechatPublic.services.messages.resp.ResponseTextMessage;
import van.project.wechatter.WechatterApplication;
import van.project.wechatter.aitool.ReminderTools;
import van.project.wechatter.entity.Reminder;
import van.project.wechatter.entity.enums.ReminderType;
import van.project.wechatter.mapper.ReminderMapper;
import van.project.wechatter.service.ReminderSchedulerService;
import van.project.wechatter.wechat.AIChatterService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@SpringBootTest(classes = WechatterApplication.class,  webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "spring.profiles.active=test")
public class TestReminderSchedulerService {

    @Autowired
    private ReminderSchedulerService reminderSchedulerService;
    @Autowired
    private AIChatterService aiChatterService;
    @Autowired
    private ReminderMapper reminderMapper;

    private final String OPENID = "oCdDY14-mL0Xab0KpADxtleiW44E";


    @Test
    @DisplayName("测试生成AI辅助任务效果")
    public void testSubTask() throws IOException, InterruptedException {

        TextMessage message = new TextMessage();
        message.setMsgId(1L);
        message.setMsgType(BaseMessage.MessageType.text);
        message.setFromUserName(OPENID);
        message.setContent("每天早上11点以表格形式告诉我当天的黄金价格和美元汇率");


        ResponseTextMessage responseTextMessage = null;
        while (responseTextMessage == null) {
            responseTextMessage = aiChatterService.handleMessage(message);
        }

        Reminder reminder = reminderMapper.selectById(1L);
        reminder.setTargetTime(LocalTime.now().plusMinutes(1));
        reminderMapper.updateById(reminder);

        reminderSchedulerService.schedule(reminder);
        System.in.read();
    }

}
