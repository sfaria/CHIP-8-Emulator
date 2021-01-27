package chip8;

import java.util.EventListener;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public interface DebuggerListener extends EventListener {
    void machineStateChanged(MachineState currentState);
}
