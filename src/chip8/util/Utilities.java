package chip8.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class Utilities {

    // -------------------- Private Statics --------------------

    private static final Executor BG_EX = Executors.newCachedThreadPool();

    // -------------------- Statics --------------------

    public static String toHex(int v) {
        return String.format("0x%04X", (v & 0x0000FFFF));
    }

    public static boolean isEqual(byte x, byte y) {
        return (x ^ y) == 0;
    }

    public static boolean isEqual(int x, int y) {
        return (x ^ y) == 0;
    }

    public static void arrayCopy(boolean[][] src, boolean[][] dest) {
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, src[i].length);
        }
    }

    public static void invokeInBackground(Runnable r) {
        BG_EX.execute(r);
    }

    // -------------------- Constructors --------------------

    private Utilities() {}
}
