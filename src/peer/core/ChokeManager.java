package peer.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tiny scheduler so ConnectionManager can time the preferred + optimistic windows itself.
 */
public class ChokeManager implements AutoCloseable {
    private final int unchokeSeconds;
    private final int optimisticSeconds;
    private final Runnable preferredTask;
    private final Runnable optimisticTask;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> preferredFuture;
    private ScheduledFuture<?> optimisticFuture;

    public ChokeManager(int unchokeSeconds, int optimisticSeconds, Runnable preferredTask, Runnable optimisticTask) {
        this.unchokeSeconds = unchokeSeconds;
        this.optimisticSeconds = optimisticSeconds;
        this.preferredTask = preferredTask;
        this.optimisticTask = optimisticTask;
    }

    public void start() {
        // start immediately so the system makes progress even if peers arrive late
        preferredFuture = executor.scheduleAtFixedRate(preferredTask, 0, unchokeSeconds, TimeUnit.SECONDS);
        optimisticFuture = executor.scheduleAtFixedRate(optimisticTask, optimisticSeconds, optimisticSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (preferredFuture != null) preferredFuture.cancel(true);
        if (optimisticFuture != null) optimisticFuture.cancel(true);
        executor.shutdownNow();
    }

    @Override
    public void close() {
        stop();
    }
}
