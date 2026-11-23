package ink.myumoon.epiphany.client.ui.progression;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.config.ProgressionConfig;
import ink.myumoon.epiphany.config.ProgressionConfigIO;
import ink.myumoon.epiphany.config.ProgressionRules;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FMLConfig;

import java.nio.file.Path;
import java.util.function.Consumer;

/** Captures both the rule and its file revision when the editor opens. */
final class CurveConfigSession {
    final ProgressionRules rules;
    final Path path;
    final String file;
    final MinecraftServer server;
    final String error;

    CurveConfigSession(ProgressionRules rules, Path path, String file, MinecraftServer server, String error) {
        this.rules = rules;
        this.path = path;
        this.file = file;
        this.server = server;
        this.error = error;
    }

    static CurveConfigSession open() {
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null && mc.getConnection() != null) {
            var synced = ClientData.progressionRules();
            return synced == null
                    ? new CurveConfigSession(ProgressionRules.DEFAULT, null, null, null, "epiphany.curve.error.sync")
                    : new CurveConfigSession(synced, null, null, null, null);
        }
        Path path = null;
        ProgressionRules rules = server == null ? ProgressionRules.DEFAULT : ProgressionConfig.serverRules();
        try {
            path = server == null
                    ? FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(ProgressionConfig.FILE_NAME)
                    : ProgressionConfig.activePath(server);
            String file = ProgressionConfigIO.readFile(path);
            if (server == null) {
                rules = ProgressionConfigIO.readFile(file, rules);
            } else if (file != null) {
                ProgressionRules disk = null;
                try { disk = ProgressionConfigIO.readFile(file, rules); }
                catch (IllegalArgumentException | ArithmeticException ignored) { /* Edit the last valid snapshot to repair a rejected file. */ }
                if (disk != null && !disk.equals(rules)) throw new java.io.IOException("epiphany.curve.error.changed");
            }
            return new CurveConfigSession(rules, path, file, server, null);
        } catch (Exception e) {
            Epiphany.LOGGER.error("Cannot open growth configuration", e);
            return new CurveConfigSession(rules, path, null, server, message(e));
        }
    }

    boolean writable() { return path != null && error == null; }

    void save(ProgressionRules candidate, Consumer<String> complete) {
        Runnable commit = () -> {
            String failure = null;
            try {
                if (!writable()) throw new IllegalStateException("Growth configuration is read-only");
                if (server == null) {
                    if (Minecraft.getInstance().getConnection() != null)
                        throw new java.io.IOException("epiphany.curve.error.world_changed");
                    ProgressionConfigIO.save(path, file, candidate);
                } else {
                    ProgressionConfig.save(server, rules, file, candidate);
                }
            } catch (Exception e) {
                Epiphany.LOGGER.error("Cannot save growth configuration to {}", path, e);
                failure = message(e);
            }
            String result = failure;
            Minecraft.getInstance().execute(() -> complete.accept(result));
        };
        if (server != null) server.execute(commit);
        else commit.run();
    }

    private static String message(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
