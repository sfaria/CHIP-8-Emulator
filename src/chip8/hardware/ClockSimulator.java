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
    private final long periodInMs;

    // -------------------- Constructors --------------------

    public ClockSimulator(int hzTickRate) {
        this.periodInMs = (long) ((1d / (double) hzTickRate) * 1000);
        this.timer = new Timer("%shz Timer".formatted(hzTickRate), true);
    }

    // -------------------- Public Methods --------------------

    public final void withClockRegulation(BooleanSupplier work) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean continueExecution = work.getAsBoolean();
                if (!continueExecution) {
                    timer.cancel();
                }
            }
        }, 0L, periodInMs);
    }

}
