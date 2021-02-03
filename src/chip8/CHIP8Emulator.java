package chip8;

import chip8.cpu.CPU;
import chip8.cpu.ExecutionResult;
import chip8.hardware.ClockSimulator;
import chip8.hardware.Display;
import chip8.hardware.Keyboard;
import chip8.hardware.PCSpeaker;
import chip8.hardware.ColorPalette;
import chip8.ui.ControlsListener;
import chip8.ui.ControlsView;
import chip8.util.Utilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

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

    private static void setupGraphicsSystem(CPU cpu, ControlsListener listener) {
        JFrame frame = new JFrame("CHIP8 Emulator");
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel displayPanel = new JPanel(new BorderLayout());
        Display view = new Display(frame, cpu);

        JPanel displayBorder = new JPanel();
        displayBorder.setBorder(new EmptyBorder(4, 4, 4, 4));
        displayBorder.add(view);
        displayPanel.add(displayBorder, BorderLayout.CENTER);

        ControlsView controlsView = new ControlsView(cpu);
        controlsView.addControlsListener(listener);
        controlsView.addControlsListener(new ControlsListener() {
            @Override
            public void colorPaletteChanged(ColorPalette selectedPalette) {
                view.setColorPalette(selectedPalette);
            }
        });
        displayPanel.add(controlsView, BorderLayout.SOUTH);
        mainPanel.add(displayPanel, BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int response = JOptionPane.showConfirmDialog(
                        frame,
                        "Are you sure you want to close this window?", "Close Window?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (response == JOptionPane.YES_OPTION){
                    Props.writeProperties();
                    System.exit(0);
                }
            }
        });
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1024, 520);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

    private static void runRom(CPU cpu, File romFile) {
        try {
            cpu.initAndLoadRom(romFile);
            ClockSimulator cpuClock = new ClockSimulator(500);
            cpuClock.withClockRegulation(() -> {
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
    }

    // -------------------- Main Method --------------------

    public static void main(String[] args) throws Exception {
        Keyboard keyboard = new Keyboard();
        PCSpeaker speaker = new PCSpeaker();
        CPU cpu = new CPU(keyboard, speaker);

        ControlsListener listener = new ControlsListener() {
            @Override
            public void shouldEndWait() {
                Utilities.invokeInBackground(cpu::endWait);
            }
            @Override
            public void setVolume(double volume) {
                Utilities.invokeInBackground(() -> speaker.setVolume(volume));
            }

            @Override
            public void shouldWaitChanged(boolean shouldWait) {
                Utilities.invokeInBackground(() -> cpu.setShouldWait(shouldWait));
            }

            @Override
            public void romSelected(File romFile) {
                Utilities.invokeInBackground(() -> runRom(cpu, romFile));
            }
        };

        // init the graphics system and show the UI
        SwingUtilities.invokeAndWait(() -> setupGraphicsSystem(cpu, listener));
    }
}
