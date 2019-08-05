package easyble2;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 执行后台任务
 * <p>
 * date: 2019/8/3 12:59
 * author: zengfansheng
 */
final class BackgroundPoster implements Runnable {
    private final Queue<Runnable> queue;
    private volatile boolean executorRunning;
    private final EasyBLE easyBle;

    BackgroundPoster(EasyBLE easyBle) {
        this.easyBle = easyBle;
        queue = new ConcurrentLinkedQueue<>();
    }

    void enqueue(Runnable runnable) {
        Inspector.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        synchronized (this) {
            queue.add(runnable);
            if (!executorRunning) {
                executorRunning = true;
                easyBle.executorService.execute(this);
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Runnable runnable = queue.poll();
                if (runnable == null) {
                    synchronized (this) {
                        runnable = queue.poll();
                        if (runnable == null) {
                            executorRunning = false;
                            return;
                        }
                    }
                }
                runnable.run();
            }
        } finally {
            executorRunning = false;
        }
    }
}
