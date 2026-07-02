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
    private static final int REFRESH_INTERVAL = 1;

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

        int freeSlots = (data != null) ? Math.max(0, data.epiphanySlots() - data.usedEpiphanySlots()) : 0;

        // Render slots 0..maxSlots-1, zigzag alternating by global index.
        for (int idx = 0; idx < maxSlots; idx++) {
            boolean offset = idx % 2 == 1;
            if (idx < selected.size()) {
                col.addChild(buildFilledSlot(lookup, selected.get(idx), offset));
            } else {
                boolean canChoose = (idx - selected.size()) < freeSlots;
                col.addChild(buildEmptySlot(ui, offset, canChoose));
            }
        }
    }

    /** Build a filled slot with epiphany icon + zigzag offset + tooltip. */
    private static UIElement buildFilledSlot(
            net.minecraft.core.HolderLookup.RegistryLookup<ink.myumoon.epiphany.content.EpiphanyData> lookup,
            ResourceLocation id, boolean offset) {
        UIElement slot = new UIElement();
        slot.addClass("epiphany-slot");
        slot.addClass("epiphany-slot-selected");
        slot.addClass(offset ? "epiphany-slot-odd" : "epiphany-slot-even");
        // Icon — same resolution as EpiphanySelectController.
        var key = net.minecraft.resources.ResourceKey.create(
                ink.myumoon.epiphany.registry.EpiphanyRegistries.EPIPHANY_REGISTRY_KEY, id);
        var epiphanyData = lookup != null ? lookup.get(key).map(h -> h.value()).orElse(null) : null;
        if (epiphanyData != null) {
            var iconOpt = ink.myumoon.epiphany.client.EpiphanyIcons.iconTexture(epiphanyData, id);
            if (iconOpt.isPresent() && resourceExists(iconOpt.get())) {
                slot.style(s -> s.background(
                        com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture.of(iconOpt.get())));
            } else {
                var icon = new ink.myumoon.epiphany.client.ui.ItemIconElement(
                        ink.myumoon.epiphany.client.EpiphanyIcons.defaultEpiphany());
                icon.layout(l -> l.width(16).height(16));
                slot.addChild(icon);
            }
        }
        // Tooltip (text-only).
        slot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            String name = epiphanyData != null && epiphanyData.name().isPresent()
                    ? epiphanyData.name().get().getString() : id.toString();
            lines.add(net.minecraft.network.chat.Component.literal(name)
                    .withStyle(net.minecraft.ChatFormatting.WHITE));
            if (epiphanyData != null && epiphanyData.description().isPresent()) {
                lines.add(epiphanyData.description().get().copy()
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                if (epiphanyData != null && epiphanyData.rewardDescription().isPresent()) {
                    lines.add(net.minecraft.network.chat.Component.translatable("epiphany.tooltip.reward")
                            .append(": ").append(epiphanyData.rewardDescription().get())
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                }
            } else {
                if (epiphanyData != null && epiphanyData.rewardDescription().isPresent()) {
                    lines.add(net.minecraft.network.chat.Component.translatable("epiphany.ui.shift_hint")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
                }
            }
            e.hoverTooltips = com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips.empty();
            for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
        });
        return slot;
    }

    private static boolean resourceExists(ResourceLocation rl) {
        try {
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(rl).isPresent();
        } catch (Throwable ignored) { return false; }
    }

    /**
     * Build an empty epiphany slot with tooltip. ZERO layout/style — LSS owns it.
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
        // Tooltip.
        slot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            var key = canChoose ? "epiphany.ui.epiphany.empty_slot" : "epiphany.ui.epiphany.no_slots";
            e.hoverTooltips = com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips.empty()
                    .append(net.minecraft.network.chat.Component.translatable(key)
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        });
        return slot;
    }

    /** Snapshot signature: selected epiphanies + slot counts (for change detection). */
    private static String currentSignature() {
        var data = ClientData.clientData();
        if (data == null) return "";
        var sb = new StringBuilder();
        sb.append("s=").append(data.epiphanySlots()).append("u=").append(data.usedEpiphanySlots()).append('|');
        data.epiphanies().entrySet().stream()
                .filter(e -> e.getValue().selected())
                .sorted(java.util.Map.Entry.comparingByKey())
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
