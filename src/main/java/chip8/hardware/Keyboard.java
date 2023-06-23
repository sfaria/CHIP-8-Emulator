package chip8.hardware;

import chip8.util.Utilities;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.awt.event.KeyEvent.*;
/**
 *
 * @author Scott Faria <scott.faria@protonmailcom>
 */
public final class Keyboard {

    // -------------------- Private Statics --------------------

    private static final Map<Integer, Byte> KEY_MAP = new HashMap<>() {{
        put(VK_X, (byte) 0x0);
        put(VK_1, (byte) 0x1);
        put(VK_2, (byte) 0x2);
        put(VK_3, (byte) 0x3);
        put(VK_Q, (byte) 0x4);
        put(VK_W, (byte) 0x5);
        put(VK_E, (byte) 0x6);
        put(VK_A, (byte) 0x7);
        put(VK_S, (byte) 0x8);
        put(VK_D, (byte) 0x9);
        put(VK_Z, (byte) 0xA);
        put(VK_C, (byte) 0xB);
        put(VK_4, (byte) 0xC);
        put(VK_R, (byte) 0xD);
        put(VK_F, (byte) 0xE);
        put(VK_V, (byte) 0xF);
    }};

    // -------------------- Private Variables --------------------

    private final boolean[] keyState = new boolean[16];
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    // -------------------- Constructors --------------------

    public Keyboard() {
        Arrays.fill(keyState, false);
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(e -> {
            if (e.getModifiersEx() == 0) {
                int keyCode = e.getKeyCode();
                switch (e.getID()) {
                    case KEY_PRESSED:
                        if (KEY_MAP.containsKey(keyCode)) {
                            byte keyIndex = KEY_MAP.getOrDefault(keyCode, (byte) -1);
                            if (keyIndex != -1) {
                                Utilities.invokeInBackground(() -> pressKey(keyIndex));
                            }

                        }
                        break;
                    case KEY_RELEASED:
                        byte keyIndex = KEY_MAP.getOrDefault(keyCode, (byte) -1);
                        if (keyIndex != -1) {
                            Utilities.invokeInBackground(() -> releaseKey(keyIndex));
                        }
                        break;
                }
                return false;
            }
            return true;
        });
    }


    // -------------------- Default Methods --------------------

    public final byte waitForKeyPress() {
        lock.lock();
        try {
            byte keyPressed = firstKeyPressed();
            while (keyPressed == -1) {
                try {
                    condition.await();
                } catch (InterruptedException ignore) {}

                keyPressed = firstKeyPressed();
            }
            return keyPressed;
        } finally {
            lock.unlock();
        }
    }

    public final boolean isPressed(byte key) {
        if (key < 0 || key >= keyState.length) {
            return false;
        }

        lock.lock();
        try {
            return keyState[key];
        } finally {
            lock.unlock();
        }
    }

    // -------------------- Private Methods --------------------

    private void pressKey(byte keyIndex) {
        lock.lock();
        try {
            keyState[keyIndex] = true;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void releaseKey(byte keyIndex) {
        lock.lock();
        try {
            keyState[keyIndex] = false;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private byte firstKeyPressed() {
        assert lock.isLocked();
        for (byte i = 0; i < keyState.length; i++) {
            boolean pressed = keyState[i];
            if (pressed) {
               return i;
            }
        }
        return -1;
    }

}
