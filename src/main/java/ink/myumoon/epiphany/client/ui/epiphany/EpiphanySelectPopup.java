package ink.myumoon.epiphany.client.ui.epiphany;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.PathData;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Popup for selecting an Epiphany into an empty slot.
 * <p>
 * Phase 5 MVP layout: title + show-locked Switch + scrollable body.
 * The body organizes epiphanies into columns by {@code path}:
 * <ul>
 *   <li>Each Path is a vertical column (left-to-right by path weight asc)</li>
 *   <li>Epiphanies without a path go into an "Other" column, fixed on the right</li>
 *   <li>Paths with no epiphanies are not shown</li>
 *   <li>Clicking an epiphany calls EpiphanyManager.select on the server</li>
 * </ul>
 */
public final class EpiphanySelectPopup {

    public static final class Filter {
        public boolean showLocked;
        public static Filter initial() {
            Filter f = new Filter();
            f.showLocked = false;
            return f;
        }
    }

    public static final ResourceLocation OTHER_PATH_MARKER =
            ResourceLocation.fromNamespaceAndPath("epiphany", "_other");

    private record Candidate(ResourceLocation id, EpiphanyData data) {}

    private EpiphanySelectPopup() {
    }

    public static Overlay create() {
        var panel = new UIElement();
        panel.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(460).height(320)
                .paddingAll(8).gapAll(4));
        panel.style(s -> s.background(
                com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites.BORDER));

        Filter filter = Filter.initial();

        // Body first (so the Switch lambda can capture it effectively-final).
        var scroller = new ScrollerView();
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.HORIZONTAL));
        scroller.layout(l -> l.flexGrow(1).widthPercent(100));

        var body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.FLEX_START)
                .gapAll(8));
        scroller.addScrollViewChild(body);

        // Header (title + switch)
        var header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW)
                .widthPercent(100).height(20)
                .marginBottom(4));
        header.addChild(new Label().setText(
                Component.translatable("epiphany.ui.epiphany.select_title")));

        var showLockedLabel = new Label().setText(
                Component.translatable("epiphany.ui.epiphany.show_locked"));
        header.addChild(showLockedLabel);

        var showLockedSwitch = new Switch();
        showLockedSwitch.setOnSwitchChanged(b -> {
            filter.showLocked = b;
            populateBody(body, filter);
        });
        header.addChild(showLockedSwitch);

        panel.addChild(header);
        panel.addChild(scroller);

        populateBody(body, filter);
        return Overlay.of(panel);
    }

    private static void populateBody(UIElement body, Filter filter) {
        removeAllChildren(body);
        if (net.neoforged.fml.loading.FMLLoader.getDist() != net.neoforged.api.distmarker.Dist.CLIENT) {
            return;
        }

        HolderLookup.RegistryLookup<EpiphanyData> epiphanyLookup = ClientData.epiphanyLookup();
        HolderLookup.RegistryLookup<PathData> pathLookup = ClientData.pathLookup();
        PlayerEpiphanyData d = ClientData.clientData();
        if (epiphanyLookup == null || d == null) return;

        // Gather visible candidates.
        List<Candidate> candidates = new ArrayList<>();
        epiphanyLookup.listElements().forEach(holder -> {
            EpiphanyData e = holder.value();
            boolean isSelectable = e.initialState() == InitialState.SELECTABLE;
            if (isSelectable || filter.showLocked) {
                candidates.add(new Candidate(holder.key().location(), e));
            }
        });

        // Group by path: ResourceLocation → list of candidates, preserving order.
        // "Other" column is collected separately so it always stays at the right.
        Map<ResourceLocation, List<Candidate>> byPath = new LinkedHashMap<>();
        List<Candidate> others = new ArrayList<>();
        for (Candidate c : candidates) {
            var path = c.data().path();
            if (path.isPresent()) {
                byPath.computeIfAbsent(path.get(), k -> new ArrayList<>()).add(c);
            } else {
                others.add(c);
            }
        }
        // Sort each group by weight asc, then by id.
        Comparator<Candidate> byWeight = Comparator
                .<Candidate>comparingInt(c -> c.data().weight())
                .thenComparing(c -> c.id().toString());
        byPath.values().forEach(list -> list.sort(byWeight));
        others.sort(byWeight);

        // Sort path columns by their PathData.weight asc. PathData may be missing
        // from the registry (e.g. epiphany points to a non-existent path); missing
        // paths are treated as Integer.MAX_VALUE so they sort after real ones.
        List<ResourceLocation> sortedPathKeys = new ArrayList<>(byPath.keySet());
        sortedPathKeys.sort(Comparator.comparingInt(pathId -> {
            if (pathLookup == null) return Integer.MAX_VALUE;
            PathData pd = ClientData.path(pathId);
            return pd != null ? pd.weight() : Integer.MAX_VALUE;
        }));

        // Render grouped columns.
        for (ResourceLocation pathId : sortedPathKeys) {
            body.addChild(pathColumn(pathId, byPath.get(pathId), pathLookup));
        }
        // "Other" column last (if non-empty).
        if (!others.isEmpty()) {
            body.addChild(otherColumn(others));
        }
    }

    /** Build a column labelled with the path's name (or id fallback). */
    private static UIElement pathColumn(ResourceLocation pathId,
                                        List<Candidate> candidates,
                                        HolderLookup.RegistryLookup<PathData> pathLookup) {
        ColumnParts parts = buildColumnCommon(candidates);
        // Header label
        Component header;
        if (pathLookup != null) {
            PathData pd = ClientData.path(pathId);
            header = (pd != null && pd.name().isPresent())
                    ? pd.name().get()
                    : Component.literal(pathId.toString());
        } else {
            header = Component.literal(pathId.toString());
        }

        var col = new UIElement();
        col.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .heightPercent(100).width(120)
                .gapAll(4).paddingAll(4));
        col.addChild(new Label().setText(header));
        for (var child : parts.rows()) col.addChild(child);
        return col;
    }

    /** Build the "Other" column with the i18n header. */
    private static UIElement otherColumn(List<Candidate> candidates) {
        ColumnParts parts = buildColumnCommon(candidates);
        var col = new UIElement();
        col.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .heightPercent(100).width(120)
                .gapAll(4).paddingAll(4));
        col.addChild(new Label().setText(
                Component.translatable("epiphany.ui.epiphany.other")));
        for (var child : parts.rows()) col.addChild(child);
        return col;
    }

    private record ColumnParts(List<UIElement> rows) {}

    private static ColumnParts buildColumnCommon(List<Candidate> candidates) {
        List<UIElement> rows = new ArrayList<>();
        for (Candidate c : candidates) {
            rows.add(buildEpiphanyButton(c.id(), c.data()));
        }
        return new ColumnParts(rows);
    }

    private static UIElement buildEpiphanyButton(ResourceLocation epiphanyId, EpiphanyData e) {
        var row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW)
                .widthPercent(100).height(28)
                .gapAll(4));

        Component name = e.name().isPresent()
                ? e.name().get()
                : Component.literal(epiphanyId.toString());
        row.addChild(new Label().setText(name));

        var selectBtn = new Button();
        selectBtn.setText(Component.translatable("epiphany.ui.select"));
        boolean isLocked = e.initialState() == InitialState.LOCKED;
        if (!isLocked) {
            selectBtn.setOnServerClick(ev -> {
                Player p = ev.currentElement.getModularUI().player;
                if (p instanceof ServerPlayer sp) {
                    ink.myumoon.epiphany.api.EpiphanyManager.select(sp, epiphanyId);
                }
            });
        }
        row.addChild(selectBtn);
        return row;
    }

    private static void removeAllChildren(UIElement element) {
        for (var child : new ArrayList<>(element.getChildren())) {
            element.removeChild(child);
        }
    }
}
