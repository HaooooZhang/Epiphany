package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.ModuleUnlockEvent} (Pre, cancellable).
 */
public class ModuleUnlockKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final ResourceLocation moduleId;

    public ModuleUnlockKubeEvent(ServerPlayer player, ResourceLocation moduleId) {
        this.player = player;
        this.moduleId = moduleId;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getModuleId() { return moduleId; }
}
