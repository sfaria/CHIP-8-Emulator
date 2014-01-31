package chip8

import javax.swing.JFrame
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.concurrent.Executors
import static java.awt.event.KeyEvent.*
/**
 *
 * @author Scott Faria <scott.faria@gmail.com>
 */
class Keyboard {

    private def keyMap = [
            (VK_X): 0x0,
            (VK_1): 0x1,
            (VK_2): 0x2,
            (VK_3): 0x3,
            (VK_Q): 0x4,
            (VK_W): 0x5,
            (VK_E): 0x6,
            (VK_A): 0x7,
            (VK_S): 0x8,
            (VK_D): 0x9,
            (VK_Z): 0xA,
            (VK_C): 0xB,
            (VK_4): 0xC,
            (VK_R): 0xD,
            (VK_F): 0xE,
            (VK_V): 0xF
    ]

    // 0x0 - 0xF, 16 keys
    private def keys = new boolean[16]
    private final def mutex = new Object()

    def attachKeyboard(JFrame keyPressProvider) {
        def keyListener = new KeyAdapter() {
            @Override
            void keyPressed(KeyEvent e) {
                if (e.modifiers == 0 && keyMap.containsKey(e.keyCode)) {
                    def keystroke = keyMap[e.keyCode]
                    println("Key Pressed: ${keystroke}")
                    synchronized (mutex) {
                        keys[keystroke] = true
                    }
                }
            }
            @Override
            void keyReleased(KeyEvent e) {
//                if (e.modifiers == 0 && keyMap.containsKey(e.keyCode)) {
//                    def keystroke = keyMap[e.keyCode]
//                    println("Key Released: ${keystroke}")
//                    synchronized (mutex) {
//                        keys[keystroke] = false
//                    }
//                }
            }
        }
        keyPressProvider.addKeyListener(keyListener)
    }

    def waitForKeyPress() {
        def pressedIndex = -1
        while (pressedIndex < 0) {
            synchronized (mutex) {
                keys.eachWithIndex { key, index ->
                   if (key) {
                       pressedIndex = index
                       return
                   }
                }
            }
        }
        return pressedIndex
    }

    def getKeyPressState(key) {
        synchronized (mutex) {
            return keys[key]
        }
    }
}
