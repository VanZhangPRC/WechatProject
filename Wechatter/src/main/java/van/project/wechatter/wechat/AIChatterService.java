package van.project.wechatter.wechat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import van.project.wechat.wechatPublic.services.messages.receive.TextMessage;
import van.project.wechat.wechatPublic.services.messages.resp.ResponseTextMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIChatterService {

    private final ChatClient chatClient;

    // 长时间消息处理过程
    private final ConcurrentHashMap<Long, CompletableFuture<String>> processMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompletableFuture<String>> completedMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> completedTimeMap = new ConcurrentHashMap<>();

    public ResponseTextMessage handleMessage(TextMessage message){

        Long msgId = message.getMsgId();

        if (msgId == null)
            return null;

        CompletableFuture<String> future = tryAquire(msgId, message);
        try {
            String s = future.get(14, TimeUnit.SECONDS);
            return ResponseTextMessage.builder(message).content(s).build();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            return null;
        }
    }

    private CompletableFuture<String> tryAquire (Long msgId, TextMessage message) {
        if (completedMap.containsKey(msgId)) {
            return completedMap.get(msgId);
        }

        CompletableFuture<String> newFuture = new CompletableFuture<>();
        CompletableFuture<String> existedFuture = processMap.putIfAbsent(msgId, newFuture);

        if (existedFuture == null) {
            if (completedMap.containsKey(msgId)) {
                processMap.remove(msgId);
                return completedMap.get(msgId);
            } else {
                newFuture
                        .completeAsync(() ->
                                chatClient.prompt()
                                        .user(u -> u.param("openId", message.getFromUserName()).text("用户[openId:`{openId}`]：" + message.getContent()))
                                        .advisors(o -> o.param(ChatMemory.CONVERSATION_ID, message.getFromUserName()))
                                        .call()
                                        .content())
                        .thenRunAsync(() -> {
                            completedMap.put(msgId, newFuture);
                            processMap.remove(msgId);
                            completedTimeMap.put(msgId, System.currentTimeMillis());
                        });
                return newFuture;
            }
        } else {
            return existedFuture;
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void cleanCompletedMap(){
        log.info("Start clean completed map of AI chat task...");
        long now = System.currentTimeMillis();
        List<Long> removableKeys = completedTimeMap
                .entrySet()
                .stream()
                .filter(entry -> now - entry.getValue() > 300_000)
                .map(Map.Entry::getKey)
                .toList();

        for (Long removableKey : removableKeys) {
            completedMap.remove(removableKey);
            completedTimeMap.remove(removableKey);
        }
    }
}
