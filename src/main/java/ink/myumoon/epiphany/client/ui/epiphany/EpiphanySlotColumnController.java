package ink.myumoon.epiphany.client.ui.epiphany;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.client.EpiphanyIcons;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.ItemIconElement;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EpiphanySlotColumnController {

    private static final String EPIPHANY_POPUP = "#epiphany-popup";
    private static final int REFRESH_INTERVAL = 1;

    private EpiphanySlotColumnController() {
    }

    // attach
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

    // Rebuild the slot column.
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

        // Render slots maxSlots-1, zigzag alternating by global index.
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

    // icon render
    private static UIElement buildFilledSlot(
            HolderLookup.RegistryLookup<EpiphanyData> lookup,
            ResourceLocation id, boolean offset) {
        UIElement slot = new UIElement();
        slot.addClass("epiphany-slot");
        slot.addClass("epiphany-slot-selected");
        slot.addClass(offset ? "epiphany-slot-odd" : "epiphany-slot-even");
        // Icon
        var key = ResourceKey.create(
                EpiphanyRegistries.EPIPHANY_REGISTRY_KEY, id);
        var epiphanyData = lookup != null ? lookup.get(key).map(Holder.Reference::value).orElse(null) : null;
        if (epiphanyData != null) {
            var iconOpt = EpiphanyIcons.iconTexture(epiphanyData, id);
            if (iconOpt.isPresent() && resourceExists(iconOpt.get())) {
                slot.style(s -> s.background(
                        SpriteTexture.of(iconOpt.get())));
            } else {
                var icon = new ItemIconElement(
                        EpiphanyIcons.defaultEpiphany());
                icon.layout(l -> l.width(16).height(16));
                slot.addChild(icon);
            }
        }
        // Tooltip
        slot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            var lines = new ArrayList<Component>();
            String name = epiphanyData != null && epiphanyData.name().isPresent()
                    ? epiphanyData.name().get().getString() : id.toString();
            lines.add(Component.literal(name)
                    .withStyle(ChatFormatting.WHITE));
            if (epiphanyData != null && epiphanyData.description().isPresent()) {
                lines.add(epiphanyData.description().get().copy()
                        .withStyle(ChatFormatting.GRAY));
            }
            if (Screen.hasShiftDown()) {
                if (epiphanyData != null && epiphanyData.rewardDescription().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.reward")
                            .append(": ").append(epiphanyData.rewardDescription().get())
                            .withStyle(ChatFormatting.GOLD));
                }
            } else {
                if (epiphanyData != null && epiphanyData.rewardDescription().isPresent()) {
                    lines.add(Component.translatable("epiphany.ui.shift_hint")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
            e.hoverTooltips = HoverTooltips.empty();
            for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
        });
        return slot;
    }

    private static boolean resourceExists(ResourceLocation rl) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent();
        } catch (Throwable ignored) { return false; }
    }

    // build empty slot
    private static UIElement buildEmptySlot(UI ui, boolean offset, boolean canChoose) {
        UIElement slot = new UIElement();
        slot.addClass("epiphany-slot");
        slot.addClass(canChoose ? "epiphany-slot-empty" : "epiphany-slot-disabled");
        slot.addClass(offset ? "epiphany-slot-odd" : "epiphany-slot-even");
        if (canChoose) {
            slot.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                UISoundUtils.playButtonClickSound();
                Overlay.show(ui, EPIPHANY_POPUP);
            });
        }
        // Tooltip.
        slot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            var key = canChoose ? "epiphany.ui.epiphany.empty_slot" : "epiphany.ui.epiphany.no_slots";
            e.hoverTooltips = HoverTooltips.empty()
                    .append(Component.translatable(key)
                            .withStyle(ChatFormatting.DARK_GRAY));
        });
        return slot;
    }

    // Snapshot signature: selected epiphanies + slot counts (for change detection).
    private static String currentSignature() {
        var data = ClientData.clientData();
        if (data == null) return "";
        var sb = new StringBuilder();
        sb.append("s=").append(data.epiphanySlots()).append("u=").append(data.usedEpiphanySlots()).append('|');
        data.epiphanies().entrySet().stream()
                .filter(e -> e.getValue().selected())
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append(','));
        return sb.toString();
    }

    // select exactly one element or throw a clear error.
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
