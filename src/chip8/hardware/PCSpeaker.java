package chip8.hardware;

import chip8.Props;
import chip8.util.Utilities;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
public final class PCSpeaker implements AutoCloseable {

    // -------------------- Private Statics --------------------

    private static final float SAMPLE_RATE = 8000f;
    private static final int HZ = 226;
    private static final int MSEC_PERIOD = 500;

    // -------------------- Private Variables --------------------

    private final Executor ex = Executors.newSingleThreadExecutor();
    private final AtomicBoolean beeping = new AtomicBoolean(false);
    private final AtomicReference<Double> volume;
    private final SourceDataLine line;

    // -------------------- Constructors --------------------

    public PCSpeaker() throws LineUnavailableException {
        this.volume = new AtomicReference<>(Props.getSavedVolume());
        AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
        this.line = AudioSystem.getSourceDataLine(audioFormat);
        line.open(audioFormat);
    }

    // -------------------- Overridden Methods --------------------

    @Override
    public final void close() {
        line.close();
    }

    // -------------------- Public Methods --------------------

    public final void setVolume(double volume) {
        volume = Math.min(Math.max(0, volume), 1.0d);
        this.volume.set(volume);
    }

    public final void startBeepIfNotStarted() {
        if (beeping.compareAndSet(false, true)) {
            Utilities.invokeInBackground(() -> {
                ex.execute(() -> {
                    double vol = volume.get();
                    byte[] buff = fillBuffer(vol);
                    line.start();
                    while (beeping.get()) {
                        line.write(buff, 0, buff.length);
                        double _vol = volume.get();
                        if (_vol != vol) {
                            vol = _vol;
                            buff = fillBuffer(vol);
                        }
                    }
                });
            });
        }
    }

    public final void endBeep() {
        if (beeping.compareAndSet(true, false)) {
            line.stop();
            line.flush();
        }
    }

    // -------------------- Private Statics --------------------

    private static byte[] fillBuffer(double vol) {
        byte[] buf = new byte[(int) SAMPLE_RATE * MSEC_PERIOD / 1000];
        for (int i = 0; i < buf.length; i++) {
            double angle = i / (SAMPLE_RATE / HZ) * 2.0 * Math.PI;
            buf[i] = (byte) (Math.sin(angle) * 127.0 * vol);
        }
        return buf;
    }


}
