package chip8.ui;

import chip8.Props;
import chip8.cpu.CPU;
import chip8.hardware.ColorPalette;
import chip8.hardware.Palettes;
import chip8.util.Utilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
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
        cpu.addDebuggerListener(new DebuggerListener() {
            @Override
            public void machineStateChanged(MachineState currentState) {
                fireMachineStateChanged(currentState);
            }
            @Override
            public void machineStopped() {
                SwingUtilities.invokeLater(() -> firePropertyChange("fileSelected", null, null));
            }
        });
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 8, 8, 8));
        setPreferredSize(new Dimension(640, 240));
        add(createRegisterPanel(), BorderLayout.CENTER);
        add(createBreakpointPanel(cpu), BorderLayout.EAST);
        add(createControlsPanel(), BorderLayout.WEST);
    }

    // -------------------- Public Methods --------------------

    public final void addControlsListener(ControlsListener l) {
        ll.add(ControlsListener.class,  Objects.requireNonNull(l));
    }

    // -------------------- Private Methods --------------------

    private JPanel createControlsPanel() {
        JPanel openFilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton openFileButton = new JButton(new OpenROMAction());
        DynamicLabel<File> selectedFileLabel = new DynamicLabel<>(this, "fileSelected", "<No ROM Loaded!>", File::getName);
        openFilePanel.add(openFileButton);
        openFilePanel.add(selectedFileLabel);

        JPanel volumePanel = new JPanel(new GridLayout(2, 1, 8, 4));
        volumePanel.add(new JLabel("Volume Control:"));

        int vol = (int) (Props.getSavedVolume() * 100.0d);
        JSlider volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, vol);
        volumeSlider.setMajorTickSpacing(10);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(false);
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting()) {
                int volume = volumeSlider.getValue();
                double adjustedVolume = (double) volume / 100.0d;
                fireVolumeChanged(adjustedVolume);
                Props.setSavedVolume(adjustedVolume);
            }
        });
        volumePanel.add(volumeSlider);

        JPanel cpuPanel = new JPanel(new GridLayout(2, 1, 8, 4));
        cpuPanel.add(new JLabel("CPU Frequency (Hz): "));
        SpinnerNumberModel model = new SpinnerNumberModel(Props.getSavedCPUClockSpeed(), 100, 900, 100);
        JSpinner freqSpinner = new JSpinner(model);
        JFormattedTextField tf = ((JSpinner.DefaultEditor) freqSpinner.getEditor()).getTextField();
        tf.setEnabled(false);
        tf.setForeground(Color.BLACK);
        tf.setBackground(Color.WHITE);
        tf.setHorizontalAlignment(JTextField.LEFT);
        cpuPanel.add(freqSpinner);

        freqSpinner.addChangeListener(e -> {
            int frequency = (int) model.getNumber();
            fireFrequencyChanged(frequency);
        });

        JPanel palettePanel = new JPanel(new GridLayout(2, 1, 8, 4));
        JComboBox<ColorPalette> paletteJComboBox = new JComboBox<>(new PaletteComboModel());
        paletteJComboBox.setRenderer(new PaletteRenderer());
        palettePanel.add(new JLabel("Color Palette:"));
        palettePanel.add(paletteJComboBox);

        paletteJComboBox.addActionListener(e -> {
            ColorPalette palette = (ColorPalette) paletteJComboBox.getSelectedItem();
            firePaletteChanged(palette);
        });

        JPanel controlsPanel = new JPanel(new GridLayout(4, 1, 8, 4));
        controlsPanel.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 2, true), "Controls"));
        controlsPanel.add(openFilePanel);
        controlsPanel.add(cpuPanel);
        controlsPanel.add(palettePanel);
        controlsPanel.add(volumePanel);
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
                    (state) -> state.getRegisterAt(index)
            );
            label.setLabelFor(renderer);
            registerPanel.add(renderer);
        }
        return registerPanel;
    }

    private JPanel createBreakpointPanel(CPU cpu) {
        JButton playButton = new JButton(new ImageIcon("res/play.png"));
        playButton.addActionListener(e -> fireEndWait());
        playButton.setEnabled(false);

        JButton stopButton = new JButton(new ImageIcon("res/stop.png"));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> fireStop());

        cpu.addDebuggerListener(new DebuggerListener() {
            @Override public void machineStarted() {
                stopButton.setEnabled(true);
                playButton.setEnabled(true);

            }
            @Override public void machineStopped() {
                stopButton.setEnabled(false);
                playButton.setEnabled(false);
            }
        });

        JCheckBox waitBox = new JCheckBox("Enable Breakpoint", false);
        waitBox.addItemListener(e -> fireWaitChanged(e.getStateChange() == ItemEvent.SELECTED));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonPanel.add(stopButton);
        buttonPanel.add(playButton);
        buttonPanel.add(waitBox);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 2, true), "Operations"));
        controlPanel.add(buttonPanel, BorderLayout.NORTH);

        JPanel operationPanel = new JPanel(new GridLayout(6, 1, 0, 4));
        operationPanel.setBorder(new EmptyBorder(12, 8, 8, 8));

        operationPanel.add(new JLabel("Program Counter: "));
        operationPanel.add(new DynamicLabel<MachineState>(
                this, "machineStateChanged", "",
                MachineState::getProgramCounter
        ));

        operationPanel.add(new JLabel("Current Opcode:"));
        operationPanel.add(new DynamicLabel<MachineState>(
                this, "machineStateChanged", "",
                state -> state.getCurrentOperation().asHexString())
        );
        operationPanel.add(new JLabel("Next Opcode:"));
        operationPanel.add(new DynamicLabel<MachineState>(
                this, "machineStateChanged",
                "", state -> state.getNextOperation().asHexString())
        );
        controlPanel.add(operationPanel, BorderLayout.CENTER);
        return controlPanel;
    }

    private void fireFrequencyChanged(int frequency) {
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.cpuSpeedChanged(frequency);
        }
    }

    private void fireVolumeChanged(double volume) {
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.setVolume(volume);
        }
    }

    private void firePaletteChanged(ColorPalette palette) {
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.colorPaletteChanged(palette);
        }
    }

    private void fireWaitChanged(boolean doWait) {
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.shouldWaitChanged(doWait);
        }
    }

    private void fireStop() {
        for (ControlsListener l : ll.getListeners(ControlsListener.class)) {
            l.stopEmulator();
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

    private static final class PaletteRenderer extends JLabel implements ListCellRenderer<ColorPalette> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ColorPalette> list, ColorPalette value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.displayName());
            return this;
        }
    }

    private static final class PaletteComboModel extends AbstractListModel<ColorPalette> implements ComboBoxModel<ColorPalette> {
        private ColorPalette selectedItem = Props.getSavedPalette();

        @Override public int getSize() {
            return Palettes.ALL_PALETTES.size();
        }

        @Override public ColorPalette getElementAt(int index) {
            return Palettes.ALL_PALETTES.get(index);
        }

        @Override public void setSelectedItem(Object anItem) {
            selectedItem = (ColorPalette) anItem;
            Props.setSelectedPalette(selectedItem);
        }

        @Override public Object getSelectedItem() {
            return selectedItem;
        }
    }

    private final class OpenROMAction extends AbstractAction {
        OpenROMAction() {
            setIcon("folder.png");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setIcon("loading.gif");
            Utilities.invokeInBackground(() -> {
                final File lastDirectoryOpened = Props.getSavedROMLocation();
                SwingUtilities.invokeLater(() -> {
                    setIcon("folder.png");
                    JFileChooser fileChooser = new JFileChooser(lastDirectoryOpened);
                    fileChooser.setDialogTitle("Select a CHIP-8 ROM!");
                    fileChooser.setMultiSelectionEnabled(false);
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int result = fileChooser.showOpenDialog(ControlsView.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        if (selectedFile != null && selectedFile.exists()) {
                            File newDir = selectedFile.getParentFile();
                            Props.setSavedROMLocation(newDir);
                        }
                        fireFileOpened(selectedFile);
                    }
                });
            });
        }

        private void setIcon(String name) {
            putValue(Action.SMALL_ICON, new ImageIcon("res/%s".formatted(name)));
        }

    }

}
