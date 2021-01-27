package chip8;


import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class Display extends Canvas {

    // -------------------- Private Variables --------------------

    private final int width = 64;
    private final int height = 32;
    private final int scaleFactor = 10;

    private boolean[][] memory = new boolean[height][width];

    // -------------------- Constructors --------------------

    Display(CPU cpu) {
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
    }

    // -------------------- Default Methods --------------------

    final void startRendering() {
        createBufferStrategy(2);
        Timer timer = new Timer("render-timer", false);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> render());

            }
        }, 0, 16);
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
                    Color color = pixelState ? Color.WHITE : Color.BLACK;
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
            Toolkit.getDefaultToolkit().sync();
        } finally {
            g2d.dispose();
        }
    }

}
