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

    /** Attach: register close handlers + populate the list once. */
    public static void attach(UI ui) {
        Overlay.attachCloseHandlers(ui, MODULE_POPUP);
        // Match main UI: ScrollerView default viewPort padding is 5. Main UI uses the
        // default (no override), so the popup should too for identical card edge spacing.
        refreshList(ui);
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
        record Candidate(ResourceLocation id, ModuleData module) {}
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
            if (!unlocked) return;
            candidates.add(new Candidate(id, module));
        });
        candidates.sort(Comparator.<Candidate, Integer>comparing(c -> c.module.weight())
                .thenComparing(c -> c.id.toString()));

        for (var c : candidates) {
            boolean affordable = playerPoints >= moduleSelectCost;
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
        titleBar.addChild(nameLabel);

        // Select button (right side of title bar).
        Button btn = new Button();
        btn.setText(Component.translatable("epiphany.ui.select"));
        if (!affordable) btn.disabled();
        btn.setOnServerClick(e -> {
            ServerPlayer sp = (ServerPlayer) e.currentElement.getModularUI().player;
            int cost = Config.MODULE_SELECT_COST.get();
            var d = sp.getData(ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes.EPIPHANY_DATA);
            if (d.insightPoints() < cost) {
                showClientError(ui, Component.translatable("epiphany.ui.error.no_points"));
                return;
            }
            if (ModuleManager.isSelected(sp, moduleId)) {
                showClientError(ui, Component.translatable("epiphany.ui.error.already_selected"));
                return;
            }
            ModuleManager.select(sp, moduleId);
            Overlay.hide(ui, MODULE_POPUP);
        });
        btn.addClass("module-select-btn");
        btn.layout(l -> l.flexShrink(0));
        titleBar.addChild(btn);
        card.addChild(titleBar);

        // --- Body: description (default) / insight tree (hover) ---
        // Hardcoded description: explicit padding + centered text in Java.
        UIElement descArea = new UIElement();
        descArea.layout(l -> l.flex(1).widthPercent(100).paddingAll(4)
                .justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        if (module.description().isPresent()) {
            Label descLabel = new Label();
            descLabel.setText(module.description().get());
            descLabel.textStyle(t -> t.fontSize(8).textColor(0xFFAAAAAA).textShadow(true)
                    .textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.CENTER));
            descLabel.layout(l -> l.widthPercent(100));
            descArea.addChild(descLabel);
        }

        // Insight preview element (hidden by default).
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

        card.addChild(descArea);
        card.addChild(previewArea);

        // Hover: swap description ↔ insight preview.
        card.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_ENTER, e -> {
            descArea.setDisplay(false);
            previewArea.setDisplay(true);
        });
        card.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_LEAVE, e -> {
            descArea.setDisplay(true);
            previewArea.setDisplay(false);
        });

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

    private static <T extends UIElement> T selectOne(UI ui, String selector, Class<T> type) {
        Optional<T> first = ui.select(selector, type).findFirst();
        return first.orElseThrow(() -> new IllegalStateException(
                "Epiphany UI missing selector '" + selector + "' of type " + type.getSimpleName()));
    }
}
