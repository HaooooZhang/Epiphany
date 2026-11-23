package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Fired before a Module is selected by the player.
 * Cancelling prevents Insight Point deduction and selection.
 */
@Cancelable
public class ModuleSelectEvent extends EpiphanyEvent {

    private final ResourceLocation moduleId;

    public ModuleSelectEvent(ServerPlayer player, ResourceLocation moduleId) {
        super(player);
        this.moduleId = moduleId;
    }

    public ResourceLocation getModuleId() { return moduleId; }
}
