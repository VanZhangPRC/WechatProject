package van.codes.project.wechatter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "wechat")
public class WeChatProperties {

    /** 公众号 AppID */
    private String appId;

    /** 公众号 AppSecret */
    private String secret;

    /** 服务器配置中的 Token */
    private String token;

    /** 模版消息 ID */
    private String templateId;
}
