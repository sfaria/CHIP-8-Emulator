package chip8;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class InstructionView extends JComponent {

    // -------------------- Private Variables --------------------

    private int initialProgramCounter = 0;
    private int currentProgramCounter = 0;
    private List<OperationInfo> operations = List.of();

    // -------------------- Constructors --------------------

    InstructionView(CPU cpu) {
        Objects.requireNonNull(cpu);
        JList<OperationInfo> operationList = new JList<>(new ListModel());
        operationList.setCellRenderer(new Renderer());
        operationList.setSelectionModel(new SelectionModel());
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 520));
        add(operationList, BorderLayout.CENTER);
        setBorder(new EtchedBorder());

        cpu.addDebuggerListener(new DebuggerListener() {
            @Override
            public void executionStarted(MachineState currentState, List<OperationInfo> operations) {
                fireExecutionStarted(currentState, operations);
            }

            @Override
            public void machineStateChanged(MachineState currentState) {
                fireMachineStateChanged(currentState);
            }
        });
    }

    // -------------------- Private Methods --------------------

    private void fireMachineStateChanged(MachineState currentState) {
        SwingUtilities.invokeLater(() -> {
            this.currentProgramCounter = currentState.getProgramCounter();
            repaint();
        });
    }

    private void fireExecutionStarted(MachineState currentState, List<OperationInfo> operations) {
        SwingUtilities.invokeLater(() -> {
            this.initialProgramCounter = currentState.getProgramCounter();
            this.currentProgramCounter = currentState.getProgramCounter();
            this.operations = List.copyOf(operations);
            repaint();
        });
    }

    // -------------------- Inner Classes --------------------

    private class SelectionModel extends DefaultListSelectionModel {
        @Override
        public final boolean isSelectedIndex(int index) {
            return index == (currentProgramCounter - initialProgramCounter);
        }
    }

    private final class ListModel extends AbstractListModel<OperationInfo> {
        @Override
        public final int getSize() {
            return operations.size();
        }

        @Override
        public final OperationInfo getElementAt(int index) {
            return operations.get(index);
        }
    }

    private class Renderer extends JLabel implements ListCellRenderer<OperationInfo> {
        @Override
        public final Component getListCellRendererComponent(JList<? extends OperationInfo> list, OperationInfo value, int index, boolean isSelected, boolean cellHasFocus) {
            int adjustedIndex = initialProgramCounter + index; // this is our "memory" index to render, which isn't the first index into system mem
            String memoryIndex = Utilities.toHex(adjustedIndex);
            String deco = isSelected ? "* " : "";
            setToolTipText(deco + memoryIndex + ": " + value.asHexString());
            return this;
        }
    }
}
