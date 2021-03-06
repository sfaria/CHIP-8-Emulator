package chip8.hardware;

import chip8.Props;
import chip8.cpu.CPU;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class Display extends Canvas {

    // -------------------- Private Variables --------------------

    private final int width = 64;
    private final int height = 32;
    private final int scaleFactor = 10;
    private final Toolkit toolkit;

    private ColorPalette palette = Props.getSavedPalette();
    private boolean[][] memory = new boolean[height][width];

    // -------------------- Constructors --------------------

    public Display(JFrame parent, CPU cpu) {
        this.toolkit = Toolkit.getDefaultToolkit();
        cpu.addRenderListener(graphicsMemory -> {
            try {
                SwingUtilities.invokeAndWait(() -> this.memory = graphicsMemory);
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException("Failed to render.", e);
            }
        });
        setPreferredSize(new Dimension(width * scaleFactor, height * scaleFactor));
        setMinimumSize(new Dimension(width * scaleFactor, height * scaleFactor));
        setIgnoreRepaint(true);

        parent.addHierarchyListener(e -> {
            if (parent.isVisible()) {
                createBufferStrategy(2);
                Timer timer = new Timer("render-timer", false);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(() -> render());

                    }
                }, 0, 16);
            }
        });
    }

    // -------------------- Default Methods --------------------

    public final void setColorPalette(ColorPalette palette) {
        SwingUtilities.invokeLater(() -> this.palette = palette);
    }

    // -------------------- Private Methods --------------------

    private void render() {
        BufferStrategy strategy = getBufferStrategy();
        Graphics graphics = strategy.getDrawGraphics();
        Graphics2D g2d = (Graphics2D) graphics;
        try {
            Rectangle pixel = new Rectangle(0, 0, getWidth() / width, getHeight() / height);
            for (boolean[] row : memory) {
                for (boolean pixelState : row) {
                    Color color = pixelState ? palette.onPixel() : palette.offPixel();
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

            strategy.show();
            toolkit.sync();
        } finally {
            g2d.dispose();
        }
    }

}
