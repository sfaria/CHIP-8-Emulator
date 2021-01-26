package chip8;


import javax.swing.*;
import java.awt.*;
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

    private boolean[][] memory = new boolean[height][width];

    // -------------------- Constructors --------------------

    Display(CPU cpu) {
        cpu.addRenderListener(graphicsMemory -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    this.memory = graphicsMemory;
                    repaint();
                });
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

        for (int y = 0; y < memory.length; y++) {
            boolean[] row = memory[y];
            for (int i = 0; i < row.length; i++) {
                Color color = row[i] ? Color.WHITE : Color.BLACK;
                Color _color = g2d.getColor();
                try {
                    g2d.setColor(color);
                    g2d.fill(pixel);
                    g2d.draw(pixel);
                    int newXPosition = pixel.x + scaleFactor;
                    pixel.setLocation(newXPosition, pixel.y);
                } finally {
                    g2d.setColor(_color);
                }
            }
            int newYPosition = pixel.y + scaleFactor;
            pixel.setLocation(0, newYPosition);
        }
    }
}
