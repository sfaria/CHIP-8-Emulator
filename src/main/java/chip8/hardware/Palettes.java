package chip8.hardware;

import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class Palettes {

    // -------------------- Private Statics --------------------

    private static final Color NOT_SO_BLACK = new Color(40, 40, 40);

    // -------------------- Statics --------------------

    public static final List<ColorPalette> ALL_PALETTES;

    static {
        ALL_PALETTES = List.of(
            new ColorPalette() {
                @Override public String id() {
                    return "1_bw";
                }
                @Override public String displayName() {
                    return "Black & White";
                }
                @Override public Color onPixel() {
                    return Color.WHITE;
                }
                @Override public Color offPixel() {
                    return Color.BLACK;
                }
                @Override public int hashCode() {
                    return Objects.hashCode(id());
                }
                @Override public boolean equals(Object obj) {
                    if (!(obj instanceof ColorPalette)) {
                        return false;
                    }
                    if (obj == this) {
                        return true;
                    }
                    return id().equals(((ColorPalette) obj).id());
                }
            },
            new ColorPalette() {
                @Override public String id() {
                    return "2_amber";
                }
                @Override public String displayName() {
                    return "Amber";
                }
                @Override public Color onPixel() {
                    return new Color(255, 176, 0);
                }
                @Override public Color offPixel() {
                    return NOT_SO_BLACK;
                }
                @Override public int hashCode() {
                    return Objects.hashCode(id());
                }
                @Override public boolean equals(Object obj) {
                    if (!(obj instanceof ColorPalette)) {
                        return false;
                    }
                    if (obj == this) {
                        return true;
                    }
                    return id().equals(((ColorPalette) obj).id());
                }
            },
            new ColorPalette() {
                @Override public String id() {
                    return "3_amber_2";
                }
                @Override public String displayName() {
                    return "Light Amber";
                }
                @Override public Color onPixel() {
                    return new Color(255, 204, 0);
                }
                @Override public Color offPixel() {
                    return NOT_SO_BLACK;
                }
                @Override public int hashCode() {
                    return Objects.hashCode(id());
                }
                @Override public boolean equals(Object obj) {
                    if (!(obj instanceof ColorPalette)) {
                        return false;
                    }
                    if (obj == this) {
                        return true;
                    }
                    return id().equals(((ColorPalette) obj).id());
                }
            },
            new ColorPalette() {
                @Override public String id() {
                    return "4_green_a2";
                }
                @Override public String displayName() {
                    return "Apple ][";
                }
                @Override public Color onPixel() {
                    return new Color(51, 255, 51);
                }
                @Override public Color offPixel() {
                    return NOT_SO_BLACK;
                }
                @Override public int hashCode() {
                    return Objects.hashCode(id());
                }
                @Override public boolean equals(Object obj) {
                    if (!(obj instanceof ColorPalette)) {
                        return false;
                    }
                    if (obj == this) {
                        return true;
                    }
                    return id().equals(((ColorPalette) obj).id());
                }
            }
        );
    }

    // -------------------- Constructors --------------------

    private Palettes() {}
}
