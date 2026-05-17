package van.codes.project.wechatter.message.handler;

import van.codes.project.wechatter.message.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class VoiceMessageHandler implements MessageHandler {

    @Override
    public String getMsgType() {
        return "voice";
    }

    @Override
    public String handle(Map<String, String> message) {
        log.info("Received voice from [{}]", message.get("FromUserName"));
        return "success";
    }
}
