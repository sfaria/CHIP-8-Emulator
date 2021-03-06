package chip8.ui;

import chip8.hardware.ColorPalette;

import java.io.File;
import java.util.EventListener;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public interface ControlsListener extends EventListener {
    default void stopEmulator() {}
    default void shouldEndWait() {}
    default void shouldWaitChanged(boolean shouldWait) {}
    default void setVolume(double volume) {}
    default void romSelected(File romFile) {}
    default void colorPaletteChanged(ColorPalette selectedPalette) {}
    default void cpuSpeedChanged(int cpuTickHz) {}
}
