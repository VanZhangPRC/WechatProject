package van.codes.project.wechatter.message.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import van.codes.project.wechatter.message.MessageHandler;
import van.codes.project.wechatter.service.WeChatMessageAiService;
import van.codes.project.wechatter.util.XmlUtil;

import java.util.Map;

@Slf4j
@Component
public class TextMessageHandler implements MessageHandler {

    private final WeChatMessageAiService aiService;

    public TextMessageHandler(WeChatMessageAiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public String getMsgType() {
        return "text";
    }

    @Override
    public String handle(Map<String, String> message) {
        String fromUser = message.get("FromUserName");
        String toUser = message.get("ToUserName");
        String content = message.get("Content");
        log.info("Received text from [{}]: {}", fromUser, content);

        String reply = aiService != null ? aiService.processMessage(fromUser, content) : "AI service unavailable";
        log.info("AI reply to [{}]: {}", fromUser, reply);
        return XmlUtil.buildTextReply(fromUser, toUser, reply);
    }
}