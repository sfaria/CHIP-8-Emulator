package chip8;


import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class Graphics extends JComponent {

    private final int width = 64;
    private final int height = 32;
    private final int scaleFactor = 8;
    private final CPU cpu;

    Graphics(CPU cpu) {
        this.cpu = cpu;
    }

    @Override
    public final int getWidth() {
        return width * scaleFactor;
    }

    @Override
    public final int getHeight() {
        return height * scaleFactor;
    }

    @Override
    protected final void paintComponent(java.awt.Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        boolean[] graphicsMemory = cpu.getGraphics();
        Rectangle pixel = new Rectangle(0, 0, getWidth() / width, getHeight() / height);

        int currentPixel = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = graphicsMemory[currentPixel++] ? Color.WHITE : Color.BLACK;
                Color _color = g2d.getColor();
                try {
                    g2d.setColor(color);
                    g2d.draw(pixel);
                    g2d.fill(pixel);
                    pixel.setLocation(pixel.x + scaleFactor, pixel.y);
                } finally {
                    g2d.setColor(_color);
                }
            }
            pixel.setLocation(0, pixel.y + scaleFactor);
        }
    }
}
