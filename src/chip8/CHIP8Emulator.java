package chip8;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.io.IOException;

/**
 *
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class CHIP8Emulator {

    private static Graphics setupGraphicsSystem(CPU cpu, Keyboard keyboard) {
        Graphics view = new Graphics(cpu);
        JFrame frame = new JFrame("CHIP8 Emulator");
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(view.getWidth(), view.getHeight());
            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(view, BorderLayout.CENTER);
            frame.setVisible(true);
        });
        keyboard.attachKeyboard(frame);
        return view;
    }

    public static void main(String[] args) throws Exception {
        CPU cpu = new CPU();
        Keyboard keyboard = new Keyboard();
        cpu.init(keyboard);
        cpu.loadRom("res/c8_test.c8");

        Graphics view = setupGraphicsSystem(cpu, keyboard);
//        long wait = (long) (1.0f / 60.0f) * 1000;

        //noinspection InfiniteLoopStatement
        while (true) {
//            Thread.sleep(wait);
            cpu.emulateCycle(() -> {
                SwingUtilities.invokeLater(view::repaint);
            });
        }
    }
}
