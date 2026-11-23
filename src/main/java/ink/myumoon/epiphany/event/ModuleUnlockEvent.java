package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Fired before a Module transitions from locked to unlocked.
 * Cancelling prevents to unlock.
 */
@Cancelable
public class ModuleUnlockEvent extends EpiphanyEvent {

    private final ResourceLocation moduleId;

    public ModuleUnlockEvent(ServerPlayer player, ResourceLocation moduleId) {
        super(player);
        this.moduleId = moduleId;
    }

    public ResourceLocation getModuleId() { return moduleId; }
}
