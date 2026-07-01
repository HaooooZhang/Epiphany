package ink.myumoon.epiphany.client.ui.module;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Module tab content.
 * <p>
 * Phase 3 MVP: shows a vertically scrollable flex-wrap grid of cards. For each
 * selected Module, a {@link ModuleCard} (containing the Insight tree) is shown.
 * Empty slots up to {@code Config.MAX_SELECTED_MODULES} are rendered as plain
 * grey placeholders — they will trigger a selection popup in Phase 5.
 */
public final class ModuleTabContent {

    private ModuleTabContent() {
    }

    public static UIElement create() {
        var scroller = new ScrollerView();
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL));
        scroller.layout(l -> l.flexGrow(1).widthPercent(100));

        // The flex-wrap grid lives inside scroller's view container.
        var grid = new UIElement();
        grid.layout(l -> l.flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP)
                .gapAll(6));

        // Build the popup overlay. We don't dist-guard here so the UI tree
        // structure is identical on both sides (server builds its own copy
        // but only the client's actually renders). Overlay.create does not
        // touch Minecraft.getInstance directly; only populateBody does, and
        // that is dist-guarded.
        var popup = ModuleSelectPopup.create();

        populateGrid(grid, popup);
        scroller.addScrollViewChild(grid);

        // Wrap scroller + overlay in a column container. The overlay is hidden
        // initially (setVisible(false) inside Overlay.of) and shown when the
        // user clicks an empty placeholder.
        var root = new UIElement();
        root.layout(l -> l.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN)
                .widthPercent(100).heightPercent(100));
        root.addChild(scroller);
        root.addChild(popup.wrapper());
        return root;
    }

    /** Build all module cards + placeholders into the grid. Reads client data once. */
    private static void populateGrid(UIElement grid,
                                     ink.myumoon.epiphany.client.ui.overlay.Overlay popup) {
        if (net.neoforged.fml.loading.FMLLoader.getDist() != net.neoforged.api.distmarker.Dist.CLIENT) {
            return;
        }

        PlayerEpiphanyData data = ClientData.clientData();
        if (data == null) return;

        // Cards for already-selected modules.
        for (Map.Entry<ResourceLocation, ModulePlayerState> entry : data.modules().entrySet()) {
            if (!entry.getValue().selected()) continue;
            ModuleData module = ClientData.module(entry.getKey());
            grid.addChild(ModuleCard.create(entry.getKey(), module, entry.getValue()));
        }

        // Placeholder grid cells: render enough empty slots to reach the configured
        // MAX_SELECTED_MODULES limit, so the user can see "this is where a new
        // Module card goes". Clicking a placeholder opens the selection popup.
        int maxModules = ink.myumoon.epiphany.Config.MAX_SELECTED_MODULES.get();
        long selectedCount = data.modules().values().stream()
                .filter(ModulePlayerState::selected)
                .count();
        int placeHoldersToAdd = Math.max(0, maxModules - (int) selectedCount);
        for (int i = 0; i < placeHoldersToAdd; i++) {
            grid.addChild(emptyPlaceholder(popup));
        }
    }

    /**
     * Phase 5 placeholder: a Button labelled "Empty Slot (+)".
     * We use Button rather than UIElement+Label because Button is
     * inherently interactive and reliable for hit-testing — UIElements
     * containing only a passive Label can miss mouse events in LDLib2.
     */
    private static UIElement emptyPlaceholder(ink.myumoon.epiphany.client.ui.overlay.Overlay popup) {
        var slot = new com.lowdragmc.lowdraglib2.gui.ui.elements.Button();
        slot.setText(Component.translatable("epiphany.ui.module.empty_slot"));
        slot.layout(l -> l.width(140).height(80));
        slot.setOnClick(e -> {
            ink.myumoon.epiphany.Epiphany.LOGGER.info(
                    "[ModuleTab] Empty slot clicked, showing popup.");
            popup.show();
        });
        return slot;
    }
}
