package ink.myumoon.epiphany.client.ui.insight;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.util.UISoundUtils;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import ink.myumoon.epiphany.client.EpiphanyIcons;
import ink.myumoon.epiphany.client.ui.ItemIconElement;
import ink.myumoon.epiphany.content.InsightData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

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

        // Icon: custom texture as child (preserves border from LSS).
        boolean hasCustomIcon = InsightIcons.iconTexture(insightData, insightId)
                .filter(InsightIcons::resourceExists)
                .map(rl -> {
                    var icon = new UIElement();
                    icon.layout(l -> l.width(16).height(16));
                    icon.style(s -> s.background(SpriteTexture.of(rl)));
                    addChild(icon);
                    return true;
                })
                .orElse(false);

        if (!hasCustomIcon) {
            var iconChild = new ItemIconElement(EpiphanyIcons.defaultInsight());
            iconChild.layout(l -> l.width(16).height(16));
            addChild(iconChild);
        }

        // tooltip
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);

        // Click: client-side sound + direct RPC to server.
        if (interactive && clickHandler != null && state == State.CAN_UNLOCK) {
            addEventListener(UIEvents.MOUSE_DOWN, e -> {
                UISoundUtils.playButtonClickSound();
                RPCPacketDistributor.rpcToServer(
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
        lines.add(Component.literal(name).withStyle(ChatFormatting.WHITE));
        // Description.
        if (insightData.description().isPresent()) {
            lines.add(insightData.description().get().copy()
                    .withStyle(ChatFormatting.GRAY));
        }
        if (Screen.hasShiftDown()) {
            if (insightData.rewardDescription().isPresent()) {
                lines.add(Component.translatable("epiphany.tooltip.reward")
                        .append(": ")
                        .append(insightData.rewardDescription().get())
                        .withStyle(ChatFormatting.GOLD));
            }
        } else {
            if (insightData.rewardDescription().isPresent()) {
                lines.add(Component.translatable("epiphany.ui.shift_hint")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
        // Cost.
        lines.add(Component.translatable("epiphany.ui.insight_cost", insightData.cost())
                .withStyle(ChatFormatting.AQUA));
        event.hoverTooltips = HoverTooltips.empty();
        for (var line : lines) {
            event.hoverTooltips = event.hoverTooltips.append(line);
        }
    }
}
