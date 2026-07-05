package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired before a Module transitions from locked to unlocked.
 * Cancelling prevents to unlock.
 */
public class ModuleUnlockEvent extends EpiphanyEvent implements ICancellableEvent {

    private final ResourceLocation moduleId;

    public ModuleUnlockEvent(ServerPlayer player, ResourceLocation moduleId) {
        super(player);
        this.moduleId = moduleId;
    }

    public ResourceLocation getModuleId() { return moduleId; }
}
