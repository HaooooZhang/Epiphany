package ink.myumoon.epiphany;

import com.electronwill.nightconfig.core.CommentedConfig;
import ink.myumoon.epiphany.config.CommonConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import ink.myumoon.epiphany.config.ProgressionConfig;
import ink.myumoon.epiphany.config.ProgressionRules;
import java.nio.file.Path;

/** Loads an in-memory config; tests never read or overwrite the developer's config. */
public final class ConfigTestSupport {
    private static boolean loaded;

    public static void reset() {
        if (!loaded) {
            CommentedConfig values = CommentedConfig.inMemory();
            CommonConfig.SPEC.correct(values);
            // ILoadedConfig is sealed. Construct FML's package-private in-memory wrapper;
            // no file or mod container is needed because these tests never call save().
            try {
                var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
                        .getDeclaredConstructor(CommentedConfig.class, Path.class, ModConfig.class);
                constructor.setAccessible(true);
                CommonConfig.SPEC.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(values, null, null));
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Could not initialize the test config", e);
            }
            loaded = true;
        }
        setRules(ProgressionRules.DEFAULT);
    }

    public static void setLimit(int limit) {
        ProgressionRules previous = ProgressionConfig.serverRules();
        setRules(new ProgressionRules(limit, previous.curve()));
    }

    public static void setRules(ProgressionRules rules) {
        try {
            var field = ProgressionConfig.class.getDeclaredField("activeRules");
            field.setAccessible(true);
            field.set(null, rules);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private ConfigTestSupport() {}
}
