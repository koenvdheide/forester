package org.forester.archaeopteryx;

import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.forester.archaeopteryx.util.TypomaticJButton;

public final class TypomaticButtonCloseTest {

    public static void main(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final JFrame host = new JFrame();
            final TypomaticJButton button = new TypomaticJButton("Zoom");
            try {
                host.add(button);
                host.pack();
                host.setVisible(true);
                final Field timerField = TypomaticJButton.class.getDeclaredField("timer");
                timerField.setAccessible(true);
                final Timer timer = (Timer) timerField.get(button);
                button.mousePressed(new MouseEvent(button, MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1));
                if (!timer.isRunning()) {
                    throw new AssertionError("The held button must start its repeat timer");
                }
                host.dispose();
                if (timer.isRunning()) {
                    throw new AssertionError("Closing the host must stop the held button timer");
                }
                host.pack();
                host.setVisible(true);
                button.mouseEntered(new MouseEvent(button, MouseEvent.MOUSE_ENTERED,
                        System.currentTimeMillis(), 0, 2, 2, 0, false));
                if (timer.isRunning()) {
                    throw new AssertionError("Reattaching must not resume an old press");
                }
                button.mousePressed(new MouseEvent(button, MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1));
                if (!timer.isRunning()) {
                    throw new AssertionError("A new press must still start repetition");
                }
                button.mouseReleased(new MouseEvent(button, MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(), 0, 2, 2, 1, false, MouseEvent.BUTTON1));
                if (timer.isRunning()) {
                    throw new AssertionError("Release must stop repetition");
                }
            }
            catch (final ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
            finally {
                button.setEnabled(false);
                host.dispose();
            }
        });
        System.out.println("TypomaticButtonCloseTest OK");
    }
}
