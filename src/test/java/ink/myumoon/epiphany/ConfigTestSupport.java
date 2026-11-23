package ink.myumoon.epiphany;

import com.electronwill.nightconfig.core.CommentedConfig;
import ink.myumoon.epiphany.config.CommonConfig;
import ink.myumoon.epiphany.config.ProgressionConfig;
import ink.myumoon.epiphany.config.ProgressionRules;

/** Loads an in-memory config; tests never read or overwrite the developer's config. */
public final class ConfigTestSupport {
    private static boolean loaded;

    public static void reset() {
        if (!loaded) {
            CommentedConfig values = CommentedConfig.inMemory();
            CommonConfig.SPEC.correct(values);
            CommonConfig.SPEC.acceptConfig(values);
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
