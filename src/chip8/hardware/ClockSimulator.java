package chip8.hardware;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class ClockSimulator {

    // -------------------- Private Variables ---------------

    private final Timer timer;
    private final ExecutorService ex;
    private final long periodInMs;

    // -------------------- Constructors --------------------

    public ClockSimulator(int hzTickRate, ExecutorService ex) {
        this.periodInMs = (long) ((1d / (double) hzTickRate) * 1000);
        this.timer = new Timer("%shz Timer".formatted(hzTickRate), true);
        this.ex = ex;
    }

    // -------------------- Public Methods --------------------

    public final void withClockRegulation(BooleanSupplier work) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    Future<Boolean> f = ex.submit(work::getAsBoolean);
                    boolean continueExecution = f.get();
                    if (!continueExecution) {
                        timer.cancel();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    timer.cancel();
                    throw new RuntimeException("Unexpected Exception.", e);
                }
            }
        }, 0L, periodInMs);
    }

}
