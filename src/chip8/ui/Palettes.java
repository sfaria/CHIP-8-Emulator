package chip8.ui;

import java.awt.*;
import java.util.List;

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
                @Override public String displayName() {
                    return "Black & White";
                }
                @Override public Color onPixel() {
                    return Color.WHITE;
                }
                @Override public Color offPixel() {
                    return Color.BLACK;
                }
            },
            new ColorPalette() {
                @Override public String displayName() {
                    return "Amber";
                }
                @Override public Color onPixel() {
                    return new Color(255, 176, 0);
                }
                @Override public Color offPixel() {
                    return NOT_SO_BLACK;
                }
            },
            new ColorPalette() {
                @Override public String displayName() {
                    return "Light Amber";
                }
                @Override public Color onPixel() {
                    return new Color(255, 204, 0);
                }
                @Override public Color offPixel() {
                    return NOT_SO_BLACK;
                }
            },
            new ColorPalette() {
                @Override public String displayName() {
                    return "Apple ][";
                }
                @Override public Color onPixel() {
                    return new Color(51, 255, 51);
                }
                @Override public Color offPixel() {
                    return NOT_SO_BLACK;
                }
            }
        );
    }

    // -------------------- Constructors --------------------

    private Palettes() {}
}
