package ink.myumoon.epiphany.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import ink.myumoon.epiphany.network.EpiphanyNetwork;
import ink.myumoon.epiphany.Epiphany;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/** Native SERVER config adapter. Gameplay never reads individual mutable config values. */
public final class ProgressionConfig {
    public static final String FILE_NAME = "epiphany-progression-server.toml";
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Validate the whole rule here, rather than let FML silently replace one invalid field
    // with a default. Explicit translation keys name the server growth settings.
    private static final ForgeConfigSpec.ConfigValue<Object> LIMIT = BUILDER
            .translation("epiphany.configuration.progression.maxTotalInsightPoints")
            .comment("Maximum total Insight Points (spent + unspent). -1: unlimited; 0: no new points.")
            .define(List.of("maxTotalInsightPoints"), () -> (Object) initialRules().maxTotalInsightPoints(), value -> value != null, Integer.class);
    private static final ForgeConfigSpec.ConfigValue<Object> BASE = BUILDER
            .translation("epiphany.configuration.progression.baseAptitudeCap")
            .comment("Legacy linear base; used only while curve=\"legacy\". Must be a positive integer.")
            .define(List.of("baseAptitudeCap"), () -> (Object) initialRules().baseAptitudeCap(), value -> value != null, Long.class);
    private static final ForgeConfigSpec.ConfigValue<Object> GROWTH = BUILDER
            .translation("epiphany.configuration.progression.aptitudeCapGrowth")
            .comment("Legacy linear growth; used only while curve=\"legacy\". Must be a nonnegative integer.")
            .define(List.of("aptitudeCapGrowth"), () -> (Object) initialRules().aptitudeCapGrowth(), value -> value != null, Long.class);
    private static final ForgeConfigSpec.ConfigValue<Object> CURVE = BUILDER
            .translation("epiphany.configuration.progression.curve")
            .comment("Piecewise aptitude prices. [[curve]] uses from=0 for the first point; the last segment extends indefinitely.",
                    "type: constant, linear, quadratic, exponential, convergent. Parameters allow up to 4 decimal places.",
                    "\"legacy\" imports baseAptitudeCap/aptitudeCapGrowth; once tables are present those two legacy values are ignored.")
            .define(List.of("curve"), ProgressionConfig::initialCurve, value -> value != null);
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static final AtomicLong revision = new AtomicLong();
    private static volatile ProgressionRules activeRules = ProgressionRules.DEFAULT;
    private static volatile MinecraftServer activeServer;
    private static volatile ModConfig activeConfig;

    private ProgressionConfig() {}

    public static void register(FMLJavaModLoadingContext context) {
        var modBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.SERVER, SPEC, FILE_NAME);
        modBus.addListener(ProgressionConfig::onLoading);
        modBus.addListener(ProgressionConfig::onReloading);
        MinecraftForge.EVENT_BUS.addListener(ProgressionConfig::onStopped);
    }

    public static ProgressionRules serverRules() {
        return activeRules;
    }

    private static Object initialCurve() {
        return activeServer == null ? "legacy" : ProgressionConfigIO.tables(activeRules.curve());
    }

    private static ProgressionRules initialRules() {
        // FML recreates syntactically broken files from defaults. During a running
        // session, those defaults must preserve the last valid rule, not reset growth.
        if (activeServer != null && activeServer == ServerLifecycleHooks.getCurrentServer()) {
            try {
                return new ProgressionRules(activeRules.maxTotalInsightPoints(), activeRules.baseAptitudeCap(), activeRules.aptitudeCapGrowth());
            } catch (IllegalStateException | ArithmeticException ignored) {
                return new ProgressionRules(activeRules.maxTotalInsightPoints(), 10, 1);
            }
        }
        return ProgressionRules.DEFAULT;
    }

    static ProgressionRules readRules(UnmodifiableConfig raw) {
        return ProgressionConfigIO.read(raw, ProgressionRules.DEFAULT);
    }

    private static void onLoading(ModConfigEvent.Loading event) {
        onConfig(event.getConfig());
    }

    private static void onReloading(ModConfigEvent.Reloading event) {
        onConfig(event.getConfig());
    }

    static void onConfig(ModConfig changed) {
        if (changed.getSpec() != SPEC) return;
        if (!(changed.getConfigData() instanceof CommentedFileConfig file)) return;
        normalizeIntegerTypes(file);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ProgressionRules candidate;
        try {
            candidate = readRules(file);
            candidate.curve().validateLimit(candidate.maxTotalInsightPoints());
        } catch (IllegalArgumentException | ArithmeticException e) {
            Epiphany.LOGGER.error("Rejected growth rules in {}: {}. Keeping the previous valid rules (defaults on first load).",
                    changed.getFullPath(), e.getMessage());
            Runnable rememberFile = () -> {
                if (ServerLifecycleHooks.getCurrentServer() == server && activeConfig == null) {
                    activeServer = server;
                    activeConfig = changed;
                }
            };
            if (server.isSameThread()) rememberFile.run();
            else server.execute(rememberFile);
            return;
        }
        long requested = revision.incrementAndGet();
        Runnable install = () -> {
            if (ServerLifecycleHooks.getCurrentServer() != server || requested != revision.get()) return;
            activeServer = server;
            activeConfig = changed;
            boolean unchanged = activeRules.equals(candidate);
            activeRules = candidate;
            if (!unchanged) EpiphanyNetwork.publishRules(server, candidate);
            Epiphany.LOGGER.info("Applied growth rules from {}: {}", changed.getFullPath(), candidate);
        };
        if (server.isSameThread()) install.run();
        else server.execute(install);
    }

    public static Path activePath(MinecraftServer server) {
        if (server != activeServer || activeConfig == null) throw new IllegalStateException("Growth config is not loaded");
        return activeConfig.getFullPath();
    }

    /** Called on the integrated server thread. A successful disk commit precedes publication. */
    public static void save(MinecraftServer server, ProgressionRules expectedRules, String expectedFile,
                            ProgressionRules candidate) throws IOException {
        if (!server.isSameThread() || ServerLifecycleHooks.getCurrentServer() != server || server != activeServer)
            throw new IOException("epiphany.curve.error.world_changed");
        if (!activeRules.equals(expectedRules)) throw new IOException("epiphany.curve.error.changed");
        Path path = activePath(server);
        ProgressionConfigIO.save(path, expectedFile, candidate);
        ProgressionConfigIO.put(activeConfig.getConfigData(), candidate);
        SPEC.afterReload();
        revision.incrementAndGet();
        activeRules = candidate;
        EpiphanyNetwork.publishRules(server, candidate);
    }

    private static void normalizeIntegerTypes(CommentedConfig raw) {
        // TOML chooses Integer or Long by magnitude. The native editor casts generic
        // ConfigValue.getRaw() to the declared default's type, so normalize exact integer
        // representations on every load, including client-only/default/remote loads.
        // Leave invalid values intact for whole-rule validation; never truncate decimals.
        Object limit = raw.get("maxTotalInsightPoints");
        if (limit instanceof Long number && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
            raw.set("maxTotalInsightPoints", number.intValue());
        }
        for (String key : List.of("baseAptitudeCap", "aptitudeCapGrowth")) {
            Object value = raw.get(key);
            if (value instanceof Integer number) raw.set(key, number.longValue());
        }
        SPEC.afterReload();
    }

    static void onStopped(ServerStoppedEvent event) {
        if (activeServer == event.getServer() || activeServer == null) {
            revision.incrementAndGet();
            activeRules = ProgressionRules.DEFAULT;
            activeServer = null;
            activeConfig = null;
        }
    }
}
