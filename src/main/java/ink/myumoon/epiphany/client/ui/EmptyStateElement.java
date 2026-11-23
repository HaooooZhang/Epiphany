package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Shared centered placeholder for selection lists with no candidates. */
public final class EmptyStateElement {

    private static final ResourceLocation NOT_FOUND_ICON =
            ResourceLocation.fromNamespaceAndPath("epiphany", "ui/not_found.png");

    private EmptyStateElement() {
    }

    public static UIElement create() {
        UIElement state = new UIElement();
        state.addClass("popup-empty-state");

        UIElement icon = new UIElement();
        icon.style(style -> style.background(SpriteTexture.of(NOT_FOUND_ICON)));
        icon.layout(layout -> layout.width(24).height(24).flexShrink(0));
        state.addChild(icon);

        Label message = new Label();
        message.setText(Component.translatable("epiphany.ui.none_available"));
        message.textStyle(style -> style.fontSize(8).textColor(0xFFAAAAAA).textShadow(true)
                .textAlignHorizontal(Horizontal.CENTER));
        message.layout(layout -> layout.widthPercent(100).flexShrink(0));
        state.addChild(message);

        return state;
    }
}
