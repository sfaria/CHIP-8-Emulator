package chip8;


import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 *
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class Display extends JComponent {

    // -------------------- Private Variables --------------------

    private final int width = 64;
    private final int height = 32;
    private final int scaleFactor = 10;
    private final boolean[] memory = new boolean[width * height];

    // -------------------- Constructors --------------------

    Display(CPU cpu) {
        Arrays.fill(memory, false);
        cpu.addRenderListener(graphicsMemory -> {
            System.arraycopy(graphicsMemory, 0, memory, 0, graphicsMemory.length);
            try {
                SwingUtilities.invokeAndWait(this::repaint);
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException("Failed to render.", e);
            }
        });
        setPreferredSize(new Dimension(width * scaleFactor, height * scaleFactor));
        setMinimumSize(new Dimension(width * scaleFactor, height * scaleFactor));
    }

    // -------------------- Overridden Methods --------------------

    @Override
    protected final void paintComponent(java.awt.Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Rectangle pixel = new Rectangle(0, 0, getWidth() / width, getHeight() / height);

        int currentPixel = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = memory[currentPixel++] ? Color.WHITE : Color.BLACK;
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
