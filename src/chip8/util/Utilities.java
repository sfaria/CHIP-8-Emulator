package chip8.util;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class Utilities {

    // -------------------- Private Statics --------------------

    private static final Executor BG_EX = Executors.newCachedThreadPool();
    private static final Path APP_DATA_PATH = Path.of(System.getenv("LOCALAPPDATA")).resolve("chip8");

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

    public static void withSavedROMLocation(Consumer<File> c) {
        invokeInBackground(() -> {
            File config = APP_DATA_PATH
                    .resolve("last_opened")
                    .toAbsolutePath()
                    .toFile();
            File dir = null;
            if (!config.exists()) {
                //noinspection ResultOfMethodCallIgnored
                config.getParentFile().mkdirs();
            } else {
                dir = readDir(config);
                if (dir == null || !dir.exists()) {
                   dir = null;
                }
            }

            final File _dir = dir;
            SwingUtilities.invokeLater(() -> c.accept(_dir));
        });
    }

    public static void writeSavedROMLocation(File dir) {
        invokeInBackground(() -> {
            File config = APP_DATA_PATH
                    .resolve("last_opened")
                    .toAbsolutePath()
                    .toFile();

            if (!config.exists()) {
                //noinspection ResultOfMethodCallIgnored
                config.getParentFile().mkdirs();
            }

            try {
                Files.writeString(config.toPath(), dir.getAbsolutePath());
            } catch (IOException ignore) {}
        });
    }


    // -------------------- Private Statics --------------------

    private static File readDir(File config) {
        try {
            return Files.readAllLines(config.toPath()).stream()
                    .findFirst()
                    .map(File::new)
                    .orElse(null);
        } catch (IOException ignore) {
            return null;
        }
    }

    // -------------------- Constructors --------------------

    private Utilities() {}
}
