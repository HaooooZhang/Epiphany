package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired before a Module is selected by the player.
 * Cancelling prevents Insight Point deduction and selection.
 */
public class ModuleSelectEvent extends EpiphanyEvent implements ICancellableEvent {

    private final ResourceLocation moduleId;

    public ModuleSelectEvent(ServerPlayer player, ResourceLocation moduleId) {
        super(player);
        this.moduleId = moduleId;
    }

    public ResourceLocation getModuleId() { return moduleId; }
}
