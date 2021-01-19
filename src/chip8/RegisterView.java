package chip8;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class RegisterView extends JComponent {

    // -------------------- Private Variables --------------------

    private MachineState currentState;

    // -------------------- Constructors --------------------

    RegisterView(CPU cpu) {
        Objects.requireNonNull(cpu);
        setLayout(new BorderLayout());

        JPanel registerPanel = new JPanel(new GridLayout(8, 2));
        for (int registerIndex = 0; registerIndex < MachineState.REGISTER_COUNT; registerIndex++) {
            final int index = registerIndex;
            JLabel label = new JLabel("v[" + index + "]: ");
            registerPanel.add(label);
            RegisterRenderer renderer = new RegisterRenderer(this, (state) -> Utilities.toHex(currentState.getRegisterAt(index)));
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
        SwingUtilities.invokeLater(() -> {
            firePropertyChange("machineStateChanged", this.currentState, currentState);
            this.currentState = currentState;
            repaint();
        });
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
