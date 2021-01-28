package chip8.cpu;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Scott Faria <scott.faria@protonmail.com>
 */
public final class Breakpointer {

    // -------------------- Private Variables --------------------

    private boolean wait;
    private boolean isWaiting;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    // -------------------- Constructors --------------------

    public Breakpointer(boolean startWait) {
        this.wait = startWait;
        this.isWaiting = startWait;
    }

    // -------------------- Public Methods --------------------

    public final void endWait() {
        lock.lock();
        try {
            this.isWaiting = false;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public final void setShouldWait(boolean shouldWait) {
        lock.lock();
        try {
            this.wait = shouldWait;
        } finally {
            lock.unlock();
        }
    }


    public final void waitForSignal() {
        lock.lock();
        if (!wait) {
            return;
        }

        isWaiting = true;
        try {
            while (isWaiting) {
                try {
                    condition.await();
                } catch (InterruptedException ignore) {}
            }
        } finally {
            lock.unlock();
        }
    }

}
