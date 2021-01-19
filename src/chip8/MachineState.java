package chip8;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class MachineState {

    // -------------------- Statics --------------------

    static final int REGISTER_COUNT = 16;

    // -------------------- Private Variables --------------------

    private short programCounter;
    private short[] registerStates;

    // -------------------- Constructors --------------------

    MachineState(short programCounter, short[] registerStates) {
        this.programCounter = programCounter;
        this.registerStates = registerStates;
    }

    // -------------------- Default Methods --------------------

    final short getProgramCounter() {
        return programCounter;
    }

    final short getRegisterAt(int registerIndex) {
        if (registerIndex < 0 || registerIndex > 15) {
            throw new IllegalStateException("Unknown register: V" + registerIndex);
        }
        return registerStates[registerIndex];
    }
}
