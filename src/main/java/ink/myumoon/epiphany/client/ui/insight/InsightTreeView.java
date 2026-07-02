package ink.myumoon.epiphany.client.ui.insight;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.InsightEntry;
import ink.myumoon.epiphany.content.InsightTreeResolver;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Renders an Insight tree using absolute positioning.
 * Layout: nodes grouped by depth (row per depth); lines drawn parent→child.
 * Content is centered in the container after layout.
 */
public class InsightTreeView {

    private static final int NODE_SIZE = 18;
    private static final int LINE_COLOR = 0xFF666666;

    private InsightTreeView() {
    }

    public static void buildInto(UIElement container, UI ui,
                                  ResourceLocation moduleId, ModuleData module,
                                  ModulePlayerState state,
                                  boolean interactive, InsightClickHandler clickHandler) {
        container.clearAllChildren();
        var insights = module.insights();
        if (insights.isEmpty()) return;

        Map<Integer, List<InsightEntry>> byDepth = new TreeMap<>();
        for (InsightEntry entry : insights) {
            byDepth.computeIfAbsent(entry.depth(), k -> new ArrayList<>()).add(entry);
        }
        int maxDepth = byDepth.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        int maxPerDepth = byDepth.values().stream().mapToInt(List::size).max().orElse(0);

        int colPitch = NODE_SIZE + 10;
        int rowPitch = NODE_SIZE + 12;
        int contentWidth = maxPerDepth * colPitch;
        int contentHeight = (maxDepth + 1) * rowPitch;

        // Inner content wrapper.
        UIElement content = new UIElement();
        content.layout(l -> {
            l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE);
            l.left(0).top(0);
            l.width(contentWidth).height(contentHeight);
        });

        // Node positions.
        Map<ResourceLocation, int[]> nodePositions = new HashMap<>();

        for (var depthEntry : byDepth.entrySet()) {
            int depth = depthEntry.getKey();
            var entries = depthEntry.getValue();
            int y = depth * rowPitch;
            for (int col = 0; col < entries.size(); col++) {
                InsightEntry ie = entries.get(col);
                int x = col * colPitch;
                nodePositions.put(ie.id(), new int[]{x, y});

                InsightData idata = ClientData.insight(ie.id());
                InsightNodeElement.State ns;
                boolean unlocked = state != null && state.unlockedInsights().contains(ie.id());
                if (unlocked) {
                    ns = InsightNodeElement.State.UNLOCKED;
                } else if (interactive && InsightTreeResolver.canUnlock(
                        ClientData.clientData(), moduleId, module, ie.id())) {
                    ns = InsightNodeElement.State.CAN_UNLOCK;
                } else {
                    ns = InsightNodeElement.State.LOCKED;
                }
                InsightNodeElement node = new InsightNodeElement(
                        ie.id(), moduleId, idata != null ? idata : defaultInsight(ie.id()),
                        ns, clickHandler, interactive);
                node.layout(l -> {
                    l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE);
                    l.left(x).top(y);
                    l.width(NODE_SIZE).height(NODE_SIZE);
                });
                content.addChild(node);
            }
        }

        // Lines: each ancestor → child (parent bottom-center → midY → child top-center).
        for (var ie : insights) {
            var ancestors = InsightTreeResolver.findAncestors(module, ie.id());
            var childPos = nodePositions.get(ie.id());
            if (childPos == null) continue;
            int cCX = childPos[0] + NODE_SIZE / 2;
            int cTY = childPos[1];
            for (var ancestorId : ancestors) {
                var parentPos = nodePositions.get(ancestorId);
                if (parentPos == null) continue;
                int pCX = parentPos[0] + NODE_SIZE / 2;
                int pBY = parentPos[1] + NODE_SIZE;
                int midY = pBY + (cTY - pBY) / 2;
                addLine(content, pCX, pBY, 1, Math.max(1, midY - pBY));
                int hs = Math.min(pCX, cCX);
                int he = Math.max(pCX, cCX);
                if (he > hs) addLine(content, hs, midY, he - hs, 1);
                addLine(content, cCX, midY, 1, Math.max(1, cTY - midY));
            }
        }

        // Center content in container.
        container.addChild(content);
        final boolean[] centered = {false};
        final int[] tc = {0};
        container.addEventListener(UIEvents.TICK, e -> {
            if (centered[0] || tc[0]++ > 10) return;
            float cw = container.getSizeWidth();
            float ch = container.getSizeHeight();
            if (cw > 0 && ch > 0) {
                float ox = Math.max(0, (cw - contentWidth) / 2f);
                float oy = Math.max(0, (ch - contentHeight) / 2f);
                content.layout(l -> l.left(ox).top(oy));
                centered[0] = true;
            }
        });
        container.addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
            float cw = container.getSizeWidth();
            float ch = container.getSizeHeight();
            if (cw > 0 && ch > 0) {
                float ox = Math.max(0, (cw - contentWidth) / 2f);
                float oy = Math.max(0, (ch - contentHeight) / 2f);
                content.layout(l -> l.left(ox).top(oy));
            }
        });
    }

    private static void addLine(UIElement content, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        InsightLineElement line = new InsightLineElement(LINE_COLOR);
        line.layout(l -> {
            l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE);
            l.left(x).top(y);
            l.width(w).height(h);
        });
        content.addChild(line);
    }

    private static InsightData defaultInsight(ResourceLocation id) {
        // Fallback when insight data is null (registry not synced to client yet).
        return new InsightData(
                java.util.Optional.of(net.minecraft.network.chat.Component.literal(id.toString())),
                java.util.Optional.empty(), java.util.Optional.empty(),
                1, java.util.Optional.empty(), java.util.Optional.empty(), 100);
    }
}
