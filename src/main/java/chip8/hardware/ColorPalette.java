package chip8.hardware;

import java.awt.*;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public interface ColorPalette {
    String id();
    String displayName();
    Color onPixel();
    Color offPixel();
}
