package chip8.ui;

import chip8.util.Utilities;

import java.util.Objects;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class OperationInfo {

    // -------------------- Private Variables --------------------

    private final int opcode;

    // -------------------- Constructors --------------------

    public OperationInfo(int opcode) {
        this.opcode = opcode;
    }

    // -------------------- Default Methods --------------------

    public final String asHexString() {
        return Utilities.toHex(opcode);
    }

    // -------------------- Overridden Methods --------------------

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationInfo that = (OperationInfo) o;
        return opcode == that.opcode;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(opcode);
    }
}
