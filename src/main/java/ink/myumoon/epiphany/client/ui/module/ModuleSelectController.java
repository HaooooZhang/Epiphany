package ink.myumoon.epiphany.client.ui.module;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Optional;

/**
 * Phase B controller for the Module selection popup.
 * <p>
 * Populates {@code #module-popup-list} with one card per unlocked && unselected
 * Module, sorted by weight. Each card has a name label + "选择" button; clicking
 * the button calls {@link ModuleManager#select(ServerPlayer, ResourceLocation)}
 * on the server, then success-closes the popup.
 * <p>
 * Simplified per Phase B plan: no hover-preview, no Insight tree thumbnail, no
 * Switch filter, no description body. Those land in Phase C.
 */
public final class ModuleSelectController {

    private static final String MODULE_POPUP = "#module-popup";
    private static final String ERROR_LABEL = "#module-popup-error";
    private static final String LIST_SELECTOR = "#module-popup-list";

    private ModuleSelectController() {
    }

    /** Filter toggle — show locked modules when true. */
    private static boolean showLocked = false;

    /** Attach: close handlers + hardcoded Switch + TICK refresh. */
    public static void attach(UI ui) {
        Overlay.attachCloseHandlers(ui, MODULE_POPUP);

        // Hardcoded toggle row: label + switch with calculated width (Taffy can't auto-size).
        ui.select("#module-toggle-row").findFirst().ifPresent(row -> {
            String labelText = Component.translatable("epiphany.ui.module.show_locked").getString();
            int labelW = labelText.length() * 8;   // ~8px per CJK char at fontSize 8
            int switchW = 24;
            int gap = 4;
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
            sw.setOnSwitchChanged(on -> {
                showLocked = on;
                refreshList(ui);
            });
            row.addChild(sw);
        });

        refreshList(ui);

        // TICK: refresh list when popup is visible + data changes.
        var popup = ui.select(MODULE_POPUP).findFirst();
        var lastSig = new String[]{""};
        var tickCount = new int[]{0};
        popup.ifPresent(overlay -> overlay.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.TICK, e -> {
            if (!overlay.isDisplayed()) return;
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

    /** Rebuild the list from the latest client-side attachment snapshot. */
    private static void refreshList(UI ui) {
        UIElement list = selectOne(ui, LIST_SELECTOR, UIElement.class);
        list.clearAllChildren();

        var lookup = ClientData.moduleLookup();
        var data = ClientData.clientData();
        if (lookup == null || data == null) return;

        int moduleSelectCost = Config.MODULE_SELECT_COST.get();
        int playerPoints = data.insightPoints();

        // Collect candidate modules first, sort by weight asc + id alphabetical.
        record Candidate(ResourceLocation id, ModuleData module, boolean unlocked) {}
        var candidates = new java.util.ArrayList<Candidate>();
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

    /** Build a single Module selection card matching main UI card structure.
     *  Layout: title bar (RECT_LIGHT + name + select button) + body (desc/tree swap). */
    private static UIElement buildCard(UI ui, ResourceLocation moduleId, ModuleData module, boolean affordable) {
        UIElement card = new UIElement();
        card.addClass("module-card");   // same class as main UI for identical sizing

        // --- Title bar: matches .module-card-title (RECT_LIGHT bg, 14px height) ---
        UIElement titleBar = new UIElement();
        titleBar.addClass("module-card-title");
        String name = module.name().isPresent() ? module.name().get().getString() : moduleId.toString();
        Label nameLabel = new Label();
        nameLabel.setText(Component.literal(name));
        nameLabel.textStyle(t -> t.fontSize(8).textColor(0xFFFFFFFF).textShadow(true)
                .textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.LEFT));
        nameLabel.layout(l -> l.maxWidth(68));  // 90 - 16(btn) - 4(pad) - 2(gap)
        titleBar.addChild(nameLabel);

        // Select button — client-side RPC + close popup + module tooltip.
        Button btn = new Button();
        btn.setText(Component.translatable("epiphany.ui.select"));
        if (!affordable) {
            btn.disabled();
        }
        // Module tooltip on button (most reliable hover target).
        btn.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.HOVER_TOOLTIPS, e -> {
            var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            lines.add(Component.literal(name).withStyle(net.minecraft.ChatFormatting.WHITE));
            if (module.description().isPresent()) {
                lines.add(module.description().get().copy()
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                if (module.onSelectRewardDescription().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.reward")
                            .append(": ").append(module.onSelectRewardDescription().get())
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                }
                if (module.onCompleteRewardDescription().isPresent()) {
                    lines.add(Component.translatable("epiphany.tooltip.completion_reward")
                            .append(": ").append(module.onCompleteRewardDescription().get())
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                }
            } else if (module.onSelectRewardDescription().isPresent()
                    || module.onCompleteRewardDescription().isPresent()) {
                lines.add(Component.translatable("epiphany.ui.shift_hint")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
            }
            if (!affordable) {
                var cd = ClientData.clientData();
                var st = cd != null ? cd.modules().get(moduleId) : null;
                boolean isUnlocked = st != null ? st.unlocked() : module.initialState() == InitialState.SELECTABLE;
                lines.add(Component.translatable(isUnlocked ? "epiphany.ui.error.no_points" : "epiphany.ui.module.locked_hint")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
            e.hoverTooltips = com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips.empty();
            for (var ln : lines) e.hoverTooltips = e.hoverTooltips.append(ln);
        });
        btn.layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .right(2).top(0).flexShrink(0));
        if (affordable) {
            btn.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_DOWN, e -> {
                com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor.rpcToServer(
                        "epiphany.select_module", moduleId.toString());
                Overlay.hide(ui, MODULE_POPUP);
            });
        }
        btn.addClass("module-select-btn");
        titleBar.addChild(btn);
        card.addChild(titleBar);

        // --- Body: description (default) / insight tree (hover), in single slot ---
        UIElement bodySlot = new UIElement();
        bodySlot.layout(l -> l.flex(1).widthPercent(100)
                .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN)
                .justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        card.addChild(bodySlot);

        // Description — multiple Labels (Taffy Label doesn't render \n).
        if (module.description().isPresent()) {
            String raw = module.description().get().getString();
            int maxPerLine = 10;
            int start = 0;
            while (start < raw.length()) {
                int end = Math.min(start + maxPerLine, raw.length());
                Label lineLbl = new Label();
                lineLbl.setText(Component.literal(raw.substring(start, end)));
                lineLbl.textStyle(t -> t.fontSize(8).textColor(0xFFAAAAAA).textShadow(true)
                        .textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.CENTER));
                lineLbl.layout(l -> l.widthPercent(100).flexShrink(0));
                bodySlot.addChild(lineLbl);
                start = end;
            }
        } else {
            // Fallback: show placeholder text so the area isn't empty.
            Label placeholder = new Label();
            placeholder.setText(Component.literal("???"));
            placeholder.textStyle(t -> t.fontSize(8).textColor(0xFF666666).textShadow(false)
                    .textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.CENTER));
            bodySlot.addChild(placeholder);
        }

        // Insight preview element (hidden by default, replaces desc on hover).
        UIElement previewArea = new UIElement();
        previewArea.addClass("insight-tree-area");
        previewArea.setDisplay(false);
        var clientData = ClientData.clientData();
        var moduleState = clientData != null ? clientData.modules().get(moduleId) : null;
        if (moduleState == null) {
            moduleState = ink.myumoon.epiphany.attachment.ModulePlayerState.createDefault();
        }
        ink.myumoon.epiphany.client.ui.insight.InsightTreeView.buildInto(
                previewArea, ui, moduleId, module, moduleState, false, null);
        bodySlot.addChild(previewArea);

        // Hover: swap description ↔ insight preview. Capture phase so children don't block.
        card.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_ENTER, e -> {
            // Hide all description labels, show tree.
            for (int i = 0; i < bodySlot.getChildren().size(); i++) {
                var child = bodySlot.getChildren().get(i);
                if (child != previewArea) child.setDisplay(false);
            }
            previewArea.setDisplay(true);
        }, true);
        card.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_LEAVE, e -> {
            // Show all description labels, hide tree.
            for (int i = 0; i < bodySlot.getChildren().size(); i++) {
                var child = bodySlot.getChildren().get(i);
                if (child != previewArea) child.setDisplay(true);
            }
            previewArea.setDisplay(false);
        }, true);

        return card;
    }

    /** Display an error message in the popup's error label (and un-hide it). */
    private static void showClientError(UI ui, Component message) {
        Optional<UIElement> errEl = ui.select(ERROR_LABEL).findFirst();
        errEl.ifPresent(el -> {
            el.setDisplay(true);
            if (el instanceof Label label) label.setText(message);
        });
    }

    /** Clear any previous error message (hide the label). */
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
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static <T extends UIElement> T selectOne(UI ui, String selector, Class<T> type) {
        Optional<T> first = ui.select(selector, type).findFirst();
        return first.orElseThrow(() -> new IllegalStateException(
                "Epiphany UI missing selector '" + selector + "' of type " + type.getSimpleName()));
    }
}
