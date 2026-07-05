package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired before a Module is completed (all Insights unlocked).
 * Cancelling prevents the Epiphany slot grant, reward, and the Post event.
 * The Module stays in an "all Insights unlocked, not completed" state
 * and can be re-triggered later.
 */
public class ModuleCompleteEvent extends EpiphanyEvent implements ICancellableEvent {

    private final ResourceLocation moduleId;

    public ModuleCompleteEvent(ServerPlayer player, ResourceLocation moduleId) {
        super(player);
        this.moduleId = moduleId;
    }

    public ResourceLocation getModuleId() { return moduleId; }
}
