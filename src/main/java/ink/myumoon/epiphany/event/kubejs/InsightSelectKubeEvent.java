package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.InsightSelectEvent} (Pre, cancellable).
 */
public class InsightSelectKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final ResourceLocation insightId;
    private final ResourceLocation moduleId;

    public InsightSelectKubeEvent(ServerPlayer player, ResourceLocation insightId, ResourceLocation moduleId) {
        this.player = player;
        this.insightId = insightId;
        this.moduleId = moduleId;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getInsightId() { return insightId; }
    public ResourceLocation getModuleId() { return moduleId; }
}
