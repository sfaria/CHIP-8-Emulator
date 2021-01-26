package chip8;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class ClockSimulator {

    // -------------------- Private Variables --------------------

    private final double nanoHerz;
    private long lastTimeNanos;

    // -------------------- Constructors --------------------

    ClockSimulator(int hzTickRate) {
        this.nanoHerz = (1.0d / (double) hzTickRate) * 1e10-9;

    }

    // -------------------- Default Methods --------------------

    final void startClock() {
        lastTimeNanos = System.nanoTime();
    }

    final void withClockRegulation(Runnable work) {
        long now = System.nanoTime();
        double diff = lastTimeNanos - now;
        int operationsToDo = Math.max(1, (int) (diff / nanoHerz));
        int operationCount = 0;
        do {
            work.run();
        } while (++operationCount < operationsToDo);
    }

}
