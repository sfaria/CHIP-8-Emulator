package chip8;

import java.util.EventListener;
import java.util.List;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public interface DebuggerListener extends EventListener {
    void executionStarted(MachineState currentState, List<OperationInfo> operations);
    void machineStateChanged(MachineState currentState);
}
