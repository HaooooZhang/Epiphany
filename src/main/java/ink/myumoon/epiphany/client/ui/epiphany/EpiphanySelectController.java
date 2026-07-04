package ink.myumoon.epiphany.client.ui.epiphany;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.PathData;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Phase B controller for the Epiphany selection popup.
 * <p>
 * Populates {@code #epiphany-paths-container} with one column per Path (sorted
 * by weight asc), + one transparent "Other" column for pathless epiphanies.
 * Each column shows up to 4 epiphanies (zig-zag), rendered as icon sprites.
 * Bottom strip replicates the player's selected epiphanies horizontally.
 * Switch toggles visibility of locked epiphanies.
 */
public final class EpiphanySelectController {

    private static final String EPIPHANY_POPUP = "#epiphany-popup";
    private static final String PATHS_SELECTOR = "#epiphany-paths-container";
    private static final String RIGHT_SELECTOR = "#epiphany-right-col";
    private static final String ERROR_SELECTOR = "#epiphany-popup-error";

    /** Filter state. */
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
            int labelW = labelText.length() * 8;
            int switchW = 24;
            int gap = 6;
            int totalW = labelW + gap + switchW;
            row.layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                    .right(4).top(0).width(totalW).height(14)
                    .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW));
            Label lbl = new Label();
            lbl.setText(Component.literal(labelText));
            lbl.textStyle(t -> t.fontSize(9).textColor(0xFFFFFFFF).textShadow(true)
                    .textAlignVertical(com.lowdragmc.lowdraglib2.gui.ui.data.Vertical.CENTER));
            lbl.layout(l -> l.width(labelW).height(14).flexShrink(0));
            row.addChild(lbl);
            var sw = new com.lowdragmc.lowdraglib2.gui.ui.elements.Switch();
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
            boolean unlocked = state != null ? state.unlocked()
                    : e.initialState() == InitialState.SELECTABLE && e.condition().isEmpty();
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
                        net.minecraft.resources.ResourceKey.create(ink.myumoon.epiphany.registry.EpiphanyRegistries.PATH_REGISTRY_KEY, entry.getKey()))
                        .map(h -> h.value()).orElse(null) : null;
                label = (pd != null && pd.name().isPresent()) ? pd.name().get().getString() : entry.getKey().toString();
            } else {
                label = "全部感悟";
            }
            UIElement titleBar = new UIElement();
            titleBar.addClass("epiphany-path-title");
            Label lbl = new Label();
            lbl.setText(Component.literal(label));
            lbl.textStyle(t -> t.fontSize(9).textColor(0xFFFFFFFF).textShadow(true)
                    .textAlignVertical(com.lowdragmc.lowdraglib2.gui.ui.data.Vertical.CENTER));
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
                addSlotIcon(slot, lookup, selected.get(i));
            } else if (i - selected.size() < freeSlots) {
                slot.addClass("epiphany-slot-empty");
            } else {
                slot.addClass("epiphany-slot-disabled");
            }
            right.addChild(slot);
        }
    }

    private static void addSlotIcon(UIElement slot,
            net.minecraft.core.HolderLookup.RegistryLookup<EpiphanyData> lookup, ResourceLocation id) {
        var key = net.minecraft.resources.ResourceKey.create(
                ink.myumoon.epiphany.registry.EpiphanyRegistries.EPIPHANY_REGISTRY_KEY, id);
        var ed = lookup.get(key).map(h -> h.value()).orElse(null);
        if (ed != null) {
            var iconOpt = ink.myumoon.epiphany.client.EpiphanyIcons.iconTexture(ed, id);
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
    }

    /** Build one popup card — no zigzag, same size as main UI slot. */
    private static UIElement buildCard(UI ui, ResourceLocation id, EpiphanyData data, boolean unlocked) {
        UIElement card = new UIElement();
        card.addClass("epiphany-popup-card");
        card.addClass(unlocked ? "epiphany-slot-empty" : "epiphany-slot-disabled");
        addSlotIcon(card, ClientData.epiphanyLookup(), id);
        if (unlocked) {
            card.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                com.lowdragmc.lowdraglib2.gui.util.UISoundUtils.playButtonClickSound();
                com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor.rpcToServer(
                        "epiphany.select_epiphany", id.toString());
                Overlay.hide(ui, EPIPHANY_POPUP);
            });
        }
        card.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            String name = data.name().isPresent() ? data.name().get().getString() : id.toString();
            lines.add(net.minecraft.network.chat.Component.literal(name).withStyle(net.minecraft.ChatFormatting.WHITE));
            if (data.description().isPresent()) lines.add(data.description().get().copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown() && data.rewardDescription().isPresent())
                lines.add(net.minecraft.network.chat.Component.translatable("epiphany.tooltip.reward").append(": ").append(data.rewardDescription().get()).withStyle(net.minecraft.ChatFormatting.GOLD));
            else if (data.rewardDescription().isPresent())
                lines.add(net.minecraft.network.chat.Component.translatable("epiphany.ui.shift_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
            e.hoverTooltips = com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips.empty();
            for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
        });
        return card;
    }

    private static int pathWeightVal(HolderLookup.RegistryLookup<PathData> lookup, ResourceLocation key) {
        if (lookup == null) return 100;
        var pk = net.minecraft.resources.ResourceKey.create(ink.myumoon.epiphany.registry.EpiphanyRegistries.PATH_REGISTRY_KEY, key);
        return lookup.get(pk).map(h -> h.value().weight()).orElse(100);
    }

    private static boolean resourceExists(ResourceLocation rl) {
        try { return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(rl).isPresent(); }
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
