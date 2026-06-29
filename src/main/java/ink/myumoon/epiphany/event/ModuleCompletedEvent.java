package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after a Module has been completed.
 */
public class ModuleCompletedEvent extends EpiphanyEvent {

    private final ResourceLocation moduleId;

    public ModuleCompletedEvent(ServerPlayer player, ResourceLocation moduleId) {
        super(player);
        this.moduleId = moduleId;
    }

    public ResourceLocation getModuleId() { return moduleId; }
}
