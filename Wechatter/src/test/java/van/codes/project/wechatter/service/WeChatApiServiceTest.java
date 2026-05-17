package van.codes.project.wechatter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WeChatApiService 集成测试 — 发送模版消息")
class WeChatApiServiceTest {

    @Autowired
    private WeChatApiService weChatApiService;

    @Autowired
    private WeChatTokenManager tokenManager;

    @Value("${test.wechat.open-id}")
    private String testOpenId;

    @BeforeEach
    void checkOpenId() {
        if (testOpenId == null || testOpenId.isBlank()) {
            throw new IllegalStateException(
                    "请通过环境变量 TEST_WECHAT_OPEN_ID 或系统属性 -Dtest.wechat.open-id=xxx 指定测试目标用户的 openId");
        }
    }

    @Test
    @DisplayName("获取真实 access_token 并发送模版消息")
    void shouldSendTemplateMessageSuccessfully() {
        String accessToken = tokenManager.getAccessToken();
        System.out.println("Access token obtained (first 20 chars): " + accessToken.substring(0, 20) + "...");

        assertThatCode(() -> weChatApiService.sendReminder(testOpenId, "这是一条来自 Wechatter 集成测试的模版消息"))
                .doesNotThrowAnyException();
    }
}