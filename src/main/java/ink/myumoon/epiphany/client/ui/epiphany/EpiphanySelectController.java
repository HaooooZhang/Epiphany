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
    private static final String ERROR_SELECTOR = "#epiphany-popup-error";
    private static final String SWITCH_SELECTOR = "#epiphany-show-locked";
    private static final String BOTTOM_SELECTOR = "#epiphany-bottom-row";

    /** Filter state — show locked items when true. Mutated by the Switch callback. */
    private static boolean showLocked = false;

    private EpiphanySelectController() {
    }

    public static void attach(UI ui) {
        Overlay.attachCloseHandlers(ui, EPIPHANY_POPUP);
        clearError(ui);

        // Refresh the popup's list whenever it becomes visible or attachment changes.
        var popup = ui.select(EPIPHANY_POPUP).findFirst();
        var lastSig = new String[]{""};
        var tickCount = new int[]{0};
        popup.ifPresent(overlay -> overlay.addEventListener(UIEvents.TICK, e -> {
            if (!overlay.isDisplayed()) return;
            tickCount[0]++;
            if (tickCount[0] < 10) return;
            tickCount[0] = 0;
            String sig = currentSignature();
            if (!sig.equals(lastSig[0])) {
                lastSig[0] = sig;
                refresh(ui);
            }
        }));

        refresh(ui);

        ui.select(SWITCH_SELECTOR, com.lowdragmc.lowdraglib2.gui.ui.elements.Switch.class)
                .findFirst()
                .ifPresent(sw -> sw.setOnSwitchChanged(on -> {
                    showLocked = on;
                    refresh(ui);
                }));
    }

    private static void refresh(UI ui) {
        clearError(ui);
        UIElement container = selectOne(ui, PATHS_SELECTOR, UIElement.class);
        container.clearAllChildren();
        var lookup = ClientData.epiphanyLookup();
        var pathLookup = ClientData.pathLookup();
        var data = ClientData.clientData();
        if (lookup == null || data == null) {
            refreshBottomStrip(ui, java.util.Collections.emptyList());
            return;
        }        // Group epiphanies by path (sorted path weight asc; pathless → "其他").
        var pathLookupLocal = pathLookup;
        ResourceLocation OTHER = ResourceLocation.fromNamespaceAndPath("epiphany", "other");
        Map<ResourceLocation, List<Candidate>> grouped = new TreeMap<>(
                Comparator.comparingInt((ResourceLocation p) -> p.equals(OTHER) ? Integer.MAX_VALUE : pathWeightVal(pathLookupLocal, p))
                        .thenComparing(ResourceLocation::toString));
        grouped.put(OTHER, new ArrayList<>());

        // Collect candidates (not yet selected).
        record Cand(ResourceLocation id, EpiphanyData data, boolean unlocked, int weight) {}
        List<Cand> cands = new ArrayList<>();
        lookup.listElements().forEach(holder -> {
            ResourceLocation id = holder.key().location();
            EpiphanyData e = holder.value();
            var state = data.epiphanies().get(id);
            boolean selected = state != null && state.selected();
            if (selected) return;
            boolean unlocked = state != null
                    ? state.unlocked()
                    : e.initialState() == InitialState.SELECTABLE;
            if (!unlocked && !showLocked) return;
            cands.add(new Cand(id, e, unlocked, e.weight()));
        });
        cands.sort(Comparator.<Cand, Integer>comparing(c -> c.weight)
                .thenComparing(c -> c.id.toString()));

        for (Cand c : cands) {
            ResourceLocation pathKey = c.data.path().orElse(OTHER);
            grouped.computeIfAbsent(pathKey, k -> new ArrayList<>())
                    .add(new Candidate(c.id, c.data, c.unlocked));
        }

        // Render each group as a column. Other column gets "path-column-other" class.
        for (var entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            ResourceLocation pathRl = entry.getKey();
            boolean isOther = pathRl.equals(OTHER);
            String columnName = "";
            if (!isOther) {
                PathData pd = null;
                if (pathLookupLocal != null) {
                    var pathKey = net.minecraft.resources.ResourceKey.create(ink.myumoon.epiphany.registry.EpiphanyRegistries.PATH_REGISTRY_KEY, pathRl);
                    pd = pathLookupLocal.get(pathKey).map(h -> h.value()).orElse(null);
                }
                columnName = (pd != null && pd.name().isPresent())
                        ? pd.name().get().getString()
                        : pathRl.toString();
            }
            container.addChild(buildPathColumn(ui, columnName, entry.getValue(), isOther));
        }

        if (container.getChildren().isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("epiphany.ui.epiphany.none"));
            container.addChild(none);
        }

        // Populate the bottom strip with selected epiphanies + count.
        List<ResourceLocation> selectedIds = new ArrayList<>();
        lookup.listElements().forEach(holder -> {
            ResourceLocation id = holder.key().location();
            var state = data.epiphanies().get(id);
            if (state != null && state.selected()) selectedIds.add(id);
        });
        refreshBottomStrip(ui, selectedIds);
    }

    /** Build the bottom horizontal slot strip: shows ALL epiphany slots (filled + empty),
     *  rather than just the selected ones, to replicate the main UI display horizontally. */
    private static void refreshBottomStrip(UI ui, List<ResourceLocation> selectedIds) {
        UIElement bottom = ui.select(BOTTOM_SELECTOR, UIElement.class).findFirst().orElse(null);
        if (bottom == null) return;
        bottom.clearAllChildren();
        int maxSlots = Config.MAX_EPIPHANY_SLOTS.get();
        var data = ClientData.clientData();
        int freeSlots = (data != null) ? Math.max(0, data.epiphanySlots() - data.usedEpiphanySlots()) : 0;
        int totalSlots = Math.max(maxSlots, selectedIds.size());
        for (int i = 0; i < totalSlots; i++) {
            UIElement slot = new UIElement();
            slot.addClass("epiphany-slot");
            if (i < selectedIds.size()) {
                slot.addClass("epiphany-slot-selected");
            } else if (i - selectedIds.size() < freeSlots) {
                slot.addClass("epiphany-slot-empty");
            } else {
                slot.addClass("epiphany-slot-disabled");
            }
            bottom.addChild(slot);
        }
    }

    /** Compute a Path's weight by id (default 100 if missing). */
    private static int pathWeightVal(HolderLookup.RegistryLookup<PathData> lookup, ResourceLocation key) {
        if (lookup == null) return 100;
        var pathKey = net.minecraft.resources.ResourceKey.create(ink.myumoon.epiphany.registry.EpiphanyRegistries.PATH_REGISTRY_KEY, key);
        return lookup.get(pathKey).map(h -> h.value().weight()).orElse(100);
    }

    /** Build one column element. isOther=true → omit title + use transparent bg. */
    private static UIElement buildPathColumn(UI ui, String columnName, List<Candidate> items, boolean isOther) {
        UIElement col = new UIElement();
        col.addClass("path-column");
        if (isOther) col.addClass("path-column-other");
        if (!columnName.isEmpty()) {
            Label title = new Label();
            title.setText(Component.literal(columnName));
            title.textStyle(t -> t.fontSize(10).textColor(0xFFFFFFFF).textShadow(true));
            title.addClass("path-column-title");
            col.addChild(title);
        }

        int max4 = Math.min(4, items.size());
        for (int i = 0; i < max4; i++) {
            Candidate c = items.get(i);
            col.addChild(buildEpiphanyCard(ui, c.id, c.data, c.unlocked, i % 2 == 1));
        }
        return col;
    }

    /** Build one epiphany card inside a path column.
     *  Icon strategy:
     *  1. If the mod ships a real PNG at the convention path, use SpriteTexture.
     *  2. Otherwise, fall back to the default GOAT_HORN item icon rendered as an ItemIconElement.
     */
    private static UIElement buildEpiphanyCard(UI ui, ResourceLocation id, EpiphanyData data,
                                                boolean unlocked, boolean offset) {
        UIElement card = new UIElement();
        card.addClass("epiphany-slot");
        card.addClass(unlocked ? "epiphany-slot-empty" : "epiphany-slot-disabled");
        card.addClass(offset ? "epiphany-slot-odd" : "epiphany-slot-even");
        // Try to apply a real PNG icon. If missing, render the default item icon (GOAT_HORN).
        java.util.Optional<ResourceLocation> iconOpt = ink.myumoon.epiphany.client.EpiphanyIcons.iconTexture(data, id);
        if (iconOpt.isPresent() && resourceExists(iconOpt.get())) {
            card.style(s -> s.background(
                    com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture.of(iconOpt.get())));
        } else {
            // Fallback: GOAT_HORN item icon at native 16x16 size — don't scale up
            // to fill the 22x22 slot, because vanilla item icons look right at 16x16.
            ink.myumoon.epiphany.client.ui.ItemIconElement icon =
                    new ink.myumoon.epiphany.client.ui.ItemIconElement(
                            ink.myumoon.epiphany.client.EpiphanyIcons.defaultEpiphany());
            icon.layout(l -> l.width(16).height(16));
            card.addChild(icon);
        }
        if (unlocked) {
            card.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
                com.lowdragmc.lowdraglib2.gui.util.UISoundUtils.playButtonClickSound();
                ServerPlayer sp = (ServerPlayer) e.currentElement.getModularUI().player;
                var d = sp.getData(ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes.EPIPHANY_DATA);
                if (d.usedEpiphanySlots() >= d.epiphanySlots()) {
                    showError(ui, Component.translatable("epiphany.ui.error.no_slot"));
                    return;
                }
                if (!EpiphanyManager.isUnlocked(sp, id)) {
                    showError(ui, Component.translatable("epiphany.ui.error.not_unlocked"));
                    return;
                }
                EpiphanyManager.select(sp, id);
                Overlay.hide(ui, EPIPHANY_POPUP);
            });
        }
        return card;
    }

    /** Check if the given texture resource location exists in the client resource manager. */
    private static boolean resourceExists(ResourceLocation rl) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var rm = mc.getResourceManager();
            return rm.getResource(rl).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void showError(UI ui, Component msg) {
        ui.select(ERROR_SELECTOR).findFirst().ifPresent(el -> {
            el.setDisplay(true);
            if (el instanceof Label label) label.setText(msg);
        });
    }

    private static void clearError(UI ui) {
        ui.select(ERROR_SELECTOR).findFirst().ifPresent(el -> el.setDisplay(false));
    }

    private static String currentSignature() {
        var data = ClientData.clientData();
        if (data == null) return "";
        var sb = new StringBuilder();
        sb.append(showLocked).append('|');
        data.epiphanies().entrySet().stream()
                .filter(e -> e.getValue().selected() || e.getValue().unlocked())
                .forEach(e -> sb.append(e.getKey()).append(':')
                        .append(e.getValue().selected() ? 's' : '-')
                        .append(e.getValue().unlocked() ? 'u' : '-')
                        .append(','));
        sb.append("used=").append(data.usedEpiphanySlots());
        return sb.toString();
    }

    private record Candidate(ResourceLocation id, EpiphanyData data, boolean unlocked) {}

    private static <T extends UIElement> T selectOne(UI ui, String selector, Class<T> type) {
        Optional<T> first = ui.select(selector, type).findFirst();
        return first.orElseThrow(() -> new IllegalStateException(
                "Epiphany UI missing selector '" + selector + "' of type " + type.getSimpleName()));
    }
}
