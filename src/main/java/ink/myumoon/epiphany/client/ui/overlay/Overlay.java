package ink.myumoon.epiphany.client.ui.overlay;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaPositionType;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

/**
 * Common Overlay infrastructure for Epiphany popups.
 * <p>
 * The overlay is placed as the last child of the target UIElement (after the
 * main content) and uses {@code setVisible(false)} / {@code true} to toggle
 * visibility.
 * <p>
 * <b>Click-outside-to-close</b>: a MOUSE_DOWN listener is attached to BOTH
 * the backdrop (closes the overlay) and the panel (does nothing — but its
 * presence ensures that we can detect "user clicked panel vs backdrop" by
 * comparing {@code e.currentElement} identity).
 */
public final class Overlay {

    private final UIElement wrapper;
    private final UIElement backdrop;
    private final UIElement panel;

    private Overlay(UIElement wrapper, UIElement backdrop, UIElement panel) {
        this.wrapper = wrapper;
        this.backdrop = backdrop;
        this.panel = panel;
    }

    /**
     * Build an overlay wrapping the given panel. The returned wrapper is the
     * element to add to the parent UIElement tree (typically as the last child
     * of the root, after the main content).
     */
    public static Overlay of(UIElement panel) {
        var backdrop = new UIElement();
        backdrop.layout(l -> l
                .flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER)
                .widthPercent(100).heightPercent(100));

        var wrapper = new UIElement();
        // ABSOLUTE positioning: wrapper does NOT participate in the parent's
        // flex flow. It floats over the parent's layout, covering it entirely
        // via setPosition(YogaEdge.ALL, 0) which insets all four edges to 0.
        wrapper.layout(l -> l
                .positionType(YogaPositionType.ABSOLUTE)
                .setPosition(YogaEdge.ALL, 0)
                .widthPercent(100).heightPercent(100));
        wrapper.addChild(backdrop);
        backdrop.addChild(panel);

        // Build overlay first, then attach handlers that reference it.
        Overlay overlay = new Overlay(wrapper, backdrop, panel);

        // Backdrop click: hide overlay ONLY when the user clicked on the
        // backdrop itself (i.e. the actual hit target is the backdrop, not
        // a bubbled event from inside the panel). We use {@code e.target}
        // which is the element the mouse actually hit, not the listener owner.
        // Additionally, panel adds its own MOUSE_DOWN listener that calls
        // stopPropagation() defensively in case currentElement differs.
        backdrop.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.target == backdrop) {
                overlay.hide();
            }
        });
        panel.addEventListener(UIEvents.MOUSE_DOWN, UIEvent::stopPropagation);

        wrapper.setDisplay(false);
        return overlay;
    }

    public UIElement wrapper() {
        return wrapper;
    }

    public UIElement panel() {
        return panel;
    }

    public void show() {
        // setVisible(boolean) only toggles a flag and does NOT affect rendering.
        // Use setDisplay(true) which sets TaffyDisplay.FLEX so the element is
        // actually laid out and drawn.
        wrapper.setDisplay(true);
    }

    public void hide() {
        wrapper.setDisplay(false);
    }

    /** Convenience: build a title row for a popup panel. */
    public static UIElement titleRow(Component title) {
        var row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW)
                .widthPercent(100).height(20)
                .marginBottom(4));
        row.addChild(new com.lowdragmc.lowdraglib2.gui.ui.elements.Label().setText(title));
        return row;
    }
}
