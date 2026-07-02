package ink.myumoon.epiphany.client.ui.insight;

import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Callback for when an Insight node is clicked and should be unlocked server-side.
 */
@FunctionalInterface
public interface InsightClickHandler {
    /**
     * Called when a node is clicked. The handler should invoke
     * {@code InsightManager.select(sp, insightId, moduleId)} on the server.
     *
     * @param serverPlayer the player who clicked (from UIEvent）
     * @param insightId    the insight being clicked
     * @param moduleId     the parent module's registry id
     */
    void onClick(ServerPlayer serverPlayer,
                 net.minecraft.resources.ResourceLocation insightId,
                 net.minecraft.resources.ResourceLocation moduleId);
}
