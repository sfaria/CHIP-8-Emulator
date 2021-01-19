package chip8;

import javax.swing.JFrame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import static java.awt.event.KeyEvent.*;
/**
 *
 * @author Scott Faria <scott.faria@protonmailcom>
 */
final class Keyboard {

    private Map<Integer, Integer> keyMap = new HashMap<>() {{
        put(VK_X, 0x0);
        put(VK_1, 0x1);
        put(VK_2, 0x2);
        put(VK_3, 0x3);
        put(VK_Q, 0x4);
        put(VK_W, 0x5);
        put(VK_E, 0x6);
        put(VK_A, 0x7);
        put(VK_S, 0x8);
        put(VK_D, 0x9);
        put(VK_Z, 0xA);
        put(VK_C, 0xB);
        put(VK_4, 0xC);
        put(VK_R, 0xD);
        put(VK_F, 0xE);
        put(VK_V, 0xF);
    }};

    // 0x0 - 0xF, 16 keys
    private final boolean[] keys = new boolean[16];
    private final Object mutex = new Object();

    final void attachKeyboard(JFrame keyPressProvider) {
        KeyAdapter keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getModifiersEx() == 0 && keyMap.containsKey(e.getKeyCode())) {
                    Integer keystroke = keyMap.get(e.getKeyCode());
                    System.out.println("Key Pressed: ${keystroke}");
                    synchronized (mutex) {
                        keys[keystroke] = true;
                    }
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
//                if (e.modifiers == 0 && keyMap.containsKey(e.keyCode)) {
//                    def keystroke = keyMap[e.keyCode]
//                    println("Key Released: ${keystroke}")
//                    synchronized (mutex) {
//                        keys[keystroke] = false
//                    }
//                }
            }
        };
        keyPressProvider.addKeyListener(keyListener);
    }

    final int waitForKeyPress() {
        while (true) {
            synchronized (mutex) {
                for (int i = 0; i < keys.length; i++) {
                    boolean key = keys[i];
                    if (key) {
                       return i;
                    }
                }
            }
        }
    }

    final boolean isPressed(int key) {
        synchronized (mutex) {
            return keys[key];
        }
    }
}
