package chip8

import org.fusesource.jansi.Ansi

import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.LineBorder
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle

/**
 *
 * @author Scott Faria <scott.faria@gmail.com>
 */
class Graphics extends JComponent {

    def graphicsProvider
    def _width = 64
    def _height = 32

    def scaleFactor = 8

    @Override
    def int getWidth() {
        return _width * scaleFactor
    }

    @Override
    def int getHeight() {
        return _height * scaleFactor
    }

    @Override
    def void paintComponent(java.awt.Graphics g) {
        def g2d = (Graphics2D) g
        def graphicsMemory = graphicsProvider.getGraphics()
        def pixel = new Rectangle(0, 0, (int) (getWidth() / _width), (int) (getHeight() / _height))

        def currentPixel = 0
        _height.times {
            _width.times {
                def color = graphicsMemory[currentPixel++] ? Color.WHITE : Color.BLACK
                def _color = g2d.getColor()
                g2d.setColor(color)
                g2d.draw(pixel)
                g2d.fill(pixel)
                g2d.setColor(_color)
                pixel.setLocation((int) (pixel.x + scaleFactor), (int) pixel.y)
            }
            pixel.setLocation(0, (int) (pixel.y + scaleFactor))
        }
    }
}
