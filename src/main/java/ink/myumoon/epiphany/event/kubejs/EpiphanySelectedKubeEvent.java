package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.EpiphanySelectedEvent} (Post, notification).
 */
public class EpiphanySelectedKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final ResourceLocation epiphanyId;

    public EpiphanySelectedKubeEvent(ServerPlayer player, ResourceLocation epiphanyId) {
        this.player = player;
        this.epiphanyId = epiphanyId;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getEpiphanyId() { return epiphanyId; }
}
