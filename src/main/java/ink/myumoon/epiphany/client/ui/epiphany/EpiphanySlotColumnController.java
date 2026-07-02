package ink.myumoon.epiphany.client.ui.epiphany;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the right-side Epiphany slot column (main UI).
 * <p>
 * Renders one selected epiphany per slot, plus empty slots up to
 * {@link Config#MAX_EPIPHANY_SLOTS}. Clicking a slot opens the Epiphany
 * selection popup via {@link Overlay#show}.
 * <p>
 * B4: refresh the column when the player's selected epiphany set changes.
 */
public final class EpiphanySlotColumnController {

    /** Popup overlay selector opened on empty-slot click. */
    private static final String EPIPHANY_POPUP = "#epiphany-popup";
    /** Re-render check interval (in ticks). */
    private static final int REFRESH_INTERVAL = 20;

    private EpiphanySlotColumnController() {
    }

    /** Attach to the UI tree. */
    public static void attach(UI ui) {
        UIElement col = selectOne(ui, "#epiphany-col", UIElement.class);
        refreshColumn(ui, col);

        // Re-render when epiphany selection changes.
        final String[] lastSig = {currentSignature()};
        final int[] tc = {0};
        col.addEventListener(UIEvents.TICK, e -> {
            tc[0]++;
            if (tc[0] < REFRESH_INTERVAL) return;
            tc[0] = 0;
            String sig = currentSignature();
            if (!sig.equals(lastSig[0])) {
                lastSig[0] = sig;
                refreshColumn(ui, col);
            }
        });
    }

    /** Rebuild the slot column. */
    private static void refreshColumn(UI ui, UIElement col) {
        col.clearAllChildren();
        int maxSlots = Config.MAX_EPIPHANY_SLOTS.get();

        // Selected epiphanies in registry order.
        List<ResourceLocation> selected = new ArrayList<>();
        var data = ClientData.clientData();
        var lookup = ClientData.epiphanyLookup();
        if (data != null && lookup != null) {
            lookup.listElements().forEach(holder -> {
                ResourceLocation id = holder.key().location();
                var state = data.epiphanies().get(id);
                if (state != null && state.selected()) selected.add(id);
            });
        }

        // Selected slot = filled first.
        for (ResourceLocation id : selected) {
            col.addChild(buildFilledSlot(ui));
        }
        // Fill up to maxSlots with empty slots (zig-zag alternating).
        // If player has no free epiphany slot, all empty slots are disabled.
        int freeSlots = (data != null) ? Math.max(0, data.epiphanySlots() - data.usedEpiphanySlots()) : 0;
        int emptyCount = Math.max(0, maxSlots - selected.size());
        for (int i = 0; i < emptyCount; i++) {
            boolean offset = (selected.size() + i) % 2 == 1;
            boolean canChoose = i < freeSlots;
            col.addChild(buildEmptySlot(ui, offset, canChoose));
        }
    }

    /** Build a filled slot for an already-selected epiphany — no click handler. */
    private static UIElement buildFilledSlot(UI ui) {
        UIElement slot = new UIElement();
        slot.addClass("epiphany-slot");
        slot.addClass("epiphany-slot-selected");
        return slot;
    }

    /**
     * Build an empty epiphany slot. ZERO layout/style — LSS owns it.
     * If {@code canChoose} is false (no free slot available),
     * use .epiphany-slot-disabled (RECT_RD_DARK) and no click handler.
     */
    private static UIElement buildEmptySlot(UI ui, boolean offset, boolean canChoose) {
        UIElement slot = new UIElement();
        slot.addClass("epiphany-slot");
        slot.addClass(canChoose ? "epiphany-slot-empty" : "epiphany-slot-disabled");
        slot.addClass(offset ? "epiphany-slot-odd" : "epiphany-slot-even");
        if (canChoose) {
            slot.addEventListener(UIEvents.MOUSE_DOWN, e -> Overlay.show(ui, EPIPHANY_POPUP));
        }
        return slot;
    }

    /** Snapshot signature for change detection. */
    private static String currentSignature() {
        var data = ClientData.clientData();
        if (data == null) return "";
        var sb = new StringBuilder();
        data.epiphanies().entrySet().stream()
                .filter(e -> e.getValue().selected())
                .forEach(e -> sb.append(e.getKey()).append(','));
        return sb.toString();
    }

    /** Convenience: select exactly one element or throw a clear error. */
    private static <T extends UIElement> T selectOne(UI ui, String selector, Class<T> type) {
        Optional<T> first = ui.select(selector)
                .findFirst()
                .filter(type::isInstance)
                .map(type::cast);
        return first.orElseThrow(() -> new IllegalStateException(
                "Epiphany main UI XML is missing selector '" + selector
                        + "' of type " + type.getSimpleName()));
    }
}
