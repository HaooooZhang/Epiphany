package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Lightweight UIElement that renders a vanilla ItemStack as its background.
 * <p>
 * Unlike LDLib2's {@code ItemSlot}, this does NOT bind to a container menu and
 * does NOT participate in JEI/EMI drag — it's purely visual. Useful for showing
 * item icons as static placeholders in Epiphany cards before real PNG textures ship.
 */
public class ItemIconElement extends UIElement {

    private final ItemStack stack;

    public ItemIconElement(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        // Use getPositionX/Y + getSizeWidth/Height (always populated after first layout)
        // instead of getContentX/Y (which can be 0 if no border/padding).
        float x = getPositionX();
        float y = getPositionY();
        float w = getSizeWidth();
        float h = getSizeHeight();
        if (w <= 0 || h <= 0) return;

        guiContext.pose.pushPose();
        guiContext.pose.translate(x, y, 0);
        // Scale the 16x16 item icon to fit the element box.
        guiContext.pose.scale(w / 16f, h / 16f, 1);
        if (!stack.isEmpty()) {
            DrawerHelper.drawItemStack(guiContext.graphics, stack, 0, 0,
                    guiContext.elementColor, null);
        }
        guiContext.pose.popPose();
    }
}
