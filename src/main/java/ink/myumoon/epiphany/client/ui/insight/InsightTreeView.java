package ink.myumoon.epiphany.client.ui.insight;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.TaffyPosition;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.InsightEntry;
import ink.myumoon.epiphany.content.InsightTreeResolver;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.*;

// Insight Tree Renderer
public class InsightTreeView {

    private static final int NODE_SIZE = 20;
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
        int rowPitch = colPitch; // same spacing for both axes
        int contentWidth = (maxPerDepth - 1) * colPitch + NODE_SIZE;  // exactly node bounds
        int contentHeight = (maxDepth + 1) * rowPitch;

        // Inner content wrapper.
        UIElement content = new UIElement();
        content.layout(l -> {
            l.positionType(TaffyPosition.ABSOLUTE);
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
                // Center single-node depths; left-align multi-node depths.
                int x = entries.size() == 1
                        ? (contentWidth - NODE_SIZE) / 2
                        : col * colPitch;
                nodePositions.put(ie.id(), new int[]{x, y});
            }
        }

        // draw lines: connect to ALL nodes at the closest lower depth (AND semantics)
        for (var ie : insights) {
            int targetDepth = ie.depth();
            if (targetDepth <= 0) continue;
            // Find the closest lower depth that has nodes.
            List<InsightEntry> parentEntries = null;
            for (int d = targetDepth - 1; d >= 0; d--) {
                parentEntries = byDepth.get(d);
                if (parentEntries != null && !parentEntries.isEmpty()) break;
                parentEntries = null;
            }
            if (parentEntries == null) continue;
            var childPos = nodePositions.get(ie.id());
            if (childPos == null) continue;
            int cCX = childPos[0] + NODE_SIZE / 2;
            int cTY = childPos[1];
            for (var parentEntry : parentEntries) {
                var parentPos = nodePositions.get(parentEntry.id());
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
                int x = entries.size() == 1
                        ? (contentWidth - NODE_SIZE) / 2
                        : col * colPitch;

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

        // Large Tree: enable drag when content likely exceeds card bounds.
        // Vertical: 4+ depths (≥120px > 86px container). Horizontal: 5+ per depth (≥110px).
        boolean largeTree = maxDepth > 2 || maxPerDepth > 3;
        container.addChild(content);
        if (largeTree) {
            addDragPan(container, content, contentWidth, contentHeight);
        } else {
            container.addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
                float cw = container.getSizeWidth();
                float ch = container.getSizeHeight();
                if (cw > 0 && ch > 0) {
                    float ox = Math.max(0, (cw - contentWidth) / 2f);
                    float oy = Math.max(2, (ch - contentHeight) / 2f);
                    content.layout(l -> l.left(ox).top(oy));
                }
            });
        }
    }

    // Drag pan: MOUSE_DOWN/UP for state, TICK for movement with GLFW mouse.
    private static void addDragPan(UIElement container, UIElement content,
                                    int cw, int ch) {
        final float[] ox = {0}, oy = {0};
        final boolean[] centered = {false}, dragging = {false}, moved = {false};
        final float[] dragStartCX = {0}, dragStartCY = {0};
        final double[] dragStartMX = {0}, dragStartMY = {0};
        final int MIN_DRAG = 3;

        container.addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
            if (centered[0]) return;
            float ccw = container.getSizeWidth();
            float cch = container.getSizeHeight();
            if (ccw > 0 && cch > 0) {
                ox[0] = cw > ccw ? 0 : Math.max(0, (ccw - cw) / 2f);
                oy[0] = ch > cch ? 0 : Math.max(2, (cch - ch) / 2f);
                content.layout(l -> l.left(ox[0]).top(oy[0]));
                centered[0] = true;
            }
        });

        container.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            dragging[0] = true;
            moved[0] = false;
            dragStartCX[0] = ox[0];
            dragStartCY[0] = oy[0];
            var mc = Minecraft.getInstance();
            dragStartMX[0] = mc.mouseHandler.xpos();
            dragStartMY[0] = mc.mouseHandler.ypos();
        });

        container.addEventListener(UIEvents.MOUSE_UP, e -> dragging[0] = false);

        container.addEventListener(UIEvents.TICK, e -> {
            if (!dragging[0]) return;
            var mc = Minecraft.getInstance();
            long win = mc.getWindow().getWindow();
            if (GLFW.glfwGetMouseButton(
                    win, GLFW.GLFW_MOUSE_BUTTON_1)
                    != GLFW.GLFW_PRESS) {
                dragging[0] = false;
                return;
            }
            double scale = mc.getWindow().getGuiScale();
            double mx = mc.mouseHandler.xpos() / scale;
            double my = mc.mouseHandler.ypos() / scale;
            float dx = (float)(mx - dragStartMX[0] / scale);
            float dy = (float)(my - dragStartMY[0] / scale);
            if (!moved[0] && Math.abs(dx) < MIN_DRAG && Math.abs(dy) < MIN_DRAG) return;
            moved[0] = true;
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
            l.positionType(TaffyPosition.ABSOLUTE);
            l.left(x).top(y);
            l.width(w).height(h);
        });
        content.addChild(line);
    }

    private static InsightData defaultInsight(ResourceLocation id) {
        // Fallback when insight data is null
        return new InsightData(
                Optional.of(Component.literal(id.toString())),
                Optional.empty(), Optional.empty(),
                1, Optional.empty(), Optional.empty(), 100);
    }
}
