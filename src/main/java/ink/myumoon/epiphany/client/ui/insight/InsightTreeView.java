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

// Insight Tree Renderer
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
        int contentWidth = (maxPerDepth - 1) * colPitch + NODE_SIZE;  // exactly node bounds
        int contentHeight = (maxDepth + 1) * rowPitch;

        // Inner content wrapper.
        UIElement content = new UIElement();
        content.layout(l -> {
            l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE);
            l.left(0).top(0);
            l.width(contentWidth).height(contentHeight);
        });

        // compute node positions.
        Map<ResourceLocation, int[]> nodePositions = new HashMap<>();

        for (var depthEntry : byDepth.entrySet()) {
            int depth = depthEntry.getKey();
            var entries = depthEntry.getValue();
            int y = depth * rowPitch;
            for (int col = 0; col < entries.size(); col++) {
                InsightEntry ie = entries.get(col);
                int x = col * colPitch;
                nodePositions.put(ie.id(), new int[]{x, y});
            }
        }

        // draw lines
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

        // draw nodes
        for (var depthEntry : byDepth.entrySet()) {
            int depth = depthEntry.getKey();
            var entries = depthEntry.getValue();
            int y = depth * rowPitch;
            for (int col = 0; col < entries.size(); col++) {
                InsightEntry ie = entries.get(col);
                int x = col * colPitch;

                InsightData idata = ClientData.insight(ie.id());
                InsightNodeElement.State ns;
                boolean unlocked = state != null && state.unlockedInsights().contains(ie.id());
                if (unlocked) {
                    ns = InsightNodeElement.State.UNLOCKED;
                } else if (interactive && InsightTreeResolver.canUnlock(
                        Objects.requireNonNull(ClientData.clientData()), moduleId, module, ie.id())) {
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

        // Large Tree Handle
        boolean largeTree = maxDepth > 3 || maxPerDepth > 3;
        container.addChild(content);
        if (largeTree) {
            addDragPan(container, content, contentWidth, contentHeight);
        } else {
            container.addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
                float cw = container.getSizeWidth();
                float ch = container.getSizeHeight();
                if (cw > 0 && ch > 0) {
                    float ox = Math.max(0, (cw - contentWidth) / 2f);
                    float oy = Math.max(4, (ch - contentHeight) / 2f);
                    content.layout(l -> l.left(ox).top(oy));
                }
            });
        }
    }

    // mouse control
    private static void addDragPan(UIElement container, UIElement content,
                                    int cw, int ch) {
        final float[] ox = {0}, oy = {0};        // current content offset
        final boolean[] centered = {false};
        final boolean[] dragging = {false};
        final float[] dragStartCX = {0}, dragStartCY = {0};  // content pos at drag start
        final double[] dragStartMX = {0}, dragStartMY = {0}; // mouse pos at drag start

        // Initial centering once layout is ready. Content starts at 0 if overflowed, else centered.
        container.addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
            if (centered[0]) return;
            float ccw = container.getSizeWidth();
            float cch = container.getSizeHeight();
            if (ccw > 0 && cch > 0) {
                ox[0] = cw > ccw ? 0 : Math.max(0, (ccw - cw) / 2f);
                oy[0] = ch > cch ? 0 : Math.max(4, (cch - ch) / 2f);
                content.layout(l -> l.left(ox[0]).top(oy[0]));
                centered[0] = true;
            }
        });

        container.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (!centered[0]) return;
            dragging[0] = true;
            dragStartCX[0] = ox[0];
            dragStartCY[0] = oy[0];
            dragStartMX[0] = net.minecraft.client.Minecraft.getInstance().mouseHandler.xpos();
            dragStartMY[0] = net.minecraft.client.Minecraft.getInstance().mouseHandler.ypos();
        });

        container.addEventListener(UIEvents.MOUSE_UP, e -> dragging[0] = false);

        container.addEventListener(UIEvents.TICK, e -> {
            if (!dragging[0]) return;
            double mx = net.minecraft.client.Minecraft.getInstance().mouseHandler.xpos();
            double my = net.minecraft.client.Minecraft.getInstance().mouseHandler.ypos();
            float dx = (float)(mx - dragStartMX[0]);
            float dy = (float)(my - dragStartMY[0]);
            float ccw = container.getSizeWidth();
            float cch = container.getSizeHeight();
            if (cw > ccw) ox[0] = Math.min(0, Math.max(ccw - cw, dragStartCX[0] + dx));
            if (ch > cch) oy[0] = Math.min(0, Math.max(cch - ch, dragStartCY[0] + dy));
            content.layout(l -> l.left(ox[0]).top(oy[0]));
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
        // Fallback when insight data is null
        return new InsightData(
                java.util.Optional.of(net.minecraft.network.chat.Component.literal(id.toString())),
                java.util.Optional.empty(), java.util.Optional.empty(),
                1, java.util.Optional.empty(), java.util.Optional.empty(), 100);
    }
}
