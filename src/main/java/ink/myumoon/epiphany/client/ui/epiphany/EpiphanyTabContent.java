package ink.myumoon.epiphany.client.ui.epiphany;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.content.EpiphanyData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Epiphany tab content.
 * <p>
 * Phase 4 MVP layout:
 * <pre>
 *   ┌─ Header (slots usage KPI) ───────────────────────┐
 *   │  Used X / Total Y slots                          │
 *   └──────────────────────────────────────────────────┘
 *   ┌─ Scroller (vertical, flex-wrap grid) ────────────┐
 *   │  [EpiphanyCard] [EpiphanyCard] ... (selected)    │
 *   │  [Empty hex slot] [Empty hex slot] ... (until Y) │
 *   └──────────────────────────────────────────────────┘
 * </pre>
 * Empty slots render a "no epiphany here" placeholder and turn clickable in
 * Phase 5's EpiphanySelectPopup.
 * <p>
 * Note: the slot count here is the player's <b>currently granted</b>
 * {@code epiphanySlots}, not the global {@code MAX_EPIPHANY_SLOTS} cap.
 * A player only has slots while they've completed modules — those slots
 * each allow selecting one epiphany from the global pool.
 */
public final class EpiphanyTabContent {

    private EpiphanyTabContent() {
    }

    public static UIElement create() {
        // Build popup overlay without dist guard so the UI tree is identical
        // on both sides (only populateBody reads client-only data and is guarded).
        var popup = EpiphanySelectPopup.create();

        var root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .widthPercent(100).heightPercent(100)
                .gapAll(4).paddingAll(8));

        // 1. Header: dynamic usage KPI ("Used X / Total Y slots")
        var header = new Label();
        header.layout(l -> l.widthPercent(100).height(20));
        header.bindDataSource(SupplierDataSource.of(EpiphanyTabContent::slotsKpi));
        root.addChild(header);

        // 2. Scrollable card grid below.
        var scroller = new ScrollerView();
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL));
        scroller.layout(l -> l.flexGrow(1).widthPercent(100));

        var grid = new UIElement();
        grid.layout(l -> l.flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP)
                .alignItems(AlignItems.FLEX_START)
                .gapAll(6));
        populateGrid(grid, popup);
        scroller.addScrollViewChild(grid);

        root.addChild(scroller);
        root.addChild(popup.wrapper());
        return root;
    }

    private static void populateGrid(UIElement grid,
                                     ink.myumoon.epiphany.client.ui.overlay.Overlay popup) {
        if (net.neoforged.fml.loading.FMLLoader.getDist() != net.neoforged.api.distmarker.Dist.CLIENT) {
            return;
        }

        PlayerEpiphanyData data = ClientData.clientData();
        if (data == null) return;

        // 1. Cards for already-selected epiphanies.
        for (Map.Entry<ResourceLocation, EpiphanyPlayerState> entry : data.epiphanies().entrySet()) {
            if (!entry.getValue().selected()) continue;
            EpiphanyData epiphany = ClientData.epiphany(entry.getKey());
            grid.addChild(EpiphanyCard.create(entry.getKey(), epiphany));
        }

        // 2. Empty placeholder slots.
        // Display total slots = MAX_EPIPHANY_SLOTS (the configured cap), so the
        // player always sees the full potential layout. The player's actually
        // granted {@code data.epiphanySlots()} only matters at selection time —
        // the server-side EpiphanyManager rejects selecting beyond granted slots.
        // As a defensive guard: if the player has somehow been granted more slots
        // than MAX (e.g. config tightened after the fact) or selected more than
        // MAX, we never hide already-selected cards — the larger of the two wins.
        long selectedCount = data.epiphanies().values().stream()
                .filter(EpiphanyPlayerState::selected)
                .count();
        int maxSlots = ink.myumoon.epiphany.Config.MAX_EPIPHANY_SLOTS.get();
        int displayTotal = Math.max(maxSlots, data.epiphanySlots());
        int slotsToFill = Math.max(0, displayTotal - (int) selectedCount);
        for (int i = 0; i < slotsToFill; i++) {
            grid.addChild(emptySlotPlaceholder(popup));
        }
    }

    private static UIElement emptySlotPlaceholder(
            ink.myumoon.epiphany.client.ui.overlay.Overlay popup) {
        // Use Button rather than UIElement+Label for reliable hit-testing.
        var slot = new com.lowdragmc.lowdraglib2.gui.ui.elements.Button();
        slot.setText(Component.translatable("epiphany.ui.epiphany.empty_slot"));
        slot.layout(l -> l.width(120).height(80));
        slot.setOnClick(e -> {
            ink.myumoon.epiphany.Epiphany.LOGGER.info(
                    "[EpiphanyTab] Empty slot clicked, showing popup.");
            popup.show();
        });
        return slot;
    }

    private static Component slotsKpi() {
        PlayerEpiphanyData d = ClientData.clientData();
        int used = d != null ? d.usedEpiphanySlots() : 0;
        int maxSlots = ink.myumoon.epiphany.Config.MAX_EPIPHANY_SLOTS.get();
        int total = d != null ? Math.max(maxSlots, d.epiphanySlots()) : maxSlots;
        return Component.translatable("epiphany.ui.epiphany.slots_kpi", used, total);
    }
}
