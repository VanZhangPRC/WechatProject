package van.codes.project.wechatter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WeChatMessageAiService {

    private final ChatClient chatClient;

    public WeChatMessageAiService(ChatClient.Builder chatClientBuilder,
                                  ToolCallbackProvider toolCallbackProvider, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("""
                        你是一个提醒助手，负责根据用户的文本消息判断是否需要创建提醒或取消提醒。提醒事项与日期、时间关联紧密，你需要查询好当前的正确日期。

                        如果需要创建提醒，调用 addReminder 工具。一个提醒事项只需要创建一次提醒。
                        如果需要取消提醒，先调用 listReminders 查看用户有哪些提醒，然后调用 cancelReminder 取消对应 ID 的提醒。
                        工具参数说明：
                        - openId: 用户的 openId，会从外部传入
                        - content: 提醒的文字内容，从用户消息中提取
                        - reminderType: "指定日期"、"每月几号"、"每月倒数几天"
                        - targetDate: 指定日期使用时填写，格式 YYYY-MM-DD
                        - targetDay: 每月几号或每月倒数几天使用时填写，表示几号或倒数几天
                        - targetTime: 通知时分，格式 HH:mm
                        - repeatCount: 0=一次或一直持续, N=重复N次，如果没有提及则设置为1

                        日期和时间的推断规则：
                        - "明天" = 当前日期的下一天
                        - "后天" = 当前日期的后两天
                        - "下周一/二..." = 下一个对应的星期几
                        - "每月X号" = 使用"每月几号"
                        - "每月倒数X天" = 使用"每月倒数几天"
                        - "每天" = 使用"指定日期"且 repeatCount 填0（一直持续）
                        - "持续N天/次" = repeatCount 填N
                        - 如果没提到日期，默认是明天
                        - 如果没提到时间，默认是上午9:00

                        一定要先从用户消息中提取完上述信息再调用工具，不要乱填。
                        如果用户的消息与创建提醒无关（如闲聊、问候），直接文字回复，不要调用任何工具。
                        """)
                .build();
    }

    /**
     * 处理用户微信文本消息，AI 自动决定是否创建提醒
     * @param openId 用户 openId
     * @param content 用户发送的文本内容
     * @return AI 的回复内容
     */
    public String processMessage(String openId, String content) {
        log.info("Processing message from [{}]: {}", openId, content);
        String reply = chatClient.prompt()
                .user(u -> u.param("openId", openId).text(content+"[用户openId:{openId}]"))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, openId))
                .call()
                .content();
        log.info("AI reply to [{}]: {}", openId, reply);
        return reply;
    }
}