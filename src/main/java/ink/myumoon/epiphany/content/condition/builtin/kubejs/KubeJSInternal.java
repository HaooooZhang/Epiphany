package ink.myumoon.epiphany.content.condition.builtin.kubejs;

import dev.latvian.mods.kubejs.stages.StageEvents;
import dev.latvian.mods.kubejs.stages.Stages;
import net.minecraft.server.level.ServerPlayer;

/**
 * Internal bridge to KubeJS Stage API. Package-private — only {@link KubeJSHelper} calls here.
 * <p>
 * <b>CRITICAL:</b> This class must NOT be referenced from any other package.
 * The JVM only loads it when {@link KubeJSHelper} enters its {@code if (LOADED)} branch.
 */
final class KubeJSInternal {

    private KubeJSInternal() {
    }

    static boolean hasStage(ServerPlayer player, String stage) {
        Stages stages = StageEvents.get(player);
        return stages.has(stage);
    }

    static boolean addStage(ServerPlayer player, String stage) {
        Stages stages = StageEvents.get(player);
        return stages.add(stage);
    }

    static boolean removeStage(ServerPlayer player, String stage) {
        Stages stages = StageEvents.get(player);
        return stages.remove(stage);
    }
}
