package chip8;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Scott Faria <scott.faria@protonmail.com>
 */
final class Breakpointer {

    // -------------------- Private Variables --------------------

    private boolean wait;
    private boolean isWaiting;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    // -------------------- Constructors --------------------

    Breakpointer(boolean startWait) {
        this.wait = startWait;
        this.isWaiting = startWait;
    }

    // -------------------- Default Methods --------------------

    final void endWait() {
        lock.lock();
        try {
            this.isWaiting = false;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    final void setShouldWait(boolean shouldWait) {
        lock.lock();
        try {
            this.wait = shouldWait;
        } finally {
            lock.unlock();
        }
    }

    final boolean isWaiting() {
        lock.lock();
        try {
            return wait;
        } finally {
            lock.unlock();
        }
    }

    final void waitUntilStepOver() {
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
