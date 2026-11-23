package ink.myumoon.epiphany.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import ink.myumoon.epiphany.ConfigTestSupport;
import ink.myumoon.epiphany.api.AptitudeFormula;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import ink.myumoon.epiphany.network.EpiphanyNetwork;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProgressionCommonConfigTest {
    @TempDir Path directory;
    private MinecraftServer server;
    private ServerPlayer player;
    private MockedStatic<ServerLifecycleHooks> lifecycle;
    private MockedStatic<EpiphanyNetwork> network;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        try (var loading = mockStatic(LoadingModList.class)) {
            loading.when(LoadingModList::get).thenReturn(mock(LoadingModList.class));
            Bootstrap.bootStrap();
        }
    }

    @BeforeEach
    void setup() {
        ConfigTestSupport.reset();
        server = mock(MinecraftServer.class);
        player = mock(ServerPlayer.class);
        PlayerList players = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(players);
        when(players.getPlayers()).thenReturn(List.of(player));
        when(server.isSameThread()).thenReturn(true);
        network = mockStatic(EpiphanyNetwork.class);
        lifecycle = mockStatic(ServerLifecycleHooks.class);
        lifecycle.when(ServerLifecycleHooks::getCurrentServer).thenReturn(server);
    }

    @AfterEach
    void cleanup() {
        ProgressionConfig.onStopped(new ServerStoppedEvent(server));
        lifecycle.close();
        network.close();
    }

    private static CommentedConfig raw(long limit, long base, long growth) {
        CommentedConfig raw = CommentedConfig.inMemory();
        raw.set("maxTotalInsightPoints", limit);
        raw.set("baseAptitudeCap", base);
        raw.set("aptitudeCapGrowth", growth);
        return raw;
    }

    private ModConfig loaded(CommentedConfig raw) throws ReflectiveOperationException {
        ModConfig config = mock(ModConfig.class);
        var file = CommentedFileConfig.of(directory.resolve(ProgressionConfig.FILE_NAME));
        file.putAll(raw);
        doReturn(ProgressionConfig.SPEC).when(config).getSpec();
        when(config.getConfigData()).thenReturn(file);
        when(config.getFullPath()).thenReturn(file.getNioPath());
        return config;
    }

    @Test
    void removesObsoleteCommonGrowthKeysAndCreatesIndependentServerDefaults() {
        CommentedConfig common = CommentedConfig.inMemory();
        common.set("maxTotalInsightPoints", 37);
        common.set("baseAptitudeCap", 9007199254740993L);
        common.set("aptitudeCapGrowth", 4L);
        CommonConfig.SPEC.correct(common);
        for (String key : List.of("maxTotalInsightPoints", "baseAptitudeCap", "aptitudeCapGrowth"))
            assertFalse(common.contains(key));
        assertTrue(common.contains("moduleSelectCost"));
        assertTrue(common.contains("notifyInsightPoints"));
        CommentedConfig fresh = CommentedConfig.inMemory();
        ProgressionConfig.SPEC.correct(fresh);
        assertEquals(ProgressionRules.DEFAULT, ProgressionConfig.readRules(fresh));
    }

    @Test
    void preservesExistingServerLinearValues() {
        CommentedConfig existing = raw(5, 21, 2);
        ProgressionConfig.SPEC.correct(existing);
        assertEquals(new ProgressionRules(5, 21, 2), ProgressionConfig.readRules(existing));
    }

    @Test
    void readsSmallTomlIntegersWithoutAutosavingFromTheWatcher() throws Exception {
        Path path = directory.resolve(ProgressionConfig.FILE_NAME);
        String original = "# Keep this file intact while the listener parses it.\n" + """
                maxTotalInsightPoints = 10
                baseAptitudeCap = 10
                aptitudeCapGrowth = 1
                curve = "legacy"
                """;
        Files.writeString(path, original);
        try (var file = CommentedFileConfig.builder(path).sync().autosave().build()) {
            file.load();
            ModConfig config = mock(ModConfig.class);
            doReturn(ProgressionConfig.SPEC).when(config).getSpec();
            when(config.getConfigData()).thenReturn(file);
            when(config.getFullPath()).thenReturn(path);
            ProgressionConfig.onConfig(config);
            assertInstanceOf(Integer.class, file.get("baseAptitudeCap"));
            assertEquals(original, Files.readString(path));
            assertEquals(new ProgressionRules(10, 10, 1), ProgressionConfig.serverRules());
        }
    }

    @Test
    void validatesTheWholeSnapshotWithoutRepairingInvalidFields() {
        for (Object invalid : List.of(-1L, 0L, 1.5, "ten")) {
            CommentedConfig values = raw(5, 10, 1);
            values.set("baseAptitudeCap", invalid);
            ProgressionConfig.SPEC.correct(values);
            assertEquals(invalid, values.get("baseAptitudeCap"));
            assertThrows(IllegalArgumentException.class, () -> ProgressionConfig.readRules(values));
        }
        assertThrows(IllegalArgumentException.class, () -> ProgressionConfig.readRules(raw(-2, 10, 1)));
        assertThrows(IllegalArgumentException.class, () -> ProgressionConfig.readRules(raw(5, 10, -1)));
        assertThrows(ArithmeticException.class, () -> ProgressionConfig.readRules(raw(Long.MAX_VALUE, 10, 1)));
    }

    @Test
    void reloadInstallsAndBroadcastsOneCompleteSnapshot() throws Exception {
        var expected = new ProgressionRules(8, 23, 4);
        ProgressionConfig.onConfig(loaded(raw(8, 23, 4)));
        assertEquals(expected, ProgressionConfig.serverRules());
        assertEquals(43, AptitudeFormula.calcRequiredAptitude(2, 3));
        network.verify(() -> EpiphanyNetwork.publishRules(server, expected));
        // Growth prices and budgets come from the installed SERVER snapshot.
        assertEquals(expected, ProgressionConfig.serverRules());
        assertEquals(43, AptitudeFormula.calcRequiredAptitude(2, 3));
    }

    @Test
    void invalidReloadPreservesThePreviouslySyncedRule() throws Exception {
        ProgressionConfig.onConfig(loaded(raw(8, 23, 4)));
        network.clearInvocations();
        ProgressionConfig.onConfig(loaded(raw(1, 0, 0)));
        assertEquals(new ProgressionRules(8, 23, 4), ProgressionConfig.serverRules());
        network.verifyNoInteractions();
    }

    @Test
    void generatedDefaultsDuringSessionPreserveTheActiveRule() throws Exception {
        ProgressionConfig.onConfig(loaded(raw(8, 23, 4)));
        network.clearInvocations();
        CommentedConfig recreated = CommentedConfig.inMemory();
        ProgressionConfig.SPEC.correct(recreated);
        ProgressionConfig.onConfig(loaded(recreated));
        assertEquals(new ProgressionRules(8, 23, 4), ProgressionConfig.serverRules());
        network.verifyNoInteractions();
    }

    @Test
    void invalidFirstLoadUsesExplicitDefaults() throws Exception {
        ProgressionConfig.onConfig(loaded(raw(1, 0, 0)));
        assertEquals(ProgressionRules.DEFAULT, ProgressionConfig.serverRules());
    }

    @Test
    void fileWatcherQueuesToServerAndOnlyTheLatestValidUpdateWins() throws Exception {
        when(server.isSameThread()).thenReturn(false);
        List<Runnable> pending = new ArrayList<>();
        doAnswer(call -> { pending.add(call.getArgument(0)); return null; }).when(server).execute(any(Runnable.class));
        ProgressionConfig.onConfig(loaded(raw(5, 20, 2)));
        ProgressionConfig.onConfig(loaded(raw(7, 30, 3)));
        assertEquals(ProgressionRules.DEFAULT, ProgressionConfig.serverRules());
        assertEquals(2, pending.size());
        pending.forEach(Runnable::run);
        assertEquals(new ProgressionRules(7, 30, 3), ProgressionConfig.serverRules());
        network.verify(() -> EpiphanyNetwork.publishRules(eq(server), any(ProgressionRules.class)), times(1));
    }

    @Test
    void stopInvalidatesQueuedUpdatesAndNextWorldStartsWithItsOwnRules() throws Exception {
        when(server.isSameThread()).thenReturn(false);
        List<Runnable> pending = new ArrayList<>();
        doAnswer(call -> { pending.add(call.getArgument(0)); return null; }).when(server).execute(any(Runnable.class));
        ProgressionConfig.onConfig(loaded(raw(5, 20, 2)));
        ProgressionConfig.onStopped(new ServerStoppedEvent(server));
        pending.forEach(Runnable::run);
        assertEquals(ProgressionRules.DEFAULT, ProgressionConfig.serverRules());
        when(server.isSameThread()).thenReturn(true);
        ProgressionConfig.onConfig(loaded(raw(2, 45, 6)));
        assertEquals(new ProgressionRules(2, 45, 6), ProgressionConfig.serverRules());
    }

    @Test
    void remoteConfigSyncNeverInstallsServerRules() throws Exception {
        ModConfig remote = loaded(raw(99, 100, 10));
        when(remote.getConfigData()).thenReturn(raw(99, 100, 10));
        ProgressionConfig.onConfig(remote);
        assertEquals(ProgressionRules.DEFAULT, ProgressionConfig.serverRules());
        network.verifyNoInteractions();
    }

    @Test
    void snapshotsKeepOldLinearPricesAndRejectOverflow() {
        assertEquals(10, ProgressionRules.DEFAULT.requiredAptitude(0, 0));
        assertEquals(15, ProgressionRules.DEFAULT.requiredAptitude(2, 3));
        assertThrows(ArithmeticException.class, () -> new ProgressionRules(-1, Long.MAX_VALUE, 1).requiredAptitude(0, 1));
    }

    @Test
    void segmentedReloadAndNetworkUseTheSameExactPrices() throws Exception {
        var curve = new AptitudeCurve(List.of(
                AptitudeCurveTest.segment(0, AptitudeCurve.Function.LINEAR, "10", "1"),
                AptitudeCurveTest.segment(2, AptitudeCurve.Function.CONVERGENT, "20.0001", "100", "0.1")));
        var expected = new ProgressionRules(20, curve);
        var values = raw(20, 999, 999);
        ProgressionConfigIO.put(values, expected);
        ProgressionConfig.onConfig(loaded(values));
        assertEquals(expected, ProgressionConfig.serverRules());
        assertEquals(21, AptitudeFormula.calcRequiredAptitude(1, 1));
        network.clearInvocations();
        values.set("curve", List.of());
        ProgressionConfig.onConfig(loaded(values));
        assertEquals(expected, ProgressionConfig.serverRules());
        network.verifyNoInteractions();
    }

    @Test
    void guiSaveCommitsFileThenPublishesAndRejectsStaleEditors() throws Exception {
        var config = loaded(raw(20, 10, 1));
        ProgressionConfig.SPEC.correct(config.getConfigData());
        ProgressionConfig.SPEC.acceptConfig(config.getConfigData());
        ProgressionConfig.onConfig(config);
        Path path = config.getFullPath();
        String original = new TomlWriter().writeToString(config.getConfigData());
        Files.writeString(path, original);
        var candidate = new ProgressionRules(30, new AptitudeCurve(List.of(
                AptitudeCurveTest.segment(0, AptitudeCurve.Function.CONSTANT, "15.25"))));
        network.clearInvocations();
        ProgressionConfig.save(server, new ProgressionRules(20, 10, 1), original, candidate);
        assertEquals(candidate, ProgressionConfig.serverRules());
        assertEquals(candidate, ProgressionConfigIO.readFile(Files.readString(path), ProgressionRules.DEFAULT));
        assertEquals(candidate, ProgressionConfig.readRules(config.getConfigData()));
        network.verify(() -> EpiphanyNetwork.publishRules(server, candidate));
        network.clearInvocations();
        String saved = Files.readString(path);
        assertThrows(java.io.IOException.class, () -> ProgressionConfig.save(server, new ProgressionRules(20, 10, 1), original, ProgressionRules.DEFAULT));
        assertEquals(saved, Files.readString(path));
        assertEquals(candidate, ProgressionConfig.serverRules());
        network.verifyNoInteractions();
    }

    @Test
    void conflictingEditorSaveKeepsTheExternalRulesForForgeUnload() throws Exception {
        var before = new ProgressionRules(20, 10, 1);
        var config = loaded(raw(20, 10, 1));
        var file = (CommentedFileConfig) config.getConfigData();
        ProgressionConfig.onConfig(config);
        ProgressionConfigIO.save(file.getNioPath(), null, before);
        var opened = Files.readString(file.getNioPath());
        var external = new ProgressionRules(30, 23, 1);
        ProgressionConfigIO.save(file.getNioPath(), opened, external);
        var draft = new ProgressionRules(40, 28, 1);
        network.clearInvocations();

        assertThrows(ProgressionConfigIO.FileChangedException.class,
                () -> ProgressionConfig.save(server, before, opened, draft));
        assertEquals(external, ProgressionConfig.serverRules());
        network.verify(() -> EpiphanyNetwork.publishRules(server, external));

        file.save(); // Forge saves the in-memory config before unloading the world.
        assertEquals(external, ProgressionConfigIO.readFile(Files.readString(file.getNioPath()), ProgressionRules.DEFAULT));
    }

    @Test
    void guiIoFailureDoesNotPublishOrChangeTheLoadedConfig() throws Exception {
        var config = loaded(raw(20, 10, 1));
        ProgressionConfig.SPEC.correct(config.getConfigData());
        ProgressionConfig.SPEC.acceptConfig(config.getConfigData());
        ProgressionConfig.onConfig(config);
        Files.createDirectory(config.getFullPath());
        network.clearInvocations();
        var before = ProgressionConfig.serverRules();
        assertThrows(java.io.IOException.class, () -> ProgressionConfig.save(server, before, null, new ProgressionRules(10, 20, 0)));
        assertEquals(before, ProgressionConfig.serverRules());
        assertEquals(before, ProgressionConfig.readRules(config.getConfigData()));
        network.verifyNoInteractions();
    }
}
