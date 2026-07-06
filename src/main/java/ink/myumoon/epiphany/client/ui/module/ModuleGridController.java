package ink.myumoon.epiphany.client.ui.module;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.InsightManager;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.insight.InsightClickHandler;
import ink.myumoon.epiphany.client.ui.insight.InsightTreeView;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModuleGridController {

    private static final String LAST_ROW_CLASS = "last-row";
    private static final String MODULE_POPUP = "#module-popup";
    private static final int REFRESH_INTERVAL = 1;

    private ModuleGridController() {
    }

    // attach
    public static void attach(UI ui) {
        cachedCardWidth = -1;  // reset cache
        UIElement grid = selectOne(ui, "#module-grid", UIElement.class);
        Epiphany.LOGGER.info("ModuleGrid attached: grid identity = {}", System.identityHashCode(grid));

        // Initial render.
        refreshGrid(ui, grid);

        final String[] lastSignature = {currentSignature()};
        final int[] tickCount = {0};
        grid.addEventListener(UIEvents.TICK, e -> {
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

    // Rebuild the grid
    private static void refreshGrid(UI ui, UIElement grid) {
        grid.clearAllChildren();
        int slotCount = Math.max(1, Config.MAX_SELECTED_MODULES.get());

        // Collect selected Module IDs from the client-side attachment mirror.
        List<Map.Entry<ResourceLocation, ModulePlayerState>> selected =
                new ArrayList<>();
        var data = ClientData.clientData();
        if (data != null) {
            for (var entry : data.modules().entrySet()) {
                if (entry.getValue().selected()) selected.add(entry);
            }
        }

        List<UIElement> slots = new ArrayList<>();
        // Render Module Card
        for (var entry : selected) {
            UIElement card = buildSelectedCard(ui, entry.getKey());
            slots.add(card);
            grid.addChild(card);
        }
        // Fill the remaining slots
        for (int i = slots.size(); i < slotCount; i++) {
            UIElement slot = buildEmptySlot(ui, i);
            slots.add(slot);
            grid.addChild(slot);
        }

        // equal width
        scheduleEqualWidth(grid, slots);
    }

    // cache width revise
    private static float cachedCardWidth = -1;

    // measure width
    private static void scheduleEqualWidth(UIElement grid, List<UIElement> slots) {
        // If already cached, apply immediately and skip measurement.
        if (cachedCardWidth > 0 && !slots.isEmpty()) {
            for (UIElement card : slots) {
                card.layout(l -> l.width(cachedCardWidth).flexGrow(0).flexShrink(0)
                        .maxWidth(cachedCardWidth).minWidth(cachedCardWidth));
            }
            return;
        }
        AtomicBoolean adjusted = new AtomicBoolean(false);
        Runnable adjust = () -> {
            if (adjusted.get()) return;
            if (grid.getModularUI() == null || grid.getContentWidth() <= 0) return;
            float parentWidth = grid.getContentWidth();
            if (parentWidth <= 0 || slots.isEmpty()) return;
            float actualCardWidth = slots.getFirst().getSizeWidth();
            if (actualCardWidth <= 0) return;
            cachedCardWidth = actualCardWidth;
            for (UIElement card : slots) {
                final float w = actualCardWidth;
                card.layout(l -> l.width(w).flexGrow(0).flexShrink(0).maxWidth(w).minWidth(w));
            }
            adjusted.set(true);
        };
        grid.addEventListener(UIEvents.LAYOUT_CHANGED, e -> adjust.run());
        final UIEventListener[] tickHolder = new UIEventListener[1];
        final int[] tickCount = {0};
        tickHolder[0] = event -> {
            if (adjusted.get() || tickCount[0] > 20) {
                grid.removeEventListener(UIEvents.TICK, tickHolder[0]);
                return;
            }
            tickCount[0]++;
            adjust.run();
        };
        grid.addEventListener(UIEvents.TICK, tickHolder[0]);
    }

    // module card with insights
    private static UIElement buildSelectedCard(UI ui, ResourceLocation moduleId) {
        UIElement card = new UIElement();
        card.addClass("module-card");

        var moduleData = ClientData.module(moduleId);
        var data = ClientData.clientData();
        var state = data != null ? data.modules().get(moduleId) : null;

        // title bar
        UIElement titleBar = new UIElement();
        titleBar.addClass("module-card-title");
        String name = (moduleData != null && moduleData.name().isPresent())
                ? moduleData.name().get().getString()
                : moduleId.toString();
        Label nameLabel = new Label();
        nameLabel.setText(Component.literal(name));
        nameLabel.textStyle(t -> t.textAlignHorizontal(Horizontal.LEFT));
        titleBar.addChild(nameLabel);
        card.addChild(titleBar);

        // Tooltip on title bar only
        titleBar.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            var lines = new ArrayList<Component>();
            lines.add(Component.literal(name).withStyle(ChatFormatting.WHITE));
            if (moduleData != null && moduleData.description().isPresent()) {
                lines.add(moduleData.description().get().copy()
                        .withStyle(ChatFormatting.GRAY));
            }
            if (Screen.hasShiftDown()) {
                if (moduleData != null && moduleData.onSelectRewardDescription().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.reward")
                            .append(": ")
                            .append(moduleData.onSelectRewardDescription().get())
                            .withStyle(ChatFormatting.GOLD));
                }
                if (moduleData != null && moduleData.onCompleteRewardDescription().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.completion_reward")
                            .append(": ")
                            .append(moduleData.onCompleteRewardDescription().get())
                            .withStyle(ChatFormatting.GOLD));
                }
            } else {
                if (moduleData != null && (moduleData.onSelectRewardDescription().isPresent()
                        || moduleData.onCompleteRewardDescription().isPresent())) {
                    lines.add(Component.translatable("epiphany.ui.shift_hint")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
            e.hoverTooltips = HoverTooltips.empty();
            for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
        });

        // Insight Tree
        UIElement treeArea = new UIElement();
        treeArea.addClass("insight-tree-area");
        if (moduleData != null && state != null) {
            InsightClickHandler handler = InsightManager::select;
            InsightTreeView.buildInto(treeArea, ui, moduleId, moduleData, state, true, handler);
        }
        card.addChild(treeArea);
        return card;
    }

    // Empty Slot
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

    // Snapshot signature: selected modules + state flags + unlocked insights.
    private static String currentSignature() {
        var data = ClientData.clientData();
        if (data == null) return "";
        var sb = new StringBuilder();
        sb.append("pts=").append(data.insightPoints()).append('|');
        data.modules().entrySet().stream()
                .filter(e -> e.getValue().selected())
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> {
                    var s = e.getValue();
                    sb.append(e.getKey()).append(':')
                            .append(s.completed() ? 'c' : '-').append(s.unlocked() ? 'u' : '-').append(':');
                    s.unlockedInsights().stream().sorted().forEach(id -> sb.append(id).append(';'));
                    sb.append(',');
                });
        return sb.toString();
    }

    // select exactly one element by CSS selector or throw.
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
