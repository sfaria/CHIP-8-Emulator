package chip8

import javax.swing.JFrame
import javax.swing.SwingUtilities
import java.awt.BorderLayout

/**
 *
 * @author Scott Faria <scott.faria@gmail.com>
 */
@SuppressWarnings("GroovyInfiniteLoopStatement")
class CHIP8Emulator {

    public static void main(String[] args) {
        def cpu = new CPU()
        cpu.init()
        cpu.loadRom("pong")

        def view = setupGraphicsSystem(cpu)
        while (true) {
            cpu.emulateCycle {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    void run() {
                        view.repaint()
                    }
                })
            }
            cpu.setKeyPressState()
        }
    }

    private static def setupGraphicsSystem(cpu) {
        def view = new View(graphicsProvider: cpu)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            void run() {
                def frame = new JFrame("CHIP8 Emulator")
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
                frame.setSize(view.getWidth(), view.getHeight())
                frame.getContentPane().setLayout(new BorderLayout())
                frame.getContentPane().add(view, BorderLayout.CENTER)
                frame.setVisible(true)
            }
        })
        return view
    }
}
