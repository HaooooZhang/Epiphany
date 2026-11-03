package ink.myumoon.epiphany;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import java.nio.file.Path;

/** Loads an in-memory config; tests never read or overwrite the developer's config. */
public final class ConfigTestSupport {
    private static boolean loaded;

    public static void reset() {
        if (!loaded) {
            CommentedConfig values = CommentedConfig.inMemory();
            Config.SPEC.correct(values);
            // ILoadedConfig is sealed. Construct FML's package-private in-memory wrapper;
            // no file or mod container is needed because these tests never call save().
            try {
                var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
                        .getDeclaredConstructor(CommentedConfig.class, Path.class, ModConfig.class);
                constructor.setAccessible(true);
                Config.SPEC.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(values, null, null));
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Could not initialize the test config", e);
            }
            loaded = true;
        }
        Config.MAX_TOTAL_INSIGHT_POINTS.set(-1);
        Config.BASE_APTITUDE_CAP.set(10L);
        Config.APTITUDE_CAP_GROWTH.set(1L);
    }

    private ConfigTestSupport() {}
}
