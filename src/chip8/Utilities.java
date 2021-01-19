package chip8;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class Utilities {

    // -------------------- Statics --------------------

    static String toHex(int v) {
        return String.format("0x%04X", v & 0xFFFF);
    }

    // -------------------- Constructors --------------------

    private Utilities() {}
}
