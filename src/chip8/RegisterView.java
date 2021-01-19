package chip8;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class RegisterView extends JComponent {

    // -------------------- Constructors --------------------

    RegisterView(CPU cpu) {
        Objects.requireNonNull(cpu);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(4, 8, 4, 4));
        setPreferredSize(new Dimension(640, 200));
        JPanel registerPanel = new JPanel(new GridLayout(8, 2));
        for (int registerIndex = 0; registerIndex < MachineState.REGISTER_COUNT; registerIndex++) {
            final int index = registerIndex;
            JLabel label = new JLabel("v[" + index + "]: ");
            registerPanel.add(label);
            RegisterRenderer renderer = new RegisterRenderer(this, (state) -> Utilities.toHex(state.getRegisterAt(index)));
            label.setLabelFor(renderer);
            registerPanel.add(renderer);
        }
        add(registerPanel, BorderLayout.CENTER);

        cpu.addDebuggerListener(new DebuggerListener() {
            @Override
            public void executionStarted(MachineState currentState, List<OperationInfo> operations) {
                fireStateChanged(currentState);
            }

            @Override
            public void machineStateChanged(MachineState currentState) {
                fireStateChanged(currentState);
            }
        });
    }

    // -------------------- Private Methods --------------------

    private void fireStateChanged(MachineState currentState) {
        SwingUtilities.invokeLater(() -> firePropertyChange("machineStateChanged", null, currentState));
    }

    private static final class RegisterRenderer extends JLabel {
        private RegisterRenderer(JComponent parent, Function<MachineState, String> valueRenderer) {
            super("");
            parent.addPropertyChangeListener("machineStateChanged", evt -> {
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
