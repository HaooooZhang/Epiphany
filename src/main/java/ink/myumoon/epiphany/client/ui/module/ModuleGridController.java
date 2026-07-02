package ink.myumoon.epiphany.client.ui.module;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.InsightManager;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.insight.InsightClickHandler;
import ink.myumoon.epiphany.client.ui.insight.InsightTreeView;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the main UI's Module grid.
 * <p>
 * Fills {@code #module-grid} with the player's selected Modules (one card each)
 * plus empty "+select" slot buttons until {@link Config#MAX_SELECTED_MODULES}
 * is reached. Empty slot clicks open the Module selection popup.
 * <p>
 * Card sizing comes from the {@code .module-card / .empty-slot} LSS classes;
 * Phase A's last-row-equal-width logic is preserved.
 * <p>
 * B4: Re-renders the grid whenever the player's selected Module set changes
 * (detected by comparing attachment state every N ticks).
 */
public final class ModuleGridController {

    /** CSS class added to cards on the last (incomplete) row to disable stretching. */
    private static final String LAST_ROW_CLASS = "last-row";
    /** Popup overlay selector opened when an empty slot is clicked. */
    private static final String MODULE_POPUP = "#module-popup";
    /** Re-render check interval (in ticks). */
    private static final int REFRESH_INTERVAL = 20;

    private ModuleGridController() {
    }

    /** Attach to the UI tree, called from {@link ink.myumoon.epiphany.client.ui.EpiphanyUIFactory#register()}. */
    public static void attach(UI ui) {
        UIElement grid = selectOne(ui, "#module-grid", UIElement.class);
        Epiphany.LOGGER.info("ModuleGrid attached: grid identity = {}", System.identityHashCode(grid));

        // Initial render.
        refreshGrid(ui, grid);

        // Re-render every REFRESH_INTERVAL ticks to pick up attachment changes
        // (e.g. after a popup-driven Module.select). The check is cheap (compares
        // a cached signature with the current attachment snapshot).
        final String[] lastSignature = {currentSignature()};
        final int[] tickCount = {0};
        grid.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.TICK, e -> {
            tickCount[0]++;
            if (tickCount[0] < REFRESH_INTERVAL) return;
            tickCount[0] = 0;
            String sig = currentSignature();
            if (!sig.equals(lastSignature[0])) {
                lastSignature[0] = sig;
                refreshGrid(ui, grid);
            }
        });
    }

    /**
     * Rebuild the grid: clear children, add one card per selected Module,
     * then fill the rest up to {@link Config#MAX_SELECTED_MODULES} with empty slots.
     * Also re-runs the last-row equal-width adjustment.
     */
    private static void refreshGrid(UI ui, UIElement grid) {
        grid.clearAllChildren();
        int slotCount = Math.max(1, Config.MAX_SELECTED_MODULES.get());

        // Collect selected Module IDs from the client-side attachment mirror.
        List<java.util.Map.Entry<net.minecraft.resources.ResourceLocation, ink.myumoon.epiphany.attachment.ModulePlayerState>> selected =
                new ArrayList<>();
        var data = ClientData.clientData();
        if (data != null) {
            for (var entry : data.modules().entrySet()) {
                if (entry.getValue().selected()) selected.add(entry);
            }
        }

        List<UIElement> slots = new ArrayList<>();
        // Render a "module-card" for each selected module.
        for (var entry : selected) {
            UIElement card = buildSelectedCard(ui, entry.getKey());
            slots.add(card);
            grid.addChild(card);
        }
        // Fill the remaining slots with empty "+select" buttons.
        for (int i = slots.size(); i < slotCount; i++) {
            UIElement slot = buildEmptySlot(ui, i);
            slots.add(slot);
            grid.addChild(slot);
        }

        // Make ALL cards equal-width: measure parent, compute stretched card width,
        // then apply that width to every card. This ensures cards align across rows.
        scheduleEqualWidth(grid, slots);
    }

    /** Measure the grid's actual width, then force every card to the same width so
     *  cards on full rows and cards on the partial last row all match exactly. */
    private static void scheduleEqualWidth(UIElement grid, List<UIElement> slots) {
        java.util.concurrent.atomic.AtomicBoolean adjusted = new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable adjust = () -> {
            if (adjusted.get()) return;
            if (grid.getModularUI() == null || grid.getContentWidth() <= 0) return;
            float parentWidth = grid.getContentWidth();
            if (parentWidth <= 0 || slots.isEmpty()) return;
            // The LSS sets width:90 + min-width:90 + flex-grow:1 + gap-all:2.
            // We let Taffy do the first layout pass, then read the actual width of
            // a full-row card (slot 0) to use as the unified width for ALL cards.
            float actualCardWidth = slots.get(0).getSizeWidth();
            if (actualCardWidth <= 0) return;
            // Apply to every card: explicit width + flexGrow:0 so all rows look identical.
            for (UIElement card : slots) {
                final float w = actualCardWidth;
                card.layout(l -> l.width(w).flexGrow(0).flexShrink(0).maxWidth(w).minWidth(w));
            }
            adjusted.set(true);
        };
        grid.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.LAYOUT_CHANGED, e -> adjust.run());
        final com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener[] tickHolder = new com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener[1];
        final int[] tickCount = {0};
        tickHolder[0] = event -> {
            if (adjusted.get() || tickCount[0] > 20) {
                grid.removeEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.TICK, tickHolder[0]);
                return;
            }
            tickCount[0]++;
            adjust.run();
        };
        grid.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.TICK, tickHolder[0]);
    }

    /** Build a card for an already-selected Module with Insight tree. */
    private static UIElement buildSelectedCard(UI ui, ResourceLocation moduleId) {
        UIElement card = new UIElement();
        card.addClass("module-card");

        var moduleData = ClientData.module(moduleId);
        var data = ClientData.clientData();
        var state = data != null ? data.modules().get(moduleId) : null;

        // --- Title bar: RECT_LIGHT background + module name (left-aligned) ---
        UIElement titleBar = new UIElement();
        titleBar.addClass("module-card-title");
        String name = (moduleData != null && moduleData.name().isPresent())
                ? moduleData.name().get().getString()
                : moduleId.toString();
        Label nameLabel = new Label();
        nameLabel.setText(Component.literal(name));
        nameLabel.textStyle(t -> t.textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.LEFT));
        card.addChild(titleBar.addChild(nameLabel));

        // Hover on title bar → Module tooltip with details.
        titleBar.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.HOVER_TOOLTIPS, e -> {
            var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            lines.add(Component.literal(name).withStyle(net.minecraft.ChatFormatting.WHITE));
            if (moduleData != null && moduleData.description().isPresent()) {
                lines.add(moduleData.description().get().copy()
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                if (moduleData != null && moduleData.onSelectRewardDescription().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.reward")
                            .append(": ")
                            .append(moduleData.onSelectRewardDescription().get())
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                }
                if (moduleData != null && moduleData.onCompleteRewardDescription().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.completion_reward")
                            .append(": ")
                            .append(moduleData.onCompleteRewardDescription().get())
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                }
            } else {
                if (moduleData != null && (moduleData.onSelectRewardDescription().isPresent()
                        || moduleData.onCompleteRewardDescription().isPresent())) {
                    lines.add(Component.translatable("epiphany.ui.shift_hint")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
                }
            }
            e.hoverTooltips = com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips.empty();
            for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
        });

        // --- Insight tree area ---
        UIElement treeArea = new UIElement();
        treeArea.addClass("insight-tree-area");
        if (moduleData != null && state != null) {
            InsightClickHandler handler = (sp, insightId, modId) ->
                    InsightManager.select(sp, insightId, modId);
            InsightTreeView.buildInto(treeArea, ui, moduleId, moduleData, state, true, handler);
        }
        card.addChild(treeArea);
        return card;
    }

    /**
     * Build an empty "+select" slot. Click opens the Module popup — but only
     * if the player has enough Insight Points; otherwise button is disabled.
     */
    private static UIElement buildEmptySlot(UI ui, int slotIndex) {
        UIElement slot = new UIElement();
        slot.addClass("empty-slot");

        // Determine affordability from the latest client attachment snapshot.
        var data = ClientData.clientData();
        int cost = Config.MODULE_SELECT_COST.get();
        boolean canAfford = data != null && data.insightPoints() >= cost;

        Button button = new Button();
        button.setText(Component.translatable("epiphany.ui.select_placeholder"));
        if (canAfford) {
            button.addEventListener(UIEvents.MOUSE_DOWN, e -> Overlay.show(ui, MODULE_POPUP));
        } else {
            button.disabled();
        }

        slot.addChild(button);
        return slot;
    }

    /** Snapshot signature: selected modules + their unlocked insights (for change detection). */
    private static String currentSignature() {
        var data = ClientData.clientData();
        if (data == null) return "";
        var sb = new StringBuilder();
        data.modules().entrySet().stream()
                .filter(e -> e.getValue().selected())
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> {
                    sb.append(e.getKey()).append(':');
                    e.getValue().unlockedInsights().stream().sorted().forEach(id -> sb.append(id).append(';'));
                    sb.append(',');
                });
        return sb.toString();
    }

    /** Convenience: select exactly one element by CSS selector or throw. */
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
