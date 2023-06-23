package chip8.hardware;

import java.util.EventListener;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public interface RenderListener extends EventListener {
    void render(boolean[][] graphicsMemory);
}
