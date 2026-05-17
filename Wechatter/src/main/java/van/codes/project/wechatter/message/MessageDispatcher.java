package van.codes.project.wechatter.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MessageDispatcher {

    private final Map<String, MessageHandler> handlerMap;

    public MessageDispatcher(List<MessageHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(MessageHandler::getMsgType, Function.identity()));
        log.info("Registered message handlers: {}", handlerMap.keySet());
    }

    /** 根据 MsgType 分发消息到对应的 Handler */
    public String dispatch(Map<String, String> message) {
        String msgType = message.get("MsgType");
        if (msgType == null) {
            log.warn("MsgType is null in message: {}", message);
            return "success";
        }
        MessageHandler handler = handlerMap.get(msgType);
        if (handler == null) {
            log.info("No handler for MsgType: {}, message: {}", msgType, message);
            return "success";
        }
        log.info("Dispatching MsgType={} to {}", msgType, handler.getClass().getSimpleName());
        return handler.handle(message);
    }
}
