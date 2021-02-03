package chip8.hardware;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BooleanSupplier;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class ClockSimulator {

    // -------------------- Private Variables ---------------

    private Timer timer;
    private final BooleanSupplier work;

    // -------------------- Constructors --------------------

    public ClockSimulator(BooleanSupplier work) {
        this.work = work;
    }

    // -------------------- Public Methods --------------------

    public final boolean isRunning() {
        return timer != null;
    }

    public final void stopGracefully() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public final void start(int hzTickRate) {
        long periodInMs = getPeriodInMs(hzTickRate);
        timer = new Timer(true);
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

    // -------------------- Private Methods --------------------

    private long getPeriodInMs(double hzTickRate) {
        return (long) ((1d / hzTickRate) * 1000);
    }

}
