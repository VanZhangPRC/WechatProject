package van.codes.project.wechatter.message.handler;

import van.codes.project.wechatter.message.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class EventMessageHandler implements MessageHandler {

    @Override
    public String getMsgType() {
        return "event";
    }

    @Override
    public String handle(Map<String, String> message) {
        String event = message.get("Event");
        String fromUser = message.get("FromUserName");
        log.info("Received event [{}] from [{}]", event, fromUser);
        // TODO: 可在此处按 Event 类型（subscribe/unsubscribe/CLICK 等）做二级分发
        return "success";
    }
}
