package chip8;

import javax.swing.*;
import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class CHIP8Emulator {

    private static Display setupGraphicsSystem(CPU cpu, Keyboard keyboard) throws InvocationTargetException, InterruptedException {
        JPanel mainPanel = new JPanel(new BorderLayout());
        Display view = new Display(cpu);
        mainPanel.add(view, BorderLayout.CENTER);

        InstructionView instructionView = new InstructionView(cpu);
        mainPanel.add(instructionView, BorderLayout.EAST);

        RegisterView registerView = new RegisterView(cpu);
        mainPanel.add(registerView, BorderLayout.SOUTH);

        JFrame frame = new JFrame("CHIP8 Emulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        return view;
    }

    public static void main(String[] args) throws Exception {
        CPU cpu = new CPU();
        Keyboard keyboard = new Keyboard();
        cpu.init(keyboard);
        cpu.loadRom("res/c8_test.c8");

        Display view = setupGraphicsSystem(cpu, keyboard);
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
