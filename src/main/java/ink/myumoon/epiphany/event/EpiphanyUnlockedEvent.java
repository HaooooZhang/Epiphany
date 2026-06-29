package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after an Epiphany has been unlocked.
 */
public class EpiphanyUnlockedEvent extends EpiphanyEvent {

    private final ResourceLocation epiphanyId;

    public EpiphanyUnlockedEvent(ServerPlayer player, ResourceLocation epiphanyId) {
        super(player);
        this.epiphanyId = epiphanyId;
    }

    public ResourceLocation getEpiphanyId() { return epiphanyId; }
}
