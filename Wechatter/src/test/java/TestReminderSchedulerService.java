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
import van.project.wechatter.entity.enums.ReminderStatus;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    @DisplayName("测试生成AI辅助任务效果，全流程")
    @Order(1)
    public void testSubTask() throws IOException, InterruptedException {

        TextMessage message = new TextMessage();
        message.setMsgId(1L);
        message.setMsgType(BaseMessage.MessageType.text);
        message.setFromUserName(OPENID);
        message.setContent("每天早上10:30告诉我当天的黄金价格和美元价格");


        ResponseTextMessage responseTextMessage = null;
        while (responseTextMessage == null) {
            responseTextMessage = aiChatterService.handleMessage(message);
        }

        Reminder reminder = reminderMapper.selectById(1L);
        reminder.setTargetTime(LocalTime.now().plusMinutes(1));
        reminderMapper.updateById(reminder);

        reminderSchedulerService.schedule(reminder);

    }

    @Test
    @DisplayName("测试生成AI辅助任务效果，指定文本")
    @Order(2)
    public void testSubTask2() throws IOException, InterruptedException {
        LocalDateTime now = LocalDateTime.now().plusMinutes(1);
        Reminder reminder = new Reminder();
        reminder.setId(2L);
        reminder.setStatus(ReminderStatus.ACTIVE);
        reminder.setReminderType(ReminderType.SPECIFIC_DATE);
        reminder.setTargetDate(now.toLocalDate());
        reminder.setTargetTime(now.toLocalTime());
        reminder.setOpenId(OPENID);
        reminder.setAiAssisted(true);
        reminder.setContent("每天早上10:30为用户播报当日黄金价格和美元汇率。执行步骤：1) 调用 goldenPrice 工具查询上海黄金交易所最新金价，获取最低价、最新价、最高价、开盘价；2) 调用 dollarExchange 工具查询美元汇率（100美元兑换人民币价格）；3) 若工具返回null或请求出错，如实告知用户暂时无法获取数据；4) 将查询到的数据整理成简洁清晰的播报内容推送给用户，内容包括金价各指标和美元汇率数值。");
        reminderMapper.insertOrUpdate(reminder);

        reminderSchedulerService.schedule(reminder);
        System.in.read();
    }

}
