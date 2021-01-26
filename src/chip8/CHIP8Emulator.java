package chip8;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

/**
 *
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class CHIP8Emulator {

    // -------------------- Static Init --------------------

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ignored) {}
    }

    // -------------------- Private Static Methods --------------------

    private static void setupGraphicsSystem(CPU cpu, Keyboard keyboard, Breakpointer breakpointer) {
        assert !SwingUtilities.isEventDispatchThread();
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel displayPanel = new JPanel(new BorderLayout());
        Display view = new Display(cpu);

        JPanel displayBorder = new JPanel();
        displayBorder.setBorder(new EmptyBorder(4, 4, 4, 0));
        displayBorder.add(view);
        displayPanel.add(displayBorder, BorderLayout.CENTER);

        RegisterView registerView = new RegisterView(cpu);
        displayPanel.add(registerView, BorderLayout.SOUTH);
        mainPanel.add(displayPanel, BorderLayout.CENTER);

        InstructionView instructionView = new InstructionView(cpu, breakpointer);
        mainPanel.add(instructionView, BorderLayout.EAST);

        JFrame frame = new JFrame("CHIP8 Emulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1024, 520);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

    // -------------------- Main Method --------------------

    public static void main(String[] args) throws Exception {
        ClockSimulator cpuClock = new ClockSimulator(500);
        ClockSimulator delayClock = new ClockSimulator(60);
        Keyboard keyboard = new Keyboard();
        CPU cpu = new CPU(keyboard, delayClock);

        boolean startWaiting = true;
        @SuppressWarnings("ConstantConditions")
        Breakpointer breakpointer = new Breakpointer(startWaiting);
        SwingUtilities.invokeAndWait(() -> setupGraphicsSystem(cpu, keyboard, breakpointer));

        cpu.initAndLoadRom("res/test_opcode.ch8");
        breakpointer.waitUntilStepOver();
        cpuClock.startClock();
        delayClock.startClock();

        //noinspection InfiniteLoopStatement
        while (true) {
            cpuClock.withClockRegulation(() -> {
                cpu.emulateCycle();
                breakpointer.waitUntilStepOver();
            });
        }
    }
}
