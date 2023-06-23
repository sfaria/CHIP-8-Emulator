package chip8.ui;

import javax.swing.*;
import java.util.function.Function;

/**
 * @author Scott Faria <scott.faria@protonmail.com>
 */
final class DynamicLabel<T> extends JLabel {

    // -------------------- Constructors --------------------

    DynamicLabel(JComponent parent, String property, String defaultText, Function<T, String> valueRenderer) {
        super(defaultText);
        parent.addPropertyChangeListener(property, evt -> {
            @SuppressWarnings("unchecked")
            T state = (T) evt.getNewValue();
            if (state != null) {
                setText(valueRenderer.apply(state));
            } else {
                setText(defaultText);
            }
        });
    }
}
