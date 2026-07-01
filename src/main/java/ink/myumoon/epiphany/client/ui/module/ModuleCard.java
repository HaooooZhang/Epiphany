package ink.myumoon.epiphany.client.ui.module;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.content.InsightEntry;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Card for a single selected Module: header (icon + name) + embedded
 * {@link InsightTreeView}.
 * <p>
 * Phase 3 MVP: header shows the module's name (fallback to its registry id if
 * the datapack didn't define one). The Insight tree is a simple list-grouped
 * view by depth; full connector rendering can be layered on Phase 3.5.
 */
public final class ModuleCard {

    private ModuleCard() {
    }

    public static UIElement create(ResourceLocation moduleId,
                                   ModuleData module,
                                   ModulePlayerState state) {
        var card = new UIElement();
        card.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(180).height(200)
                .paddingAll(4).gapAll(2));
        // Light grey background so the card stands out of the scroller.
        card.style(s -> s.background(new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(0xFF2B2B2B)));

        card.addChild(buildHeader(moduleId, module));
        List<InsightEntry> entries = module != null ? module.insights() : List.of();
        card.addChild(InsightTreeView.create(moduleId, entries, state));

        // Phase 6: hover tooltip with Shift-aware reward description.
        ink.myumoon.epiphany.client.ui.tooltips.TooltipBuilder.attach(card, () -> {
            Component header = (module != null && module.name().isPresent())
                    ? module.name().get()
                    : Component.literal(moduleId.toString());
            return ink.myumoon.epiphany.client.ui.tooltips.TooltipBuilder.forContent(
                    header,
                    module != null ? module.description() : java.util.Optional.empty(),
                    module != null ? module.onSelectRewardDescription() : java.util.Optional.empty(),
                    module != null ? module.onCompleteRewardDescription() : java.util.Optional.empty());
        });
        return card;
    }

    private static UIElement buildHeader(ResourceLocation moduleId, ModuleData module) {
        var header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .widthPercent(100).height(20));

        Component name = module != null && module.name().isPresent()
                ? module.name().get()
                : Component.literal(moduleId.toString());
        header.addChild(new Label().setText(name));
        return header;
    }
}
