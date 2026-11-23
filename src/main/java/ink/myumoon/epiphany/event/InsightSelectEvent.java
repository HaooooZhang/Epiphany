package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Fired before an Insight is unlocked (Insight Points are about to be deducted).
 * Cancelling prevents to unlock.
 */
@Cancelable
public class InsightSelectEvent extends EpiphanyEvent {

    private final ResourceLocation insightId;
    private final ResourceLocation moduleId;

    public InsightSelectEvent(ServerPlayer player, ResourceLocation insightId, ResourceLocation moduleId) {
        super(player);
        this.insightId = insightId;
        this.moduleId = moduleId;
    }

    public ResourceLocation getInsightId() { return insightId; }
    public ResourceLocation getModuleId() { return moduleId; }
}
