package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.ModuleCompleteEvent} (Pre, cancellable).
 */
public class ModuleCompleteKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final ResourceLocation moduleId;

    public ModuleCompleteKubeEvent(ServerPlayer player, ResourceLocation moduleId) {
        this.player = player;
        this.moduleId = moduleId;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getModuleId() { return moduleId; }
}
