package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.ModuleUnlockedEvent} (Post, notification).
 */
public class ModuleUnlockedKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final ResourceLocation moduleId;
    private final boolean silent;

    public ModuleUnlockedKubeEvent(ServerPlayer player, ResourceLocation moduleId, boolean silent) {
        this.player = player;
        this.moduleId = moduleId;
        this.silent = silent;
    }

    public ServerPlayer getPlayer() { return player; }
    public ResourceLocation getModuleId() { return moduleId; }

    /** True if the unlock was triggered by auto-unlock (checkAutoUnlock) or reset. */
    public boolean isSilent() { return silent; }
}
