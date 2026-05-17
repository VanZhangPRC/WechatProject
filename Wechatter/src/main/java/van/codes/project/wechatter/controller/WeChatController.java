package van.codes.project.wechatter.controller;

import org.springframework.http.MediaType;
import van.codes.project.wechatter.config.WeChatProperties;
import van.codes.project.wechatter.message.MessageDispatcher;
import van.codes.project.wechatter.util.MessageDeduplicator;
import van.codes.project.wechatter.util.XmlUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/wechat")
public class WeChatController {

    private final WeChatProperties weChatProperties;
    private final MessageDispatcher messageDispatcher;
    private final MessageDeduplicator messageDeduplicator;

    public WeChatController(WeChatProperties weChatProperties,
                            MessageDispatcher messageDispatcher,
                            MessageDeduplicator messageDeduplicator) {
        this.weChatProperties = weChatProperties;
        this.messageDispatcher = messageDispatcher;
        this.messageDeduplicator = messageDeduplicator;
    }

    /** 微信服务器配置验证（GET） */
    @GetMapping
    public String verify(@RequestParam("signature") String signature,
                         @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce,
                         @RequestParam("echostr") String echostr) {
        String token = weChatProperties.getToken();
        if (token == null || token.isBlank()) {
            log.warn("WeChat server verification failed: token not configured");
            return "token not configured";
        }
        if (checkSignature(signature, timestamp, nonce, token)) {
            log.info("WeChat server verification passed");
            return echostr;
        }
        log.warn("WeChat server verification failed: signature mismatch");
        return "signature verification failed";
    }

    /** 接收用户消息（POST），微信会将 XML 消息体 POST 到此接口 */
    @PostMapping(produces = MediaType.TEXT_XML_VALUE)
    public String receive(HttpServletRequest request) {
        Map<String, String> message = null;
        try {
            message = XmlUtil.parse(request.getInputStream());
            log.info("Received wechat message: MsgId={}, MsgType={}, FromUserName={}",
                    message.get("MsgId"), message.get("MsgType"), message.get("FromUserName"));

            String msgId = message.get("MsgId");
            if (msgId == null)
                return "success";

            CompletableFuture<String> retryFuture = messageDeduplicator.tryAcquire(msgId);
            if (retryFuture != null) {
                try {
                    return retryFuture.get(messageDeduplicator.retryTimeoutSeconds(), TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Retry MsgId={} timed out or failed", msgId);
                    return "success";
                }
            }

            String result = messageDispatcher.dispatch(message);
            messageDeduplicator.complete(msgId, result);
            log.info("Message dispatch result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Failed to process wechat message", e);
            if (message != null) {
                messageDeduplicator.complete(message.get("MsgId"), "success");
            }
            return "success";
        }
    }

    private boolean checkSignature(String signature, String timestamp, String nonce, String token) {
        String sorted = Stream.of(token, timestamp, nonce).sorted().reduce("", (a, b) -> a + b);
        String calculated = sha1(sorted);
        return calculated != null && calculated.equals(signature);
    }

    private String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
}
