package chip8.ui;

import chip8.ui.MachineState;

import java.util.EventListener;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public interface DebuggerListener extends EventListener {
    default void machineStarted() {}
    default void machineStopped() {}
    default void machineStateChanged(MachineState currentState) {}
}
