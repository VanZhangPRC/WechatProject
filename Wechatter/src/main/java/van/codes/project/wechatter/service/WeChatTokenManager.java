package van.codes.project.wechatter.service;

import van.codes.project.wechatter.config.WeChatProperties;
import van.codes.project.wechatter.feign.AccessTokenReq;
import van.codes.project.wechatter.feign.AccessTokenResp;
import van.codes.project.wechatter.feign.WechatFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatTokenManager {

    private final WeChatProperties weChatProperties;
    private final WechatFeign wechatFeign;

    private volatile String accessToken;
    private volatile long expiresAt;

    /** 获取 access_token，自动缓存，提前5分钟刷新 */
    public synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (accessToken != null && now < expiresAt) {
            return accessToken;
        }
        AccessTokenResp resp = wechatFeign.stableToken(
                new AccessTokenReq(weChatProperties.getAppId(), weChatProperties.getSecret()));
        if (resp.getAccessToken() == null) {
            throw new RuntimeException("Failed to get access_token, response: " + resp);
        }
        accessToken = resp.getAccessToken();
        long expiresIn = resp.getExpiresIn() != null ? resp.getExpiresIn() : 7200;
        expiresAt = now + (expiresIn - 300) * 1000L;
        log.info("Access token refreshed, expires in {}s", expiresIn);
        return accessToken;
    }
}