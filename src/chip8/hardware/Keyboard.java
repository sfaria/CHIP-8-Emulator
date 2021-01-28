package chip8.hardware;

import chip8.cpu.Breakpointer;
import chip8.util.Utilities;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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

    private byte currentKey = -1;
    private final Breakpointer waiter = new Breakpointer(true);

    // -------------------- Constructors --------------------

    public Keyboard() {
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(e -> {
            switch (e.getID()) {
                case KEY_PRESSED:
                    if (e.getModifiersEx() == 0 && KEY_MAP.containsKey(e.getKeyCode())) {
                        byte keystroke = KEY_MAP.get(e.getKeyCode());
                        System.out.printf("Key Pressed: %s%n", Utilities.toHex(keystroke));
                        currentKey = keystroke;
                        waiter.endWait();
                    }
                    break;
                case KEY_RELEASED:
                    Byte keystroke = KEY_MAP.get(e.getKeyCode());
                    byte key = keystroke == null ? -1 : keystroke;
                    if (e.getModifiersEx() == 0 && Utilities.isEqual(key, currentKey)) {
                        System.out.printf("Key Released: %s%n", Utilities.toHex(key));
                        currentKey = -1;
                    }
                    break;
            }
            return false;
        });
    }

    // -------------------- Default Methods --------------------

    public final byte waitForKeyPress() {
        if (currentKey == -1) {
            waiter.waitForSignal();
        }
        return currentKey;
    }

    public final boolean isPressed(byte key) {
        return Utilities.isEqual(key, currentKey);
    }
}
