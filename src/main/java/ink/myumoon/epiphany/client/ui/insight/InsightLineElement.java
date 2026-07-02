package ink.myumoon.epiphany.client.ui.insight;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

/**
 * Renders a thin line segment (horizontal or vertical) for connecting Insight nodes.
 * Uses ColorRectTexture via drawBackgroundAdditional for pixel-precise rendering.
 */
public class InsightLineElement extends UIElement {

    private final int color;

    public InsightLineElement(int color) {
        this.color = color;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        float x = getPositionX();
        float y = getPositionY();
        float w = getSizeWidth();
        float h = getSizeHeight();
        if (w <= 0 || h <= 0) return;
        guiContext.drawTexture(new ColorRectTexture(color), x, y, w, h);
    }
}
