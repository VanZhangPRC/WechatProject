package van.project.wechatter.wechat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import van.project.wechat.wechatPublic.services.messages.IMessageReceiver;
import van.project.wechat.wechatPublic.services.messages.receive.*;
import van.project.wechat.wechatPublic.services.messages.resp.ResponseBaseMessage;
import van.project.wechat.wechatPublic.services.messages.resp.ResponseTextMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageReceiver implements IMessageReceiver {
    private final String NOT_SUPPORT = "占不支持处理该类型信息";

    private final AIChatterService aiChatterService;

    private ResponseBaseMessage returnNotSupport(BaseMessage message) {
        return ResponseTextMessage.builder(message).content(NOT_SUPPORT).build();
    }

    @Override
    public ResponseBaseMessage receiveLinkMessage(LinkMessage message) {
        return returnNotSupport(message);
    }

    @Override
    public ResponseBaseMessage receiveLocationMessage(LocationMessage message) {
        return returnNotSupport(message);
    }

    @Override
    public ResponseBaseMessage receivePic(PicMessage message) {
        return returnNotSupport(message);
    }

    @Override
    public ResponseBaseMessage receiveShortVideo(ShortVideoMessage message) {
        return returnNotSupport(message);
    }

    @Override
    public ResponseBaseMessage receiveText(TextMessage message) {
        return aiChatterService.handleMessage(message);
    }

    @Override
    public ResponseBaseMessage receiveVideo(VideoMessage message) {
        return returnNotSupport(message);
    }

    @Override
    public ResponseBaseMessage receiveVoice(VoiceMessage message) {
        return returnNotSupport(message);
    }

    @Override
    public ResponseBaseMessage receiveEvent(EventMessage message) {
        return null;
    }
}
