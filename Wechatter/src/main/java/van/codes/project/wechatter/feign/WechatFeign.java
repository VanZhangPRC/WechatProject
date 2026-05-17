package van.codes.project.wechatter.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "wechatFeign", url = "https://api.weixin.qq.com/cgi-bin")
public interface WechatFeign {

    @PostMapping("/stable_token")
    AccessTokenResp stableToken(@RequestBody AccessTokenReq req);

    @PostMapping("/message/template/send")
    TemplateMessageSendResp templateMessageSend(@RequestParam("access_token") String accessToken,
                                                @RequestBody TemplateMessageSendReq req);
}