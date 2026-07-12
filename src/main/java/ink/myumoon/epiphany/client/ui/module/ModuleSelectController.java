package ink.myumoon.epiphany.client.ui.module;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.insight.InsightTreeView;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ModuleSelectController {

    private static final String MODULE_POPUP = "#module-popup";
    private static final String ERROR_LABEL = "#module-popup-error";
    private static final String LIST_SELECTOR = "#module-popup-list";

    private ModuleSelectController() {
    }

    // Filter toggle
    private static boolean showLocked = false;

    // Attach
    public static void attach(UI ui) {
        showLocked = false;
        Overlay.attachCloseHandlers(ui, MODULE_POPUP);

        // Hardcoded toggle row
        ui.select("#module-toggle-row").findFirst().ifPresent(row -> {
            String labelText = Component.translatable("epiphany.ui.module.show_locked").getString();
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
            sw.setOnSwitchChanged(on -> {
                showLocked = on;
                refreshList(ui);
            });
            row.addChild(sw);
        });

        refreshList(ui);

        // refresh list
        var popup = ui.select(MODULE_POPUP).findFirst();
        var lastSig = new String[]{""};
        var tickCount = new int[]{0};
        popup.ifPresent(overlay -> overlay.addEventListener(UIEvents.TICK, e -> {
            if (!overlay.isDisplayed()) { showLocked = false; return; }
            tickCount[0]++;
            if (tickCount[0] < 5) return;
            tickCount[0] = 0;
            String sig = currentPopupSignature();
            if (!sig.equals(lastSig[0])) {
                lastSig[0] = sig;
                refreshList(ui);
            }
        }));
    }

    // Rebuild the list from the latest client-side attachment snapshot.
    private static void refreshList(UI ui) {
        UIElement list = selectOne(ui, LIST_SELECTOR, UIElement.class);
        list.clearAllChildren();

        var lookup = ClientData.moduleLookup();
        var data = ClientData.clientData();
        if (lookup == null || data == null) return;

        int moduleSelectCost = Config.MODULE_SELECT_COST.get();
        int playerPoints = data.insightPoints();

        // Collect candidate modules
        record Candidate(ResourceLocation id, ModuleData module, boolean unlocked) {}
        var candidates = new ArrayList<Candidate>();
        lookup.listElements().forEach(holder -> {
            ResourceLocation id = holder.key().location();
            ModuleData module = holder.value();
            ModulePlayerState state = data.modules().get(id);
            boolean selected = state != null && state.selected();
            if (selected) return;
            boolean unlocked = state != null
                    ? state.unlocked()
                    : module.initialState() == InitialState.SELECTABLE;
            if (!unlocked && !showLocked) return;
            candidates.add(new Candidate(id, module, unlocked));
        });
        candidates.sort(Comparator.<Candidate, Integer>comparing(c -> c.module.weight())
                .thenComparing(c -> c.id.toString()));

        for (var c : candidates) {
            boolean affordable = c.unlocked && playerPoints >= moduleSelectCost;
            UIElement card = buildCard(ui, c.id(), c.module(), affordable);
            list.addChild(card);
        }
    }

    // Module Card
    private static UIElement buildCard(UI ui, ResourceLocation moduleId, ModuleData module, boolean affordable) {
        UIElement card = new UIElement();
        card.addClass("module-card");

        // Title Bar
        UIElement titleBar = new UIElement();
        titleBar.addClass("module-card-title");
        String name = module.effectiveName(moduleId).getString();
        Label nameLabel = new Label();
        nameLabel.setText(Component.literal(name));
        nameLabel.textStyle(t -> t.fontSize(8).textColor(0xFFFFFFFF).textShadow(true)
                .textAlignHorizontal(Horizontal.LEFT));
        nameLabel.layout(l -> l.maxWidth(68));  // 90 - 16 - 4 - 2 = 68
        titleBar.addChild(nameLabel);

        // Select button
        Button btn = new Button();
        btn.setText(Component.translatable("epiphany.ui.select"));
        if (!affordable) {
            btn.disabled();
        }
        // Module tooltip on button (most reliable hover target).
        btn.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            var lines = new ArrayList<Component>();
            lines.add(Component.literal(name).withStyle(ChatFormatting.WHITE));
            lines.add(module.effectiveDescription(moduleId).copy()
                    .withStyle(ChatFormatting.GRAY));
            if (Screen.hasShiftDown()) {
                if (module.onSelectReward().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.reward")
                            .append(": ").append(module.effectiveOnSelectRewardDescription(moduleId))
                            .withStyle(ChatFormatting.GOLD));
                }
                if (module.onCompleteReward().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.completion_reward")
                            .append(": ").append(module.effectiveOnCompleteRewardDescription(moduleId))
                            .withStyle(ChatFormatting.GOLD));
                }
            } else if (module.onSelectReward().isPresent()
                    || module.onCompleteReward().isPresent()) {
                lines.add(Component.translatable("epiphany.ui.shift_hint")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
            if (!affordable) {
                var cd = ClientData.clientData();
                var st = cd != null ? cd.modules().get(moduleId) : null;
                boolean isUnlocked = st != null ? st.unlocked() : module.initialState() == InitialState.SELECTABLE;
                if (!isUnlocked && module.condition().isPresent()) {
                    lines.add(module.effectiveConditionDescription(moduleId).copy()
                            .withStyle(ChatFormatting.DARK_RED));
                }
                lines.add(Component.translatable(isUnlocked ? "epiphany.ui.error.no_points" : "epiphany.ui.module.locked_hint")
                        .withStyle(ChatFormatting.RED));
            }
            e.hoverTooltips = HoverTooltips.empty();
            for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
        });
        btn.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                .right(2).top(0).flexShrink(0));
        if (affordable) {
            btn.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                RPCPacketDistributor.rpcToServer(
                        "epiphany.select_module", moduleId.toString());
                Overlay.hide(ui, MODULE_POPUP);
            });
        }
        btn.addClass("module-select-btn");
        titleBar.addChild(btn);
        card.addChild(titleBar);

        // description / insight preview
        UIElement bodySlot = new UIElement();
        bodySlot.layout(l -> l.flex(1).widthPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .justifyContent(AlignContent.CENTER)
                .alignItems(AlignItems.CENTER));
        card.addChild(bodySlot);

        // Description
        {
            String raw = module.effectiveDescription(moduleId).getString();
            var font = Minecraft.getInstance().font;
            int maxPixelWidth = 84;
            int start = 0;
            while (start < raw.length()) {
                int end = start + 1;
                while (end < raw.length() && font.width(raw.substring(start, end + 1)) <= maxPixelWidth) {
                    end++;
                }
                Label lineLbl = new Label();
                lineLbl.setText(Component.literal(raw.substring(start, end)));
                lineLbl.textStyle(t -> t.fontSize(8).textColor(0xFFAAAAAA).textShadow(true)
                        .textAlignHorizontal(Horizontal.CENTER));
                lineLbl.layout(l -> l.widthPercent(100).flexShrink(0));
                bodySlot.addChild(lineLbl);
                start = end;
            }
        }

        // Insight preview element
        UIElement previewArea = new UIElement();
        previewArea.addClass("insight-tree-area");
        previewArea.setDisplay(false);
        var clientData = ClientData.clientData();
        var moduleState = clientData != null ? clientData.modules().get(moduleId) : null;
        if (moduleState == null) {
            moduleState = ModulePlayerState.createDefault();
        }
        InsightTreeView.buildInto(
                previewArea, ui, moduleId, module, moduleState, false, null);
        bodySlot.addChild(previewArea);

        // Hover
        card.addEventListener(UIEvents.MOUSE_ENTER, e -> {
            // Hide all description labels, show tree.
            for (int i = 0; i < bodySlot.getChildren().size(); i++) {
                var child = bodySlot.getChildren().get(i);
                if (child != previewArea) child.setDisplay(false);
            }
            previewArea.setDisplay(true);
        }, true);
        card.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            // Show all description labels, hide tree.
            for (int i = 0; i < bodySlot.getChildren().size(); i++) {
                var child = bodySlot.getChildren().get(i);
                if (child != previewArea) child.setDisplay(true);
            }
            previewArea.setDisplay(false);
        }, true);

        return card;
    }

    // Error Message
    private static void showClientError(UI ui, Component message) {
        Optional<UIElement> errEl = ui.select(ERROR_LABEL).findFirst();
        errEl.ifPresent(el -> {
            el.setDisplay(true);
            if (el instanceof Label label) label.setText(message);
        });
    }

    // clear message
    private static void clearClientError(UI ui) {
        ui.select(ERROR_LABEL).findFirst().ifPresent(el -> el.setDisplay(false));
    }

    private static String currentPopupSignature() {
        var data = ClientData.clientData();
        if (data == null) return "";
        return showLocked + "|" + data.insightPoints() + "|"
                + data.modules().entrySet().stream()
                .map(e -> e.getKey() + ":" + (e.getValue().selected() ? "s" : "-"))
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static <T extends UIElement> T selectOne(UI ui, String selector, Class<T> type) {
        Optional<T> first = ui.select(selector, type).findFirst();
        return first.orElseThrow(() -> new IllegalStateException(
                "Epiphany UI missing selector '" + selector + "' of type " + type.getSimpleName()));
    }
}
