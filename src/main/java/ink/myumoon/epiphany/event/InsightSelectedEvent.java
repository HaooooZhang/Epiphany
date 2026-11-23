package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after an Insight has been unlocked.
 */
public class InsightSelectedEvent extends EpiphanyEvent {

    private final ResourceLocation insightId;
    private final ResourceLocation moduleId;

    public InsightSelectedEvent(ServerPlayer player, ResourceLocation insightId, ResourceLocation moduleId) {
        super(player);
        this.insightId = insightId;
        this.moduleId = moduleId;
    }

    public ResourceLocation getInsightId() { return insightId; }
    public ResourceLocation getModuleId() { return moduleId; }
}
