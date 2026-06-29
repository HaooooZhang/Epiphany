package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after a Module has been selected.
 */
public class ModuleSelectedEvent extends EpiphanyEvent {

    private final ResourceLocation moduleId;

    public ModuleSelectedEvent(ServerPlayer player, ResourceLocation moduleId) {
        super(player);
        this.moduleId = moduleId;
    }

    public ResourceLocation getModuleId() { return moduleId; }
}
