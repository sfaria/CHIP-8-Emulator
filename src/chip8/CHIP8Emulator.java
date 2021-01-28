package chip8;

import chip8.cpu.Breakpointer;
import chip8.cpu.CPU;
import chip8.cpu.ExecutionResult;
import chip8.hardware.ClockSimulator;
import chip8.hardware.Display;
import chip8.hardware.Keyboard;
import chip8.hardware.PCSpeaker;
import chip8.ui.ControlsListener;
import chip8.ui.ControlsView;
import chip8.util.Utilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private static final ExecutorService EX = Executors.newSingleThreadExecutor();

    // -------------------- Private Static Methods --------------------

    private static void setupGraphicsSystem(CPU cpu, ControlsListener listener) {
        assert !SwingUtilities.isEventDispatchThread();
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel displayPanel = new JPanel(new BorderLayout());
        Display view = new Display(cpu);

        JPanel displayBorder = new JPanel();
        displayBorder.setBorder(new EmptyBorder(4, 4, 4, 4));
        displayBorder.add(view);
        displayPanel.add(displayBorder, BorderLayout.CENTER);

        ControlsView controlsView = new ControlsView(cpu);
        controlsView.addControlsListener(listener);
        displayPanel.add(controlsView, BorderLayout.SOUTH);
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
        CPU cpu = new CPU(EX, keyboard, speaker);

        Breakpointer breakpointer = new Breakpointer(false);
        ControlsListener listener = new ControlsListener() {
            @Override
            public void shouldEndWait() {
                Utilities.invokeInBackground(breakpointer::endWait);
            }

            @Override
            public void shouldWaitChanged(boolean shouldWait) {
                EX.submit(() -> breakpointer.setShouldWait(shouldWait));
            }

            @Override
            public void setVolume(double volume) {
                Utilities.invokeInBackground(() -> speaker.setVolume(volume));
            }

            @Override
            public void romSelected(File romFile) {
                EX.submit(() -> {
                    try {
                        cpu.initAndLoadRom(romFile);
                        ClockSimulator cpuClock = new ClockSimulator(500, EX);
                        cpuClock.withClockRegulation(() -> {
                            breakpointer.waitForSignal();
                            ExecutionResult result = cpu.emulateCycle();
                            switch (result) {
                                case OK -> {
                                    return true;
                                }
                                case END_PROGRAM, FATAL -> {
                                    return false;
                                }
                            }
                            return false;
                        });
                    } catch (IOException e) {
                        throw new RuntimeException("Unexpected Exception.", e);
                    }
                });

            }
        };

        // init the graphics system and show the UI
        SwingUtilities.invokeAndWait(() -> setupGraphicsSystem(cpu, listener));
    }
}
