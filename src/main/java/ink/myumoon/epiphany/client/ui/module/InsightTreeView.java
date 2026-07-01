package ink.myumoon.epiphany.client.ui.module;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.InsightEntry;
import ink.myumoon.epiphany.content.InsightTreeResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Phase 3 MVP view of a single module's Insight tree.
 * <p>
 * Rendering: for each depth level (starting from 0), a horizontal flex row of
 * insight nodes. Each node is a Button whose server-side click handler invokes
 * {@code InsightManager.select(player, insightId, moduleId)} when the node is
 * unlockable. Connector lines between layers are deferred to Phase 3.5.
 */
public final class InsightTreeView {

    private InsightTreeView() {
    }

    public static UIElement create(ResourceLocation moduleId,
                                   List<InsightEntry> entries,
                                   ModulePlayerState state) {
        // Inner scroller so tall trees remain visible inside the fixed-height card.
        var scroller = new ScrollerView();
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL));
        scroller.layout(l -> l.flexGrow(1).widthPercent(100));

        var root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(4));

        // Group entries by depth, preserve order.
        int maxDepth = entries.stream().mapToInt(InsightEntry::depth).max().orElse(-1);
        Set<ResourceLocation> unlocked = state.unlockedInsights();
        for (int depth = 0; depth <= maxDepth; depth++) {
            var row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).gapAll(4));
            for (InsightEntry entry : entries) {
                if (entry.depth() != depth) continue;
                row.addChild(insightNode(moduleId, entry, entries, unlocked));
            }
            root.addChild(row);
        }
        scroller.addScrollViewChild(root);
        return scroller;
    }

    private static UIElement insightNode(ResourceLocation moduleId,
                                         InsightEntry entry,
                                         List<InsightEntry> allEntries,
                                         Set<ResourceLocation> unlocked) {
        // Use a synthetic "module data" wrapper to call canUnlock. We build a
        // minimal ModuleData-like via InsightTreeResolver, but it needs the
        // real ModuleData — we look it up via the entries' parent.
        InsightData insight = ClientData.insight(entry.id());
        boolean isUnlocked = unlocked.contains(entry.id());

        var button = new Button();
        button.layout(l -> l.width(64).height(24));

        Component text;
        if (insight != null && insight.name().isPresent()) {
            text = insight.name().get();
        } else {
            text = Component.literal(entry.id().toString());
        }
        button.setText(text);

        if (isUnlocked) {
            // Already unlocked — no-op. (We can visually mark it later via style.)
        } else {
            // Unlockable only if all ancestors are unlocked.
            boolean canUnlock = canUnlockSafe(moduleId, allEntries, entry.id(), unlocked);
            if (canUnlock) {
                button.setOnServerClick(event -> {
                    Player p = event.currentElement.getModularUI().player;
                    if (p instanceof ServerPlayer sp) {
                        // InsightManager.select requires insightId and moduleId.
                        ink.myumoon.epiphany.api.InsightManager.select(sp, entry.id(), moduleId);
                    }
                });
            }
            // else: locked → no handler, button is effectively disabled.
        }
        return button;
    }

    /**
     * Client-side pre-check whether an insight is currently unlockable.
     * The server-side {@link ink.myumoon.epiphany.api.InsightManager#select}
     * still re-validates everything (defense in depth — UI button being
     * clickable doesn't guarantee validity; see review notes).
     */
    private static boolean canUnlockSafe(ResourceLocation moduleId,
                                         List<InsightEntry> entries,
                                         ResourceLocation insightId,
                                         Set<ResourceLocation> unlocked) {
        // Need a ModuleData wrapper to call InsightTreeResolver.canUnlock.
        // We construct a synthetic one if real lookup isn't available; the
        // Insights field is all we need for tree resolution.
        ink.myumoon.epiphany.content.ModuleData module =
                new ink.myumoon.epiphany.content.ModuleData(
                        java.util.Optional.empty(), java.util.Optional.empty(),
                        java.util.Optional.empty(), java.util.Optional.empty(),
                        ink.myumoon.epiphany.content.InitialState.SELECTABLE,
                        new ArrayList<>(entries),
                        java.util.Optional.empty(), java.util.Optional.empty(),
                        java.util.Optional.empty(), java.util.Optional.empty(),
                        0);
        // Build a temp PlayerEpiphanyData just enough for the resolver: it only
        // queries modules().get(moduleId).selected + unlockedInsights.
        // For the client pre-check, we assume module is selected (the card only
        // renders for selected modules). Real check is server-side.
        ink.myumoon.epiphany.attachment.PlayerEpiphanyData dummy =
                new ink.myumoon.epiphany.attachment.PlayerEpiphanyData(
                        0, 0, 0,
                        java.util.Collections.singletonMap(moduleId,
                                new ModulePlayerState(true, true, false, unlocked)),
                        java.util.Collections.emptyMap(),
                        java.util.Collections.emptyMap(),
                        0, 0);
        return InsightTreeResolver.canUnlock(dummy, moduleId, module, insightId);
    }
}
