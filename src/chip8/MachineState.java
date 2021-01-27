package chip8;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class MachineState {

    // -------------------- Statics --------------------

    static final int REGISTER_COUNT = 16;

    // -------------------- Private Variables --------------------

    private final OperationInfo currentOperation;
    private final OperationInfo nextOperation;
    private final short programCounter;
    private final byte[] registerStates;

    // -------------------- Constructors --------------------

    MachineState(OperationState state, short programCounter, byte[] registerStates) {
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

    final short getProgramCounter() {
        return programCounter;
    }

    final byte getRegisterAt(int registerIndex) {
        if (registerIndex < 0 || registerIndex > 15) {
            throw new IllegalStateException("Unknown register: V" + registerIndex);
        }
        return registerStates[registerIndex];
    }
}
