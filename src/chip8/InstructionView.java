package chip8;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static javax.swing.ScrollPaneConstants.*;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class InstructionView extends JComponent {

    // -------------------- Private Variables --------------------

    private final Executor ex = Executors.newSingleThreadExecutor();

    // -------------------- Constructors --------------------

    InstructionView(CPU cpu, Breakpointer breakpointer) {
        Objects.requireNonNull(cpu);

        JList<OperationInfo> operationList = new JList<>();
        operationList.setCellRenderer(new Renderer());

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

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 520));
        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(operationList, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        setBorder(new EtchedBorder());

        cpu.addDebuggerListener(new DebuggerListener() {
            @Override
            public void executionStarted(MachineState currentState, List<OperationInfo> operations) {
                fireExecutionStarted(operationList, currentState, operations);
            }

            @Override
            public void machineStateChanged(MachineState currentState) {
                fireMachineStateChanged(operationList, currentState);
            }
        });
    }

    // -------------------- Private Methods --------------------

    private void fireMachineStateChanged(JList<OperationInfo> operationList, MachineState currentState) {
        SwingUtilities.invokeLater(() -> {
            ListModel model = (ListModel) operationList.getModel();
            int initialProgramCounter = model.getInitialProgramCounter();
            int programCounter = currentState.getProgramCounter();
            int index = programCounter - initialProgramCounter;
            operationList.setSelectedIndex(index);
            operationList.ensureIndexIsVisible(index);
        });
    }

    private void fireExecutionStarted(JList<OperationInfo> list, MachineState currentState, List<OperationInfo> operations) {
        SwingUtilities.invokeLater(() -> {
            int initialProgramCounter = currentState.getProgramCounter();
            ListModel model = new ListModel(initialProgramCounter, operations);
            list.setModel(model);
            list.setSelectedIndex(0);
            list.ensureIndexIsVisible(0);
        });
    }

    // -------------------- Inner Classes --------------------

    private static class ListModel extends AbstractListModel<OperationInfo> {
        private final int initialProgramCounter;
        private final List<OperationInfo> operations;

        private ListModel(int initialProgramCounter, List<OperationInfo> operations) {
            this.initialProgramCounter = initialProgramCounter;
            this.operations = operations;
        }

        @Override
        public final int getSize() {
            return operations.size();
        }

        @Override
        public final OperationInfo getElementAt(int index) {
            return operations.get(index);
        }

        private int getInitialProgramCounter() {
            return initialProgramCounter;
        }

    }

    private static class Renderer extends JLabel implements ListCellRenderer<OperationInfo> {
        @Override
        public final Component getListCellRendererComponent(JList<? extends OperationInfo> list, OperationInfo value, int index, boolean isSelected, boolean cellHasFocus) {
            ListModel model = (ListModel) list.getModel();
            int adjustedIndex = model.getInitialProgramCounter() + index; // this is our "memory" index to render, which isn't the first index into system mem
            String memoryIndex = Utilities.toHex(adjustedIndex);
            String deco = isSelected ? "* " : "";
            setText(deco + memoryIndex + ": " + value.asHexString());
            return this;
        }
    }
}
