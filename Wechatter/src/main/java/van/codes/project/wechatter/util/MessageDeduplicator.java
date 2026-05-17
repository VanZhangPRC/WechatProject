package van.codes.project.wechatter.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信消息去重器，基于 MsgId 防抖，纯内存实现。
 * 微信会对未及时响应的消息进行最多 3 次重试（间隔约 5s），
 * 首次请求开始处理后，后续重试等待首次结果完成再返回。
 */
@Slf4j
@Component
public class MessageDeduplicator {

    private static final long DEDUP_WINDOW_MS = 60_000;
    private static final long RETRY_TIMEOUT_SECONDS = 14;

    private final ConcurrentHashMap<String, CompletableFuture<String>> futures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> completed = new ConcurrentHashMap<>();

    /**
     * @param msgId 消息 ID
     * @return null 表示首次请求，调用方负责处理并调用 {@link #complete}
     *         non-null 表示重试请求，调用方应等待该 Future 获取首次请求的结果
     */
    public CompletableFuture<String> tryAcquire(String msgId) {
        if (msgId == null) {
            return null;
        }

        Long doneTime = completed.get(msgId);
        if (doneTime != null && System.currentTimeMillis() - doneTime < DEDUP_WINDOW_MS) {
            log.debug("MsgId={} already completed, returning empty", msgId);
            return CompletableFuture.completedFuture("");
        }

        CompletableFuture<String> newFuture = new CompletableFuture<>();
        CompletableFuture<String> existing = futures.putIfAbsent(msgId, newFuture);
        if (existing != null) {
            log.info("Retry request for MsgId={}, waiting for first request result", msgId);
            return existing;
        }
        return null;
    }

    /**
     * 首次请求处理完成后调用，唤醒所有等待中的重试请求。
     */
    public void complete(String msgId, String result) {
        CompletableFuture<String> future = futures.remove(msgId);
        if (future != null) {
            future.complete(result);
        }
        completed.put(msgId, System.currentTimeMillis());
    }

    public long retryTimeoutSeconds() {
        return RETRY_TIMEOUT_SECONDS;
    }

    @Scheduled(fixedRate = 300_000)
    void cleanExpired() {
        long now = System.currentTimeMillis();
        int beforeCompleted = completed.size();
        completed.values().removeIf(t -> now - t > DEDUP_WINDOW_MS);
        int beforeFutures = futures.size();
        futures.values().removeIf(f -> f.isDone());
        log.debug("Cleaned {} completed, {} stale futures",
                beforeCompleted - completed.size(), beforeFutures - futures.size());
    }
}