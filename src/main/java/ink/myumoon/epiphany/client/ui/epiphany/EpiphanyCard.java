package ink.myumoon.epiphany.client.ui.epiphany;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.PathData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Card for a single selected Epiphany.
 * <p>
 * Phase 4 MVP: title (name) + path tag (if any). Hover tooltip and
 * reward-description tooltips are deferred to Phase 5.
 */
public final class EpiphanyCard {

    private EpiphanyCard() {
    }

    public static UIElement create(ResourceLocation epiphanyId, EpiphanyData epiphany) {
        var card = new UIElement();
        card.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(120).height(80)
                .paddingAll(4).gapAll(2));
        card.style(s -> s.background(new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(0xFF2B2B2B)));

        // Title: epiphany name or id fallback.
        Component title = (epiphany != null && epiphany.name().isPresent())
                ? epiphany.name().get()
                : Component.literal(epiphanyId.toString());
        card.addChild(new Label().setText(title));

        // Path tag (if the epiphany declares a path and it resolves).
        if (epiphany != null && epiphany.path().isPresent()) {
            PathData path = ClientData.path(epiphany.path().get());
            if (path != null && path.name().isPresent()) {
                card.addChild(new Label().setText(path.name().get()));
            }
        }

        // Phase 6: hover tooltip with Shift-aware reward description.
        ink.myumoon.epiphany.client.ui.tooltips.TooltipBuilder.attach(card, () -> {
            Component header = (epiphany != null && epiphany.name().isPresent())
                    ? epiphany.name().get()
                    : Component.literal(epiphanyId.toString());
            return ink.myumoon.epiphany.client.ui.tooltips.TooltipBuilder.forContent(
                    header,
                    epiphany != null ? epiphany.description() : java.util.Optional.empty(),
                    epiphany != null ? epiphany.rewardDescription() : java.util.Optional.empty(),
                    java.util.Optional.empty());  // epiphany has no completion reward
        });
        return card;
    }
}
