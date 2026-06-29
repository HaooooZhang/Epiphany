package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after an Epiphany has been selected.
 */
public class EpiphanySelectedEvent extends EpiphanyEvent {

    private final ResourceLocation epiphanyId;

    public EpiphanySelectedEvent(ServerPlayer player, ResourceLocation epiphanyId) {
        super(player);
        this.epiphanyId = epiphanyId;
    }

    public ResourceLocation getEpiphanyId() { return epiphanyId; }
}
