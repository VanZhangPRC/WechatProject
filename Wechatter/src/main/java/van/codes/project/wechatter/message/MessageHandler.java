package van.codes.project.wechatter.message;

import java.util.Map;

/** 消息处理策略接口 */
public interface MessageHandler {

    /** 该 Handler 处理的 MsgType */
    String getMsgType();

    /** 处理消息，返回回复文本 */
    String handle(Map<String, String> message);
}
