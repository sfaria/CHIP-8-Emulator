package chip8;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class RegisterView extends JComponent {

    // -------------------- Private Variables --------------------

    private final Executor ex = Executors.newSingleThreadExecutor();

    // -------------------- Constructors --------------------

    RegisterView(CPU cpu, Breakpointer breakpointer) {
        Objects.requireNonNull(cpu);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 8, 8, 8));
        setPreferredSize(new Dimension(640, 200));
        JPanel registerPanel = new JPanel(new GridLayout(8, 2));
        registerPanel.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 2, true), "Registers"));
        for (int registerIndex = 0; registerIndex < MachineState.REGISTER_COUNT; registerIndex++) {
            final int index = registerIndex;
            JLabel label = new JLabel("v[" + index + "]: ");
            registerPanel.add(label);
            StateLabelRenderer renderer = new StateLabelRenderer((state) -> Utilities.toHex(state.getRegisterAt(index)));
            label.setLabelFor(renderer);
            registerPanel.add(renderer);
        }
        add(registerPanel, BorderLayout.CENTER);

        boolean startWaiting = breakpointer.isWaiting();
        JButton playButton = new JButton(new ImageIcon("res/play.png"));
        playButton.addActionListener(e -> ex.execute(breakpointer::endWait));

        JCheckBox waitBox = new JCheckBox("Enable Breakpoint", startWaiting);
        waitBox.addItemListener(e -> {
            boolean doWait = e.getStateChange() == ItemEvent.SELECTED;
            ex.execute(() -> breakpointer.setShouldWait(doWait));
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonPanel.add(playButton);
        buttonPanel.add(waitBox);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new TitledBorder(new LineBorder(Color.GRAY, 2, true), "Operations"));
        controlPanel.add(buttonPanel, BorderLayout.NORTH);

        JPanel operationPanel = new JPanel(new GridLayout(4, 1, 0, 4));
        operationPanel.setBorder(new EmptyBorder(12, 8, 8, 8));
        operationPanel.add(new JLabel("Current Opcode:"));
        operationPanel.add(new StateLabelRenderer(state -> state.getCurrentOperation().asHexString()));
        operationPanel.add(new JLabel("Next Opcode:"));
        operationPanel.add(new StateLabelRenderer(state -> state.getNextOperation().asHexString()));
        controlPanel.add(operationPanel, BorderLayout.CENTER);

        add(controlPanel, BorderLayout.EAST);

        cpu.addDebuggerListener(this::fireStateChanged);
    }

    // -------------------- Private Methods --------------------

    private void fireStateChanged(MachineState currentState) {
        SwingUtilities.invokeLater(() -> firePropertyChange("machineStateChanged", null, currentState));
    }

    private final class StateLabelRenderer extends JLabel {
        private StateLabelRenderer(Function<MachineState, String> valueRenderer) {
            super("");
            RegisterView.this.addPropertyChangeListener("machineStateChanged", evt -> {
                MachineState state = (MachineState) evt.getNewValue();
                if (state != null) {
                    setText(valueRenderer.apply(state));
                } else {
                    setText("");
                }
            });
        }
    }
}
