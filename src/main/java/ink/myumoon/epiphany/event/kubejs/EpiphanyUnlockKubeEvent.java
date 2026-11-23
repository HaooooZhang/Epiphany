package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.EpiphanyUnlockEvent} (Pre, cancellable).
 */
public class EpiphanyUnlockKubeEvent extends EventJS {

    private final ServerPlayer player;
    private final ResourceLocation epiphanyId;

    public EpiphanyUnlockKubeEvent(ServerPlayer player, ResourceLocation epiphanyId) {
        this.player = player;
        this.epiphanyId = epiphanyId;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getEpiphanyId() { return epiphanyId; }
}
