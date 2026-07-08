package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after an Epiphany has been unlocked.
 */
public class EpiphanyUnlockedEvent extends EpiphanyEvent {

    private final ResourceLocation epiphanyId;
    private final boolean silent;

    public EpiphanyUnlockedEvent(ServerPlayer player, ResourceLocation epiphanyId) {
        this(player, epiphanyId, false);
    }

    public EpiphanyUnlockedEvent(ServerPlayer player, ResourceLocation epiphanyId, boolean silent) {
        super(player);
        this.epiphanyId = epiphanyId;
        this.silent = silent;
    }

    public ResourceLocation getEpiphanyId() { return epiphanyId; }
    public boolean isSilent() { return silent; }
}
