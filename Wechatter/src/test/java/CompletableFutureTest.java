import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Test
    @DisplayName("测试顺序执行线程")
    public void test2() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future = CompletableFuture
                .runAsync(() -> run("T-1"), Executors.newFixedThreadPool(3))
                .thenRun(() -> run("T-2"))
                .thenRun(() -> run("T-3"));
        future.join();
        future.complete(null);
        future.get();
    }
    @Test
    @DisplayName("测试顺序启动线程")
    public void test3() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Thread t1 = new Thread(() -> run("T-1"));
        Thread t2 = new Thread(() -> run("T-2"));
        Thread t3 = new Thread(() -> run("T-3"));

        // 顺序启动，异步，本质是 runnable 里面启动一个线程
        future = CompletableFuture
                .completedFuture(null)
                .thenRun(t1::start)
                .thenRun(t2::start)
                .thenRun(t3::start);
        future.join();
        future.get();
    }

    private Object run(String name) {
        System.out.println(name + " started.");
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(name + " completed.");
        return null;
    }
}
