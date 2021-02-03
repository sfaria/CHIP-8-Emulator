package chip8;

import chip8.hardware.ColorPalette;
import chip8.hardware.Palettes;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public final class Props {

    // -------------------- Private Statics --------------------

    private static final Path PROPS_PATH = Path.of(System.getenv("LOCALAPPDATA"))
            .resolve("chip8")
            .resolve("c8.properties");

    private static final Properties PROPS;
    static {
        PROPS = new Properties();
        File file = PROPS_PATH.toFile().getAbsoluteFile();
        file.getParentFile().mkdirs();
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                PROPS.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Config file '%s' not found.".formatted(file.getPath()), e);
            }
        }
    }

    private static final String ROM_DIR_KEY = "lastOpenedROMDirectory";
    private static final String SELECTED_PALETTE_KEY = "selectedColorPaletteId";
    private static final String CURRENT_VOLUME_KEY = "currentVolume";
    private static final String CPU_CLOCK_SPEED_KEY = "cpuSpeedInHz";

    // -------------------- Public Statics --------------------

    public static File getSavedROMLocation() {
        String value = PROPS.getProperty(ROM_DIR_KEY, null);
        if (value == null || value.isBlank()) {
            return null;
        }
        return new File(value).getAbsoluteFile();
    }

    public static void setSavedROMLocation(File dir) {
        if (dir == null || !dir.exists()) {
            PROPS.setProperty(ROM_DIR_KEY, "");
        } else {
            PROPS.setProperty(ROM_DIR_KEY, dir.getAbsolutePath());
        }
    }

    public static ColorPalette getSavedPalette() {
        List<ColorPalette> allPalettes = Palettes.ALL_PALETTES;
        String value = PROPS.getProperty(SELECTED_PALETTE_KEY, "");
        return allPalettes.stream()
                .filter(p -> p.id().equals(value))
                .findFirst()
                .orElseGet(() -> allPalettes.get(0));
    }

    public static void setSelectedPalette(ColorPalette palette) {
        if (palette != null) {
            PROPS.setProperty(SELECTED_PALETTE_KEY, palette.id());
        }
    }

    public static double getSavedVolume() {
        String stringValue = PROPS.getProperty(CURRENT_VOLUME_KEY, "0.5");
        double val = 0.5d;
        try {
            val = Double.parseDouble(stringValue);
        } catch (NumberFormatException ignored) {}
        return val;
    }

    public static void setSavedVolume(double volume) {
        PROPS.setProperty(CURRENT_VOLUME_KEY, String.valueOf(volume));
    }

    public static int getSavedCPUClockSpeed() {
        String stringValue = PROPS.getProperty(CPU_CLOCK_SPEED_KEY, "500");
        int val = 500;
        try {
            val = Integer.parseInt(stringValue);
        } catch (NumberFormatException ignored) {}
        return val;
    }

    public static void setSavedCPUSpeed(int cpuSpeedHz) {
        PROPS.setProperty(CPU_CLOCK_SPEED_KEY, String.valueOf(cpuSpeedHz));
    }

    // -------------------- Default Static Methods --------------------

    static void writeProperties() {
        File file = PROPS_PATH.toFile().getAbsoluteFile();
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            PROPS.store(out, "JCHIP8 v1");
        } catch (IOException e) {
            throw new RuntimeException("Config file '%s' failed to save to disk.".formatted(file.getPath()), e);
        }
    }

    // -------------------- Constructors --------------------

    private Props() {}
}
