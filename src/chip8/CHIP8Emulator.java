package chip8;

import chip8.cpu.Breakpointer;
import chip8.cpu.CPU;
import chip8.cpu.ExecutionResult;
import chip8.hardware.ClockSimulator;
import chip8.hardware.Display;
import chip8.hardware.Keyboard;
import chip8.hardware.PCSpeaker;
import chip8.ui.RegisterView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 *
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class CHIP8Emulator {

    // -------------------- Static Init --------------------

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    // -------------------- Private Static Methods --------------------

    private static void setupGraphicsSystem(CPU cpu, Keyboard keyboard, Breakpointer breakpointer) {
        assert !SwingUtilities.isEventDispatchThread();
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel displayPanel = new JPanel(new BorderLayout());
        Display view = new Display(cpu);

        JPanel displayBorder = new JPanel();
        displayBorder.setBorder(new EmptyBorder(4, 4, 4, 4));
        displayBorder.add(view);
        displayPanel.add(displayBorder, BorderLayout.CENTER);

        RegisterView registerView = new RegisterView(cpu, breakpointer);
        displayPanel.add(registerView, BorderLayout.SOUTH);
        mainPanel.add(displayPanel, BorderLayout.CENTER);

        JFrame frame = new JFrame("CHIP8 Emulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1024, 520);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
        view.startRendering();
    }

    // -------------------- Main Method --------------------

    public static void main(String[] args) throws Exception {
        Keyboard keyboard = new Keyboard();
        PCSpeaker speaker = new PCSpeaker(0.5d);
        CPU cpu = new CPU(keyboard, speaker);

        boolean startWaiting = false;
        @SuppressWarnings("ConstantConditions")
        Breakpointer breakpointer = new Breakpointer(startWaiting);
        SwingUtilities.invokeAndWait(() -> setupGraphicsSystem(cpu, keyboard, breakpointer));

        cpu.initAndLoadRom("res/space_invaders.ch8");
//        breakpointer.waitUntilStepOver();

        ClockSimulator cpuClock = new ClockSimulator(500);
        cpuClock.withClockRegulation(() -> {
            ExecutionResult result = cpu.emulateCycle();
            switch (result) {
                case OK -> {
//                    breakpointer.waitUntilStepOver();
                    return true;
                }
                case END_PROGRAM, FATAL -> {
                    return false;
                }
            }
            return false;
        });
    }
}
