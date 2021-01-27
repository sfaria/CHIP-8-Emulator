package chip8;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class Utilities {

    // -------------------- Statics --------------------

    static String toHex(int v) {
        return String.format("0x%04X", v & 0xFFFF);
    }

    static boolean isEqual(byte x, byte y) {
        return (x ^ y) == 0;
    }

    static boolean isEqual(short x, short y) {
        return (x ^ y) == 0;
    }

    static boolean isEqual(int x, int y) {
        return (x ^ y) == 0;
    }

    // -------------------- Constructors --------------------

    private Utilities() {}
}
