package ink.myumoon.epiphany.client.ui.insight;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.ItemIconElement;
import ink.myumoon.epiphany.content.InsightData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A single Insight node in the tree. 18×18 px square.
 * <p>
 * Visual states (via CSS classes):
 * - unlocked: BORDER1_RT0 + icon
 * - locked: BORDER1_RT0_DARK + icon
 * - can-unlock: border highlight
 * <p>
 * Click → server-side InsightManager.select via InsightClickHandler.
 * Hover → Tooltip with name + description + optional reward (Shift).
 */
public class InsightNodeElement extends UIElement {

    public enum State { UNLOCKED, LOCKED, CAN_UNLOCK }

    private final ResourceLocation insightId;
    private final ResourceLocation moduleId;
    private final InsightData insightData;
    private final State state;
    private final InsightClickHandler clickHandler;
    private final boolean interactive;

    public InsightNodeElement(ResourceLocation insightId, ResourceLocation moduleId,
                               InsightData insightData, State state,
                               InsightClickHandler clickHandler, boolean interactive) {
        this.insightId = insightId;
        this.moduleId = moduleId;
        this.insightData = insightData;
        this.state = state;
        this.clickHandler = clickHandler;
        this.interactive = interactive;

        // Apply LSS class.
        addClass("insight-node");
        switch (state) {
            case UNLOCKED -> addClass("insight-node-unlocked");
            case LOCKED -> addClass("insight-node-locked");
            case CAN_UNLOCK -> addClass("insight-node-can-unlock");
        }

        // Add icon (16×16) centered inside the 18×18 node.
        InsightIcons.iconTexture(insightData, insightId)
                .ifPresent(rl -> {
                    if (InsightIcons.resourceExists(rl)) {
                        addChild(new com.lowdragmc.lowdraglib2.gui.ui.UIElement()
                                .layout(l -> l.widthPercent(100).heightPercent(100)));
                    }
                });
        // Fallback: default insight item icon.
        var iconChild = new ItemIconElement(ink.myumoon.epiphany.client.EpiphanyIcons.defaultInsight());
        iconChild.layout(l -> l.width(16).height(16));
        addChild(iconChild);

        // Hover tooltip.
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);

        // Click: client-side sound + direct RPC to server (bypass LDLib2 dynamic-element issue).
        if (interactive && clickHandler != null && state == State.CAN_UNLOCK) {
            addEventListener(UIEvents.MOUSE_DOWN, e -> {
                com.lowdragmc.lowdraglib2.gui.util.UISoundUtils.playButtonClickSound();
                com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor.rpcToServer(
                        "epiphany.select_insight", insightId.toString(), moduleId.toString());
            });
        }
    }

    private void onHoverTooltips(UIEvent event) {
        List<Component> lines = new ArrayList<>();
        // Name.
        String name = insightData.name().isPresent()
                ? insightData.name().get().getString()
                : insightId.toString();
        lines.add(Component.literal(name).withStyle(net.minecraft.ChatFormatting.WHITE));
        // Description.
        if (insightData.description().isPresent()) {
            lines.add(insightData.description().get().copy()
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        // Shift → reward description.
        if (Screen.hasShiftDown()) {
            if (insightData.rewardDescription().isPresent()) {
                lines.add(Component.translatable("epiphany.tooltip.reward")
                        .append(": ")
                        .append(insightData.rewardDescription().get())
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
            }
        } else {
            if (insightData.rewardDescription().isPresent()) {
                lines.add(Component.translatable("epiphany.ui.shift_hint")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
            }
        }
        // Cost.
        lines.add(Component.translatable("epiphany.ui.insight_cost", insightData.cost())
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        event.hoverTooltips = HoverTooltips.empty();
        for (var line : lines) {
            event.hoverTooltips = event.hoverTooltips.append(line);
        }
    }
}
