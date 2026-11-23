package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.InsightSelectedEvent} (Post, notification).
 */
public class InsightSelectedKubeEvent extends EventJS {

    private final ServerPlayer player;
    private final ResourceLocation insightId;
    private final ResourceLocation moduleId;

    public InsightSelectedKubeEvent(ServerPlayer player, ResourceLocation insightId, ResourceLocation moduleId) {
        this.player = player;
        this.insightId = insightId;
        this.moduleId = moduleId;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getInsightId() { return insightId; }
    public ResourceLocation getModuleId() { return moduleId; }
}
