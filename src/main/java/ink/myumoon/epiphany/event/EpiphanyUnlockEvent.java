package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired before an Epiphany transitions from locked to unlocked.
 * Cancelling prevents to unlock.
 */
public class EpiphanyUnlockEvent extends EpiphanyEvent implements ICancellableEvent {

    private final ResourceLocation epiphanyId;

    public EpiphanyUnlockEvent(ServerPlayer player, ResourceLocation epiphanyId) {
        super(player);
        this.epiphanyId = epiphanyId;
    }

    public ResourceLocation getEpiphanyId() { return epiphanyId; }
}
