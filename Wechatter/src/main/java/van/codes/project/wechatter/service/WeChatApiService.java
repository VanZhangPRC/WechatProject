package van.codes.project.wechatter.service;

import van.codes.project.wechatter.config.WeChatProperties;
import van.codes.project.wechatter.feign.TemplateMessageSendReq;
import van.codes.project.wechatter.feign.TemplateMessageSendResp;
import van.codes.project.wechatter.feign.WechatFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatApiService {

    private final WeChatProperties weChatProperties;
    private final WechatFeign wechatFeign;
    private final WeChatTokenManager tokenManager;

    /**
     * 发送提醒模版消息
     * @param openId 接收者 openId
     * @param content 消息内容（对应模版中的 {{content.DATA}}）
     */
    public void sendReminder(String openId, String content) {
        TemplateMessageSendReq req = new TemplateMessageSendReq();
        req.setTouser(openId);
        req.setTemplate_id(weChatProperties.getTemplateId());
        req.setData(Map.of("content", new TemplateMessageSendReq.DataElement(content)));

        TemplateMessageSendResp resp = wechatFeign.templateMessageSend(tokenManager.getAccessToken(), req);
        if (resp.getErrcode() != null && resp.getErrcode() != 0) {
            log.error("Template message send failed: errcode={}, errmsg={}", resp.getErrcode(), resp.getErrmsg());
        } else {
            log.info("Template message sent to [{}], msgid={}", openId, resp.getMsgid());
        }
    }
}