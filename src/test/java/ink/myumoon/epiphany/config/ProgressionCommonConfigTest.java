package ink.myumoon.epiphany.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import ink.myumoon.epiphany.ConfigTestSupport;
import ink.myumoon.epiphany.api.AptitudeFormula;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
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
        lifecycle = mockStatic(ServerLifecycleHooks.class);
        lifecycle.when(ServerLifecycleHooks::getCurrentServer).thenReturn(server);
    }

    @AfterEach
    void cleanup() {
        ProgressionConfig.onStopped(new ServerStoppedEvent(server));
        lifecycle.close();
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
        var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
                .getDeclaredConstructor(CommentedConfig.class, Path.class, ModConfig.class);
        constructor.setAccessible(true);
        var loaded = (IConfigSpec.ILoadedConfig) constructor.newInstance(raw, directory.resolve(ProgressionConfig.FILE_NAME), config);
        when(config.getSpec()).thenReturn(ProgressionConfig.SPEC);
        when(config.getLoadedConfig()).thenReturn(loaded);
        when(config.getFullPath()).thenReturn(directory.resolve(ProgressionConfig.FILE_NAME));
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

    private static Object editorValue(String key) {
        ModConfigSpec.ConfigValue<?> value = ProgressionConfig.SPEC.getValues().get(key);
        return value.getRaw();
    }

    @Test
    void nativeEditorReadsSmallTomlIntegersAsDeclaredTypes() throws Exception {
        CommentedConfig values = new TomlParser().parse("""
                maxTotalInsightPoints = 10
                baseAptitudeCap = 10
                aptitudeCapGrowth = 1
                """);
        assertInstanceOf(Integer.class, values.get("baseAptitudeCap"));
        ProgressionConfig.SPEC.correct(values);
        ModConfig config = loaded(values);
        ProgressionConfig.SPEC.acceptConfig(config.getLoadedConfig());
        ProgressionConfig.onConfig(config);

        // Same casts used by ConfigurationScreen's integer/long editor suppliers.
        assertEquals(10, (Integer) editorValue("maxTotalInsightPoints"));
        assertEquals(10L, (Long) editorValue("baseAptitudeCap"));
        assertEquals(1L, (Long) editorValue("aptitudeCapGrowth"));
        assertEquals(new ProgressionRules(10, 10, 1), ProgressionConfig.serverRules());
    }

    @Test
    void clientOnlyLoadsNormalizeEditorValuesWithoutInstallingServerRules() throws Exception {
        CommentedConfig values = new TomlParser().parse("""
                maxTotalInsightPoints = 10
                baseAptitudeCap = 9007199254740993
                aptitudeCapGrowth = 0
                """);
        values.set("maxTotalInsightPoints", 10L);
        ProgressionConfig.SPEC.correct(values);
        ModConfig config = loaded(values);
        when(config.getFullPath()).thenThrow(new IllegalStateException("Remote config has no file"));
        ProgressionConfig.SPEC.acceptConfig(config.getLoadedConfig());
        lifecycle.when(ServerLifecycleHooks::getCurrentServer).thenReturn(null);
        ProgressionConfig.onConfig(config);

        assertEquals(10, (Integer) editorValue("maxTotalInsightPoints"));
        assertEquals(9007199254740993L, (Long) editorValue("baseAptitudeCap"));
        assertEquals(0L, (Long) editorValue("aptitudeCapGrowth"));
        assertEquals(ProgressionRules.DEFAULT, ProgressionConfig.serverRules());
        verifyNoInteractions(player);
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
        verify(player).setData(EpiphanyAttachmentTypes.PROGRESSION_RULES, expected);
        // Growth prices and budgets come from the installed SERVER snapshot.
        assertEquals(expected, ProgressionConfig.serverRules());
        assertEquals(43, AptitudeFormula.calcRequiredAptitude(2, 3));
    }

    @Test
    void invalidReloadPreservesThePreviouslySyncedRule() throws Exception {
        ProgressionConfig.onConfig(loaded(raw(8, 23, 4)));
        clearInvocations(player);
        ProgressionConfig.onConfig(loaded(raw(1, 0, 0)));
        assertEquals(new ProgressionRules(8, 23, 4), ProgressionConfig.serverRules());
        verifyNoInteractions(player);
    }

    @Test
    void generatedDefaultsDuringSessionPreserveTheActiveRule() throws Exception {
        ProgressionConfig.onConfig(loaded(raw(8, 23, 4)));
        clearInvocations(player);
        CommentedConfig recreated = CommentedConfig.inMemory();
        ProgressionConfig.SPEC.correct(recreated);
        ProgressionConfig.onConfig(loaded(recreated));
        assertEquals(new ProgressionRules(8, 23, 4), ProgressionConfig.serverRules());
        verifyNoInteractions(player);
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
        verify(player, times(1)).setData(eq(EpiphanyAttachmentTypes.PROGRESSION_RULES), any(ProgressionRules.class));
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
    void loginUsesActiveRuleEvenWhenNativeConfigIsInvalid() throws Exception {
        var expected = new ProgressionRules(8, 23, 4);
        ProgressionConfig.onConfig(loaded(raw(8, 23, 4)));
        ProgressionConfig.onConfig(loaded(raw(1, 0, 0)));
        clearInvocations(player);
        ProgressionConfig.onLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        verify(player).setData(EpiphanyAttachmentTypes.PROGRESSION_RULES, expected);
    }

    @Test
    void remoteConfigSyncNeverInstallsServerRules() throws Exception {
        ModConfig remote = loaded(raw(99, 100, 10));
        when(remote.getFullPath()).thenThrow(new IllegalStateException("Remote config has no file"));
        ProgressionConfig.onConfig(remote);
        assertEquals(ProgressionRules.DEFAULT, ProgressionConfig.serverRules());
        verifyNoInteractions(player);
    }

    @Test
    void snapshotsRoundTripWithoutLosingLongPrecision() {
        var expected = new ProgressionRules(99, 9007199254740993L, Long.MAX_VALUE);
        var bytes = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ProgressionRules.STREAM_CODEC.encode(bytes, expected);
            assertEquals(expected, ProgressionRules.STREAM_CODEC.decode(bytes));
        } finally {
            bytes.release();
        }
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
        var bytes = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ProgressionRules.STREAM_CODEC.encode(bytes, expected);
            var decoded = ProgressionRules.STREAM_CODEC.decode(bytes);
            assertEquals(expected, decoded);
            assertEquals(29, decoded.requiredAptitude(1, 2));
        } finally { bytes.release(); }
        clearInvocations(player);
        values.set("curve", List.of());
        ProgressionConfig.onConfig(loaded(values));
        assertEquals(expected, ProgressionConfig.serverRules());
        verifyNoInteractions(player);
    }

    @Test
    void guiSaveCommitsFileThenPublishesAndRejectsStaleEditors() throws Exception {
        var config = loaded(raw(20, 10, 1));
        ProgressionConfig.SPEC.correct(config.getLoadedConfig().config());
        ProgressionConfig.SPEC.acceptConfig(config.getLoadedConfig());
        ProgressionConfig.onConfig(config);
        Path path = config.getFullPath();
        String original = new TomlWriter().writeToString(config.getLoadedConfig().config());
        Files.writeString(path, original);
        var candidate = new ProgressionRules(30, new AptitudeCurve(List.of(
                AptitudeCurveTest.segment(0, AptitudeCurve.Function.CONSTANT, "15.25"))));
        clearInvocations(player);
        ProgressionConfig.save(server, new ProgressionRules(20, 10, 1), original, candidate);
        assertEquals(candidate, ProgressionConfig.serverRules());
        assertEquals(candidate, ProgressionConfigIO.readFile(Files.readString(path), ProgressionRules.DEFAULT));
        assertEquals(candidate, ProgressionConfig.readRules(config.getLoadedConfig().config()));
        verify(player).setData(EpiphanyAttachmentTypes.PROGRESSION_RULES, candidate);
        clearInvocations(player);
        String saved = Files.readString(path);
        assertThrows(java.io.IOException.class, () -> ProgressionConfig.save(server, new ProgressionRules(20, 10, 1), original, ProgressionRules.DEFAULT));
        assertEquals(saved, Files.readString(path));
        assertEquals(candidate, ProgressionConfig.serverRules());
        verifyNoInteractions(player);
    }

    @Test
    void guiIoFailureDoesNotPublishOrChangeTheLoadedConfig() throws Exception {
        var config = loaded(raw(20, 10, 1));
        ProgressionConfig.SPEC.correct(config.getLoadedConfig().config());
        ProgressionConfig.SPEC.acceptConfig(config.getLoadedConfig());
        ProgressionConfig.onConfig(config);
        Files.createDirectory(config.getFullPath());
        clearInvocations(player);
        var before = ProgressionConfig.serverRules();
        assertThrows(java.io.IOException.class, () -> ProgressionConfig.save(server, before, null, new ProgressionRules(10, 20, 0)));
        assertEquals(before, ProgressionConfig.serverRules());
        assertEquals(before, ProgressionConfig.readRules(config.getLoadedConfig().config()));
        verifyNoInteractions(player);
    }
}
