package chip8.ui;

import java.awt.*;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public interface ColorPalette {
    String displayName();
    Color onPixel();
    Color offPixel();
}
