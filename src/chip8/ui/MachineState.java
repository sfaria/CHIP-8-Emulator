package chip8.ui;

import chip8.cpu.OperationState;
import chip8.util.Utilities;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class MachineState {

    // -------------------- Statics --------------------

    public static final int REGISTER_COUNT = 16;

    // -------------------- Private Variables --------------------

    private final OperationInfo currentOperation;
    private final OperationInfo nextOperation;
    private final short programCounter;
    private final byte[] registerStates;

    // -------------------- Constructors --------------------

    public MachineState(OperationState state, short programCounter, byte[] registerStates) {
        this.currentOperation = state.getCurrent();
        this.nextOperation = state.getNext();
        this.programCounter = programCounter;
        this.registerStates = registerStates;
    }

    // -------------------- Default Methods --------------------

    final OperationInfo getCurrentOperation() {
        return currentOperation;
    }

    final OperationInfo getNextOperation() {
        return nextOperation;
    }

    final String getProgramCounter() {
        return Utilities.toHex(programCounter & 0x0FFF);
    }

    final String getRegisterAt(int registerIndex) {
        if (registerIndex < 0 || registerIndex > 15) {
            throw new IllegalStateException("Unknown register: V" + registerIndex);
        }
        byte state = registerStates[registerIndex];
        return Utilities.toHex(state & 0x000000FF);
    }
}
