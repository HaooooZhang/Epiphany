package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Fired before an Epiphany is selected (slotted).
 * Cancelling prevents slot usage and the reward.
 */
@Cancelable
public class EpiphanySelectEvent extends EpiphanyEvent {

    private final ResourceLocation epiphanyId;

    public EpiphanySelectEvent(ServerPlayer player, ResourceLocation epiphanyId) {
        super(player);
        this.epiphanyId = epiphanyId;
    }

    public ResourceLocation getEpiphanyId() { return epiphanyId; }
}
