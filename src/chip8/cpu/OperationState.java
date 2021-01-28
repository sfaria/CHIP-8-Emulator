package chip8.cpu;

import chip8.ui.OperationInfo;

import static chip8.util.Utilities.toHex;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class OperationState {

    // -------------------- Private Variables --------------------

    // current operation state
    private final byte highByte;
    private final byte lowByte;
    private final short currentOpcode;
    private final short nnn;
    private final byte n;
    private final byte x;
    private final byte y;
    private final byte highNibble;

    // next operation (debug purposes)
    private final short nextOpcode;


    // -------------------- Constructors --------------------

    OperationState(short programCounter, byte[] memory) {
        this.highByte = memory[programCounter];
        this.lowByte = memory[programCounter + 1];
        this.currentOpcode = (short) ((((short) highByte) << 8) | (((short) lowByte)) & 0x00FF);
        this.nnn = (short) (currentOpcode & 0x0FFF);
        this.n = (byte) (currentOpcode & 0x000F);
        this.x = (byte) (highByte & 0x0F);
        this.y = (byte) ((lowByte & 0xF0) >> 4);
        this.highNibble = (byte) ((highByte & 0xF0) >> 4);
        this.nextOpcode = (short) ((((short) memory[programCounter + 2]) << 8) | (((short) memory[programCounter + 3])) & 0x00FF);
    }

    // -------------------- Public Methods --------------------

    public final OperationInfo getCurrent() {
        return new OperationInfo(currentOpcode);
    }

    public final OperationInfo getNext() {
        return new OperationInfo(nextOpcode);
    }

    // -------------------- Default Methods --------------------

    final byte getLowByte() {
        return lowByte;
    }

    final short getCurrentOpcode() {
        return currentOpcode;
    }

    final short getNNN() {
        return nnn;
    }

    final byte getN() {
        return n;
    }

    final byte getX() {
        return x;
    }

    final byte getY() {
        return y;
    }

    final byte getHighNibble() {
        return highNibble;
    }

    // -------------------- Overridden Methods --------------------

    @Override
    public final String toString() {
        return toHex(currentOpcode) + " (" + Integer.toBinaryString(currentOpcode) + ")";
    }
}
