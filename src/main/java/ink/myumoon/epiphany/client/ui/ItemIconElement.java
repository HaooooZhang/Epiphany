package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import net.minecraft.world.item.ItemStack;

// Lightweight UIElement that renders a vanilla ItemStack as its background.
public class ItemIconElement extends UIElement {

    private final ItemStack stack;

    public ItemIconElement(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        float x = getPositionX();
        float y = getPositionY();
        float w = getSizeWidth();
        float h = getSizeHeight();
        if (w <= 0 || h <= 0) return;

        guiContext.pose.pushPose();
        guiContext.pose.translate(x, y, 0);
        // 16 * 16
        guiContext.pose.scale(w / 16f, h / 16f, 1);
        if (!stack.isEmpty()) {
            DrawerHelper.drawItemStack(guiContext.graphics, stack, 0, 0,
                    guiContext.elementColor, null);
        }
        guiContext.pose.popPose();
    }
}
