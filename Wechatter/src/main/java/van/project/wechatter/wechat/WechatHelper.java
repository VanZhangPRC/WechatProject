package van.project.wechatter.wechat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import van.project.wechat.wechatPublic.services.api.TemplateMessageSendReq;
import van.project.wechatter.util.MarkdownUtil;

import java.util.Collections;

@Component
public class WechatHelper {

    @Value("${wechatter.wechat.notify-template-id:bnDl2Gnssulz21Opa1zPawCJi6S5WDF91k6F8E-DZKE}")
    private String NOTIFY_TEMPLATE_ID;
    @Value("${wechatter.wechat.host}")
    private String HOST;

    public TemplateMessageSendReq buildNotifyTemplateMessage(String openId, String shortContent, String content) {
        return TemplateMessageSendReq
                .builder()
                .touser(openId)
                .template_id(NOTIFY_TEMPLATE_ID)
                .data(Collections.singletonMap("content", new TemplateMessageSendReq.DataElement(shortContent)))
                .url(HOST + "/markdown/view?data=" + MarkdownUtil.encode(content))
                .build();
    }

    public String getNotifyTemplateId() {
        return NOTIFY_TEMPLATE_ID;
    }
}
