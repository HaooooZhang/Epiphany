package ink.myumoon.epiphany.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after a Module has been unlocked.
 */
public class ModuleUnlockedEvent extends EpiphanyEvent {

    private final ResourceLocation moduleId;
    private final boolean silent;

    public ModuleUnlockedEvent(ServerPlayer player, ResourceLocation moduleId) {
        this(player, moduleId, false);
    }

    public ModuleUnlockedEvent(ServerPlayer player, ResourceLocation moduleId, boolean silent) {
        super(player);
        this.moduleId = moduleId;
        this.silent = silent;
    }

    public ResourceLocation getModuleId() { return moduleId; }
    public boolean isSilent() { return silent; }
}
