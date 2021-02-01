package chip8.ui;

import chip8.cpu.CPU;
import chip8.util.Utilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Hashtable;
import java.util.Objects;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class ControlsView extends JComponent {

    // -------------------- Private Variables --------------------

    private final EventListenerList ll = new EventListenerList();

    // -------------------- Constructors --------------------

    public ControlsView(CPU cpu) {
        Objects.requireNonNull(cpu);
        cpu.addDebuggerListener(this::fireMachineStateChanged);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 8, 8, 8));
        setPreferredSize(new Dimension(640, 200));
        add(createRegisterPanel(), BorderLayout.CENTER);
        add(createBreakpointPanel(), BorderLayout.EAST);
        add(createControlsPanel(), BorderLayout.WEST);
    }

    // -------------------- Public Methods --------------------

    public final void addControlsListener(ControlsListener l) {
        ll.add(ControlsListener.class,  Objects.requireNonNull(l));
    }

    // -------------------- Private Methods --------------------

    private JPanel createControlsPanel() {
        JPanel openFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton openFileButton = new JButton(new ImageIcon("res/folder.png"));
        DynamicLabel<File> selectedFileLabel = new DynamicLabel<>(this, "fileSelected", "<No ROM Loaded!>", File::getName);
        openFilePanel.add(openFileButton);
        openFilePanel.add(selectedFileLabel);
        openFileButton.addActionListener(new OpenFileAction());

        JPanel volumePanel = new JPanel(new GridLayout(2, 1, 8, 4));
        volumePanel.add(new JLabel("Volume Control:"));

        JSlider volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        volumeSlider.setMajorTickSpacing(10);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setLabelTable(new Hashtable<>() {{
            put(0, new JLabel("Min"));
            put(100, new JLabel("Max"));
        }});
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting()) {
                int volume = volumeSlider.getValue();
                double adjustedVolume = (double) volume / 100.0d;
                fireVolumeChanged(adjustedVolume);
            }
        });
        volumePanel.add(volumeSlider);

        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 2, true), "Controls"));
        controlsPanel.add(openFilePanel, BorderLayout.NORTH);
        controlsPanel.add(volumePanel, BorderLayout.CENTER);
        return controlsPanel;
    }

    private JPanel createRegisterPanel() {
        JPanel registerPanel = new JPanel(new GridLayout(8, 2));
        registerPanel.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 2, true), "Registers"));
        for (int registerIndex = 0; registerIndex < MachineState.REGISTER_COUNT; registerIndex++) {
            final int index = registerIndex;
            JLabel label = new JLabel("v[" + index + "]: ");
            registerPanel.add(label);
            DynamicLabel<MachineState> renderer = new DynamicLabel<>(
                    this, "machineStateChanged", "",
                    (state) -> Utilities.toHex(state.getRegisterAt(index))
            );
            label.setLabelFor(renderer);
            registerPanel.add(renderer);
        }
        return registerPanel;
    }

    private JPanel createBreakpointPanel() {
        JButton playButton = new JButton(new ImageIcon("res/play.png"));
        playButton.addActionListener(e -> fireEndWait());

        JCheckBox waitBox = new JCheckBox("Enable Breakpoint", false);
        waitBox.addItemListener(e -> fireWaitChanged(e.getStateChange() == ItemEvent.SELECTED));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonPanel.add(playButton);
        buttonPanel.add(waitBox);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 2, true), "Operations"));
        controlPanel.add(buttonPanel, BorderLayout.NORTH);

        JPanel operationPanel = new JPanel(new GridLayout(4, 1, 0, 4));
        operationPanel.setBorder(new EmptyBorder(12, 8, 8, 8));
        operationPanel.add(new JLabel("Current Opcode:"));
        operationPanel.add(new DynamicLabel<MachineState>(
                this, "machineStateChange", "",
                state -> state.getCurrentOperation().asHexString())
        );
        operationPanel.add(new JLabel("Next Opcode:"));
        operationPanel.add(new DynamicLabel<MachineState>(
                this, "machineStateChange",
                "", state -> state.getNextOperation().asHexString())
        );
        controlPanel.add(operationPanel, BorderLayout.CENTER);
        return controlPanel;
    }

    private void fireVolumeChanged(double volume) {
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.setVolume(volume);
        }
    }

    private void fireWaitChanged(boolean doWait) {
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.shouldWaitChanged(doWait);
        }
    }

    private void fireEndWait() {
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.shouldEndWait();
        }
    }

    private void fireMachineStateChanged(MachineState currentState) {
        SwingUtilities.invokeLater(() -> firePropertyChange("machineStateChanged", null, currentState));
    }

    private void fireFileOpened(File file) {
        SwingUtilities.invokeLater(() -> firePropertyChange("fileSelected", null, file));
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.romSelected(file);
        }
    }

    // -------------------- Inner Classes --------------------

    private final class OpenFileAction implements ActionListener {
        private File lastDirectoryOpened = getSavedLocation();

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser(lastDirectoryOpened);
            fileChooser.setDialogTitle("Select a CHIP-8 ROM!");
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(ControlsView.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (selectedFile != null && selectedFile.exists()) {
                    lastDirectoryOpened = selectedFile.getParentFile();
                    writeSavedLocation(lastDirectoryOpened);
                }
                fireFileOpened(selectedFile);
            }
        }

        private File getSavedLocation() {
            if (lastDirectoryOpened == null) {
                File config = Path.of(System.getenv("LOCALAPPDATA"))
                        .resolve("chip8")
                        .resolve("last_opened")
                        .toAbsolutePath()
                        .toFile();
                if (!config.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    config.getParentFile().mkdirs();
                    return null;
                } else {
                    File dir = readDir(config);
                    if (dir == null || !dir.exists()) {
                        return null;
                    }
                    lastDirectoryOpened = dir;
                }
            }
            return lastDirectoryOpened;
        }

        private void writeSavedLocation(File dir) {
            File config = Path.of(System.getenv("LOCALAPPDATA"))
                    .resolve("chip8")
                    .resolve("last_opened")
                    .toAbsolutePath()
                    .toFile();

            if (!config.exists()) {
                //noinspection ResultOfMethodCallIgnored
                config.getParentFile().mkdirs();
            }

            try {
                Files.writeString(config.toPath(), dir.getAbsolutePath());
            } catch (IOException ignore) {}
        }

        private File readDir(File config) {
            try {
                return Files.readAllLines(config.toPath()).stream()
                        .findFirst()
                        .map(File::new)
                        .orElse(null);
            } catch (IOException ignore) {
                return null;
            }
        }
    }

}
