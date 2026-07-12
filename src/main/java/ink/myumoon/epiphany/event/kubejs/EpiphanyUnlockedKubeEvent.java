package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.EpiphanyUnlockedEvent} (Post, notification).
 */
public class EpiphanyUnlockedKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final ResourceLocation epiphanyId;
    private final boolean silent;

    public EpiphanyUnlockedKubeEvent(ServerPlayer player, ResourceLocation epiphanyId, boolean silent) {
        this.player = player;
        this.epiphanyId = epiphanyId;
        this.silent = silent;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getEpiphanyId() { return epiphanyId; }

    /** True if the unlock was triggered by auto-unlock (checkAutoUnlock) or reset. */
    public boolean isSilent() { return silent; }
}
