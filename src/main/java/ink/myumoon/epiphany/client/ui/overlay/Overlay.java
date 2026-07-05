package ink.myumoon.epiphany.client.ui.overlay;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import java.util.Optional;

/**
 * Lightweight popup overlay infrastructure.
 * <p>
 * Popups are pre-declared in main.xml as {@code .popup-overlay} elements at the
 * end of {@code #root} (last in DOM = drawn on top, since LDLib2 has no zIndex).
 * Show/hide by toggling {@code setDisplay(true/false)} — the LSS rule
 * {@code .popup-overlay { display: none; }} keeps them hidden by default.
 * <p>
 * Click-outside-to-close: we attach a MOUSE_DOWN listener on the overlay itself;
 * if the event's target IS the overlay (not bubbled from inside the panel), we
 * close it. ESC close: KEY_DOWN with keyCode == 256 (GLFW_KEY_ESCAPE) on the
 * overlay.
 */
public final class Overlay {

    private static final int KEY_ESCAPE = 256;

    private Overlay() {
    }

    // Show the overlay identified by selector
    public static void show(UI ui, String overlaySelector) {
        findOverlay(ui, overlaySelector).ifPresent(el -> el.setDisplay(true));
    }

    // Hide the overlay identified by selector.
    public static void hide(UI ui, String overlaySelector) {
        findOverlay(ui, overlaySelector).ifPresent(el -> el.setDisplay(false));
    }

    // Attach close handlers for the given overlay.
    public static void attachCloseHandlers(UI ui, String overlaySelector) {
        Optional<UIElement> overlayOpt = findOverlay(ui, overlaySelector);
        if (overlayOpt.isEmpty()) return;
        UIElement overlay = overlayOpt.get();

        // Click on backdrop (target == overlay, not bubbled from inside panel) → close.
        overlay.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.target == overlay) {
                hide(ui, overlaySelector);
            }
        });
        // ESC key → close.
        overlay.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == KEY_ESCAPE) {
                hide(ui, overlaySelector);
            }
        });
    }

    private static Optional<UIElement> findOverlay(UI ui, String overlaySelector) {
        return ui.select(overlaySelector).findFirst();
    }
}
