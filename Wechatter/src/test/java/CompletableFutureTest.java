import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureTest {

    @Test
    @DisplayName("测试completeAsync和thenRun")
    public void test1() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeAsync(() -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("complete async");
            return "hello";
        }).thenRun(() -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("run complete");
        });

        long time = System.currentTimeMillis();
        future.get();
        System.out.println(System.currentTimeMillis() - time);
        System.out.println(future.isDone());
        Thread.sleep(4_000);
    }

}
