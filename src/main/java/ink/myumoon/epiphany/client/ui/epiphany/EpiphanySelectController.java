package ink.myumoon.epiphany.client.ui.epiphany;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.client.EpiphanyIcons;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.ItemIconElement;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.PathData;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class EpiphanySelectController {

    private static final String EPIPHANY_POPUP = "#epiphany-popup";
    private static final String PATHS_SELECTOR = "#epiphany-paths-container";
    private static final String RIGHT_SELECTOR = "#epiphany-right-col";
    private static final String ERROR_SELECTOR = "#epiphany-popup-error";

    private static boolean showLocked = false;

    private EpiphanySelectController() {
    }

    public static void attach(UI ui) {
        showLocked = false;
        Overlay.attachCloseHandlers(ui, EPIPHANY_POPUP);
        clearError(ui);

        var popup = ui.select(EPIPHANY_POPUP).findFirst();
        var lastSig = new String[]{""};
        var tickCount = new int[]{0};
        var wasDisplayed = new boolean[]{false};
        popup.ifPresent(overlay -> overlay.addEventListener(UIEvents.TICK, e -> {
            boolean now = overlay.isDisplayed();
            if (!now) { wasDisplayed[0] = false; showLocked = false; return; }
            if (!wasDisplayed[0]) { wasDisplayed[0] = true; lastSig[0] = ""; tickCount[0] = 99; }
            tickCount[0]++;
            if (tickCount[0] < 1) return;
            tickCount[0] = 0;
            String sig = currentSignature();
            if (!sig.equals(lastSig[0])) { lastSig[0] = sig; refresh(ui); }
        }));

        ui.select("#epiphany-toggle-row").findFirst().ifPresent(row -> {
            String labelText = Component.translatable("epiphany.ui.epiphany.show_locked").getString();
            int labelW = Minecraft.getInstance().font.width(labelText);
            int switchW = 24;
            int gap = 0;
            int totalW = labelW + gap + switchW;
            row.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                    .right(4).top(0).width(totalW).height(14)
                    .flexDirection(FlexDirection.ROW));
            Label lbl = new Label();
            lbl.setText(Component.literal(labelText));
            lbl.textStyle(t -> t.fontSize(9).textColor(0xFFFFFFFF).textShadow(true)
                    .textAlignVertical(Vertical.CENTER));
            lbl.layout(l -> l.width(labelW).height(14).flexShrink(0));
            row.addChild(lbl);
            var sw = new Switch();
            sw.layout(l -> l.width(switchW).height(14).flexShrink(0).marginLeft(gap));
            sw.setOnSwitchChanged(on -> { showLocked = on; refresh(ui); });
            row.addChild(sw);
        });

        refresh(ui);
    }

    private static void refresh(UI ui) {
        clearError(ui);
        var lookup = ClientData.epiphanyLookup();
        var pathLookup = ClientData.pathLookup();
        var data = ClientData.clientData();
        if (lookup == null || data == null) return;

        // Collect candidates grouped by path.
        ResourceLocation OTHER = ResourceLocation.fromNamespaceAndPath("epiphany", "other");
        Map<ResourceLocation, List<Candidate>> grouped = new TreeMap<>(
                Comparator.comparingInt((ResourceLocation p) -> p.equals(OTHER) ? Integer.MAX_VALUE
                        : pathWeightVal(pathLookup, p)).thenComparing(ResourceLocation::toString));
        grouped.put(OTHER, new ArrayList<>());

        lookup.listElements().forEach(holder -> {
            ResourceLocation id = holder.key().location();
            EpiphanyData e = holder.value();
            var state = data.epiphanies().get(id);
            if (state != null && state.selected()) return;
            boolean unlocked = state != null ? state.unlocked() : e.initialState() == InitialState.SELECTABLE;
            if (!unlocked && !showLocked) return;
            ResourceLocation pathKey = e.path().orElse(OTHER);
            grouped.computeIfAbsent(pathKey, k -> new ArrayList<>()).add(new Candidate(id, e, unlocked));
        });

        // Left: path rows.
        UIElement left = selectOne(ui, PATHS_SELECTOR, UIElement.class);
        left.clearAllChildren();
        for (var entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            UIElement row = new UIElement();
            row.addClass("epiphany-path-row");
            String label;
            if (!entry.getKey().equals(OTHER)) {
                PathData pd = pathLookup != null ? pathLookup.get(
                        ResourceKey.create(EpiphanyRegistries.PATH_REGISTRY_KEY, entry.getKey()))
                        .map(Holder.Reference::value).orElse(null) : null;
                label = (pd != null && pd.name().isPresent()) ? pd.name().get().getString() : entry.getKey().toString();
            } else {
                label = Component.translatable("epiphany.ui.epiphany.all_epiphany").getString();
            }
            UIElement titleBar = new UIElement();
            titleBar.addClass("epiphany-path-title");
            Label lbl = new Label();
            lbl.setText(Component.literal(label));
            lbl.textStyle(t -> t.fontSize(9).textColor(0xFFFFFFFF).textShadow(true)
                    .textAlignVertical(Vertical.CENTER));
            lbl.layout(l -> l.height(14));
            titleBar.addChild(lbl);
            row.addChild(titleBar);
            UIElement cardsRow = new UIElement();
            cardsRow.addClass("epiphany-path-cards");
            for (Candidate c : entry.getValue()) {
                cardsRow.addChild(buildCard(ui, c.id, c.data, c.unlocked));
            }
            row.addChild(cardsRow);
            left.addChild(row);
        }
        if (left.getChildren().isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("epiphany.ui.epiphany.none"));
            left.addChild(none);
        }

        // Right: vertical slot column (same layout as main UI).
        UIElement right = selectOne(ui, RIGHT_SELECTOR, UIElement.class);
        right.clearAllChildren();
        int maxSlots = Config.MAX_EPIPHANY_SLOTS.get();
        int freeSlots = Math.max(0, data.epiphanySlots() - data.usedEpiphanySlots());
        List<ResourceLocation> selected = new ArrayList<>();
        lookup.listElements().forEach(holder -> {
            var st = data.epiphanies().get(holder.key().location());
            if (st != null && st.selected()) selected.add(holder.key().location());
        });
        for (int i = 0; i < maxSlots; i++) {
            boolean off = i % 2 == 1;
            UIElement slot = new UIElement();
            slot.addClass("epiphany-slot");
            slot.addClass(off ? "epiphany-slot-odd" : "epiphany-slot-even");
            if (i < selected.size()) {
                slot.addClass("epiphany-slot-selected");
                var sid = selected.get(i);
                addSlotIcon(slot, lookup, sid);
                // Tooltip
                var ed = lookup.get(ResourceKey.create(
                        EpiphanyRegistries.EPIPHANY_REGISTRY_KEY, sid))
                        .map(Holder.Reference::value).orElse(null);
                slot.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
                    var lines = new java.util.ArrayList<Component>();
                    String nm = ed != null && ed.name().isPresent() ? ed.name().get().getString() : sid.toString();
                    lines.add(Component.literal(nm).withStyle(ChatFormatting.WHITE));
                    if (ed != null && ed.description().isPresent())
                        lines.add(ed.description().get().copy().withStyle(ChatFormatting.GRAY));
                    if (Screen.hasShiftDown() && ed != null && ed.rewardDescription().isPresent())
                        lines.add(Component.translatable("epiphany.tooltip.reward").append(": ").append(ed.rewardDescription().get()).withStyle(ChatFormatting.GOLD));
                    else if (ed != null && ed.rewardDescription().isPresent())
                        lines.add(Component.translatable("epiphany.ui.shift_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    e.hoverTooltips = HoverTooltips.empty();
                    for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
                });
            } else if (i - selected.size() < freeSlots) {
                slot.addClass("epiphany-slot-empty");
            } else {
                slot.addClass("epiphany-slot-disabled");
            }
            right.addChild(slot);
        }
    }

    private static void addSlotIcon(UIElement slot,
                                    HolderLookup.RegistryLookup<EpiphanyData> lookup, ResourceLocation id) {
        var key = ResourceKey.create(
                EpiphanyRegistries.EPIPHANY_REGISTRY_KEY, id);
        var ed = lookup.get(key).map(Holder.Reference::value).orElse(null);
        if (ed != null) {
            var iconOpt = EpiphanyIcons.iconTexture(ed, id);
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
    }

    // pop-up card
    private static UIElement buildCard(UI ui, ResourceLocation id, EpiphanyData data, boolean unlocked) {
        UIElement card = new UIElement();
        card.addClass("epiphany-popup-card");
        card.addClass(unlocked ? "epiphany-slot-empty" : "epiphany-slot-disabled");
        addSlotIcon(card, Objects.requireNonNull(ClientData.epiphanyLookup()), id);
        if (unlocked) {
            card.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                UISoundUtils.playButtonClickSound();
                RPCPacketDistributor.rpcToServer(
                        "epiphany.select_epiphany", id.toString());
                Overlay.hide(ui, EPIPHANY_POPUP);
            });
        }
        card.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            var lines = new java.util.ArrayList<Component>();
            String name = data.name().isPresent() ? data.name().get().getString() : id.toString();
            lines.add(Component.literal(name).withStyle(ChatFormatting.WHITE));
            if (data.description().isPresent()) lines.add(data.description().get().copy().withStyle(ChatFormatting.GRAY));
            if (Screen.hasShiftDown() && data.rewardDescription().isPresent())
                lines.add(Component.translatable("epiphany.tooltip.reward").append(": ").append(data.rewardDescription().get()).withStyle(ChatFormatting.GOLD));
            else if (data.rewardDescription().isPresent())
                lines.add(Component.translatable("epiphany.ui.shift_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            if (!unlocked && data.conditionDescription().isPresent())
                lines.add(data.conditionDescription().get().copy().withStyle(ChatFormatting.DARK_RED));
            e.hoverTooltips = HoverTooltips.empty();
            for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
        });
        return card;
    }

    private static int pathWeightVal(HolderLookup.RegistryLookup<PathData> lookup, ResourceLocation key) {
        if (lookup == null) return 100;
        var pk = ResourceKey.create(EpiphanyRegistries.PATH_REGISTRY_KEY, key);
        return lookup.get(pk).map(h -> h.value().weight()).orElse(100);
    }

    private static boolean resourceExists(ResourceLocation rl) {
        try { return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent(); }
        catch (Throwable ignored) { return false; }
    }

    private static void clearError(UI ui) {
        ui.select(ERROR_SELECTOR).findFirst().ifPresent(el -> el.setDisplay(false));
    }

    private static String currentSignature() {
        var data = ClientData.clientData();
        if (data == null) return "";
        var sb = new StringBuilder().append(showLocked).append('|');
        data.epiphanies().entrySet().stream()
                .filter(e -> e.getValue().selected() || e.getValue().unlocked())
                .forEach(e -> sb.append(e.getKey()).append(':').append(e.getValue().selected() ? 's' : '-').append(e.getValue().unlocked() ? 'u' : '-').append(','));
        return sb.append("used=").append(data.usedEpiphanySlots()).toString();
    }

    private record Candidate(ResourceLocation id, EpiphanyData data, boolean unlocked) {}

    private static <T extends UIElement> T selectOne(UI ui, String selector, Class<T> type) {
        return ui.select(selector, type).findFirst().orElseThrow(() ->
                new IllegalStateException("Missing: " + selector));
    }
}
