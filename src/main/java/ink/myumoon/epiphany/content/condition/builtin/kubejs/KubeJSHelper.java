package ink.myumoon.epiphany.content.condition.builtin.kubejs;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Public entry point for all KubeJS condition and reward checks.
 * <p>
 * All KubeJS API calls are isolated in the package-private {@link KubeJSInternal} class.
 * This class only references {@link KubeJSInternal} after verifying that the
 * {@code kubejs} mod is loaded, preventing {@link NoClassDefFoundError}
 * when KubeJS is not installed.
 */
public final class KubeJSHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean LOADED = ModList.get().isLoaded("kubejs");

    static {
        if (LOADED) {
            LOGGER.info("KubeJSHelper: kubejs mod detected — KubeJS conditions/rewards enabled");
        } else {
            LOGGER.warn("KubeJSHelper: kubejs mod NOT found — all KubeJS conditions will return false");
        }
    }

    private KubeJSHelper() {
    }

    public static boolean hasStage(ServerPlayer player, String stage) {
        if (!LOADED) return false;
        return KubeJSInternal.hasStage(player, stage);
    }

    public static boolean addStage(ServerPlayer player, String stage) {
        if (!LOADED) return false;
        return KubeJSInternal.addStage(player, stage);
    }

    public static boolean removeStage(ServerPlayer player, String stage) {
        if (!LOADED) return false;
        return KubeJSInternal.removeStage(player, stage);
    }
}
