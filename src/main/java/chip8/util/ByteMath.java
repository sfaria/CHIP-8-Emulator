package chip8.util;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class ByteMath {

    // -------------------- Public Statics --------------------

    public static boolean equal(byte x, byte y) {
        return (((short) x) & 0x00FF) == (((short) y) & 0x00FF);
    }

    public static byte add(byte x, byte y) {
        return (byte) ((((short) x) & 0x00FF) + (((short) y)) & 0x00FF);
    }

    public static byte subtract(byte x, byte y) {
        return (byte) ((((short) x) & 0x00FF) - (((short) y)) & 0x00FF);
    }

    public static short addWithOverflow(byte x, byte y) {
        return (short) ((((short) x) & 0x00FF) + (((short) y) & 0x00FF));
    }

    public static boolean gt(byte x, byte y) {
        return ((((short) x) & 0x00FF)  > (((short) y) & 0x00FF));
    }

    // -------------------- Constructors --------------------

    private ByteMath() {}
}
