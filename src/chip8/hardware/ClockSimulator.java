package chip8.hardware;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class ClockSimulator {

    // -------------------- Private Variables ---------------

    private final Timer timer;
    private final BooleanSupplier work;
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);

    // -------------------- Constructors --------------------

    public ClockSimulator(BooleanSupplier work) {
        this.timer = new Timer(true);
        this.work = work;
    }


    // -------------------- Public Methods --------------------

    public final void stopGracefully() {
        keepRunning.set(false);
    }

    public final void start(int hzTickRate) {
        keepRunning.set(true);
        long periodInMs = getPeriodInMs(hzTickRate);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean continueExecution = work.getAsBoolean();
                if (!continueExecution || !keepRunning.get()) {
                    timer.cancel();
                }
            }
        }, 0L, periodInMs);
    }

    // -------------------- Private Methods --------------------

    private long getPeriodInMs(double hzTickRate) {
        return (long) ((1d / hzTickRate) * 1000);
    }

}
