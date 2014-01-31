package chip8

import javax.swing.JFrame
import javax.swing.SwingUtilities
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

/**
 *
 * @author Scott Faria <scott.faria@gmail.com>
 */
@SuppressWarnings("GroovyInfiniteLoopStatement")
class CHIP8Emulator {

    private static def setupGraphicsSystem(CPU cpu, Keyboard keyboard) {
        def view = new Graphics(graphicsProvider: cpu)
        def frame = new JFrame("CHIP8 Emulator")
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            void run() {
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
                frame.setSize(view.getWidth(), view.getHeight())
                frame.getContentPane().setLayout(new BorderLayout())
                frame.getContentPane().add(view, BorderLayout.CENTER)
                frame.setVisible(true)
            }
        })
        keyboard.attachKeyboard(frame)
        return view
    }

    public static void main(String[] args) {
        def cpu = new CPU()
        def keyboard = new Keyboard()
        cpu.init(keyboard)
        cpu.loadRom("roms/PONG")

        def view = setupGraphicsSystem(cpu, keyboard)
        while (true) {
            cpu.emulateCycle {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    void run() {
                        view.repaint()
                    }
                })
            }
        }
    }
}
