package ink.myumoon.epiphany.client.ui.insight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Callback for when an Insight is clicked and should be unlocked server-side.
 */
@FunctionalInterface
public interface InsightClickHandler {
    /**
     * Called when an insight is clicked. The handler should invoke
     * {@code InsightManager.select(sp, insightId, moduleId)} on the server.
     *
     * @param serverPlayer the player who clicked (from UIEvent）
     * @param insightId    the insight being clicked
     * @param moduleId     the parent module's registry id
     */
    void onClick(ServerPlayer serverPlayer,
                 ResourceLocation insightId,
                 ResourceLocation moduleId);
}
