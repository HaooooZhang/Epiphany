package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerData;
import org.junit.jupiter.api.AfterEach;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerState;
import ink.myumoon.epiphany.config.ProgressionConfig;
import ink.myumoon.epiphany.config.ProgressionRules;
import ink.myumoon.epiphany.ConfigTestSupport;
import ink.myumoon.epiphany.content.reward.InsightPointsReward;
import ink.myumoon.epiphany.content.reward.RewardListHelper;
import ink.myumoon.epiphany.content.reward.RewardSource;
import ink.myumoon.epiphany.content.AptitudeAmount;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import ink.myumoon.epiphany.command.EpiphanyCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import ink.myumoon.epiphany.event.AptitudeChangedEvent;
import ink.myumoon.epiphany.event.AptitudeLevelUpEvent;
import ink.myumoon.epiphany.event.InsightPointsChangedEvent;
import ink.myumoon.epiphany.event.ModuleSelectEvent;
import ink.myumoon.epiphany.event.ModuleSelectedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.NbtOps;
import ink.myumoon.epiphany.registry.EpiphanyAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AptitudeSourceGrantTest {
    private static final ResourceLocation CLAIM = ResourceLocation.parse("epiphany:kill_entity/minecraft.zombie");
    private ServerPlayer player;
    private RandomSource random;
    private AttributeInstance attribute;
    private AtomicReference<PlayerEpiphanyData> state;
    private MockedStatic<EpiphanyPlayerData> progress;

    @BeforeAll
    static void bootstrapMinecraft() throws ReflectiveOperationException {
        SharedConstants.tryDetectVersion();
        // Vanilla registries query the loading mod list while collecting feature flags.
        // These isolated JVM tests deliberately load no mods.
        try (var loading = mockStatic(LoadingModList.class);
             var networkHooks = mockStatic(net.minecraftforge.network.NetworkHooks.class)) {
            loading.when(LoadingModList::get).thenReturn(mock(LoadingModList.class));
            Bootstrap.bootStrap();
        }
        // Prime the listener lists that ModLauncher normally installs in event classes.
        var eventPlayer = mock(ServerPlayer.class);
        new AptitudeChangedEvent(eventPlayer, 0, 0).getListenerList();
        new AptitudeLevelUpEvent(eventPlayer, 0).getListenerList();
        new InsightPointsChangedEvent(eventPlayer, 0, 0).getListenerList();
        MinecraftForge.EVENT_BUS.start();
        var field = net.minecraftforge.registries.RegistryObject.class.getDeclaredField("value");
        field.setAccessible(true);
        field.set(EpiphanyAttributes.APTITUDE_GAIN_MULTIPLIER,
                new net.minecraft.world.entity.ai.attributes.RangedAttribute("test.epiphany", 1.0, 0.0, 100.0));
    }

    @BeforeEach
    void setUp() {
        ConfigTestSupport.reset();
        player = mock(ServerPlayer.class);
        random = mock(RandomSource.class);
        attribute = mock(AttributeInstance.class);
        state = new AtomicReference<>(PlayerEpiphanyData.createDefault());
        when(player.getRandom()).thenReturn(random);
        when(player.getAttribute(any(net.minecraft.world.entity.ai.attributes.Attribute.class))).thenReturn(attribute);
        when(attribute.getValue()).thenReturn(1.0);
        progress = mockStatic(EpiphanyPlayerData.class);
        progress.when(() -> EpiphanyPlayerData.get(player)).thenAnswer(call -> state.get());
        progress.when(() -> EpiphanyPlayerData.set(eq(player), any(PlayerEpiphanyData.class)))
                .thenAnswer(call -> { state.set(call.getArgument(1)); return null; });
    }

    @AfterEach
    void closeProgressMock() {
        if (progress != null) progress.close();
    }

    @Test
    void missingModuleAndInsightRefundOnlyOnce() throws ReflectiveOperationException {
        var module = ResourceLocation.parse("test:orphan_module");
        var insight = ResourceLocation.parse("test:orphan_insight");
        mockRegistries(null, null);
        when(player.getGameProfile()).thenReturn(new GameProfile(UUID.randomUUID(), "test"));
        state.set(PlayerEpiphanyData.createDefault()
                .withInsightPoints(2).withTotalInsightPointsSpent(3)
                .withModuleState(module, new ModulePlayerState(true, true, true, Set.of(insight)))
                .withInsightState(insight, new ink.myumoon.epiphany.attachment.InsightPlayerState(true, false)));

        ModuleManager.cleanupOrphanedData(player);
        var cleaned = state.get();
        assertFalse(cleaned.modules().containsKey(module));
        assertFalse(cleaned.insights().containsKey(insight));
        assertEquals(4, cleaned.insightPoints());
        assertEquals(1, cleaned.totalInsightPointsSpent());

        ModuleManager.cleanupOrphanedData(player);
        assertEquals(cleaned, state.get());
    }

    @Test
    void canceledModuleSelectionKeepsPointsAndSkipsThePostEvent() throws ReflectiveOperationException {
        var module = ResourceLocation.parse("test:module");
        mockRegistries(null, null);
        var initial = PlayerEpiphanyData.createDefault().withInsightPoints(3)
                .withModuleState(module, new ModulePlayerState(true, false, false, Set.of()));
        state.set(initial);
        new ModuleSelectEvent(player, module).getListenerList();
        new ModuleSelectedEvent(player, module).getListenerList();
        Consumer<ModuleSelectEvent> cancel = event -> event.setCanceled(true);
        var postCount = new AtomicInteger();
        Consumer<ModuleSelectedEvent> post = event -> postCount.incrementAndGet();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModuleSelectEvent.class, cancel);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModuleSelectedEvent.class, post);
        try {
            ModuleManager.select(player, module);
            assertEquals(initial, state.get());
            assertEquals(0, postCount.get());
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
            MinecraftForge.EVENT_BUS.unregister(post);
        }
    }

    @Test
    void ordinaryPlayerCannotUseManagementCommands() {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        EpiphanyCommand.register(dispatcher);
        var source = mock(CommandSourceStack.class);
        assertThrows(com.mojang.brigadier.exceptions.CommandSyntaxException.class,
                () -> dispatcher.execute("epiphany path list", source));
        verify(source).hasPermission(2);
    }

    private MockedStatic<ProgressionConfig> price(long required) {
        ProgressionRules rules = mock(ProgressionRules.class);
        when(rules.requiredAptitude(anyLong(), anyInt())).thenReturn(required);
        when(rules.remainingPoints(anyInt(), anyInt())).thenAnswer(call ->
                (long) Integer.MAX_VALUE - (int) call.getArgument(1));
        MockedStatic<ProgressionConfig> config = mockStatic(ProgressionConfig.class);
        config.when(ProgressionConfig::serverRules).thenReturn(rules);
        return config;
    }

    @Test
    void settlesAcrossSegmentsAndRejectsOverflowWithoutPartialCommitOrClaim() {
        var rules = ink.myumoon.epiphany.config.ProgressionConfigIO.readFile("""
                maxTotalInsightPoints = -1
                [[curve]]
                from = 0
                type = "constant"
                base = 10
                [[curve]]
                from = 2
                type = "linear"
                base = 20
                slope = 5
                """, ProgressionRules.DEFAULT);
        ConfigTestSupport.setRules(rules);
        assertTrue(AptitudeManager.grantAptitude(player, 66, CLAIM));
        assertEquals(4, state.get().insightPoints());
        assertEquals(1, state.get().aptitude());
        assertTrue(state.get().claimedFirsts().containsKey(CLAIM));

        state.set(PlayerEpiphanyData.createDefault());
        ConfigTestSupport.setRules(ink.myumoon.epiphany.config.ProgressionConfigIO.readFile("""
                maxTotalInsightPoints = -1
                [[curve]]
                from = 0
                type = "constant"
                base = 1
                [[curve]]
                from = 1
                type = "quadratic"
                base = 9223372036854775807
                slope = 1
                quadratic = 1
                """, ProgressionRules.DEFAULT));
        state.set(state.get().withInsightPoints(2));
        var before = state.get();
        assertFalse(AptitudeManager.grantAptitude(player, 10, CLAIM));
        assertSame(before, state.get());
        assertEquals(0, AptitudeManager.fillAptitude(player));
    }

    @SuppressWarnings("unchecked")
    private Registry<ModuleData> mockRegistries(InsightData insight, EpiphanyData epiphany)
            throws ReflectiveOperationException {
        MinecraftServer server = mock(MinecraftServer.class);
        var serverField = ServerPlayer.class.getField("server");
        serverField.setAccessible(true);
        serverField.set(player, server);
        RegistryAccess.Frozen access = mock(RegistryAccess.Frozen.class);
        when(server.registryAccess()).thenReturn(access);
        Registry<ModuleData> modules = mock(Registry.class);
        Registry<InsightData> insights = mock(Registry.class);
        Registry<EpiphanyData> epiphanies = mock(Registry.class);
        when(access.registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY)).thenReturn(modules);
        when(access.registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY)).thenReturn(insights);
        when(access.registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY)).thenReturn(epiphanies);
        when(insights.get(ResourceLocation.parse("epiphany:insight"))).thenReturn(insight);
        when(epiphanies.get(ResourceLocation.parse("epiphany:ability"))).thenReturn(epiphany);
        when(player.getGameProfile()).thenReturn(new GameProfile(UUID.randomUUID(), "Test"));
        return modules;
    }

    @Test
    void independentlySamplesThenAppliesMultiplierOnceAndCommitsOnce() {
        var resolution = new AptitudeSourceResolver.Resolution(true,
                new AptitudeAmount(1, 2), new AptitudeAmount(1, 2), CLAIM);
        when(random.nextLong()).thenReturn(0L);
        when(attribute.getValue()).thenReturn(0.5);
        try (var ignored = price(10)) {
            assertTrue(AptitudeSourceManager.grantResolved(player, resolution));
        }
        assertEquals(1, state.get().aptitude()); // floor((1 + 1) * 0.5), not floor(0.5) twice
        assertTrue(state.get().claimedFirsts().containsKey(CLAIM));
        verify(random, times(2)).nextLong();
        verify(attribute, times(1)).getValue();
        progress.verify(() -> EpiphanyPlayerData.set(eq(player), any(PlayerEpiphanyData.class)), times(1));
    }

    @Test
    void zeroCombinedGainOrZeroMultiplierDoesNotClaim() {
        var zero = new AptitudeSourceResolver.Resolution(true, AptitudeAmount.ZERO, new AptitudeAmount(0, 1), CLAIM);
        when(random.nextLong()).thenReturn(0L);
        assertFalse(AptitudeSourceManager.grantResolved(player, zero));
        when(attribute.getValue()).thenReturn(0.0);
        assertFalse(AptitudeSourceManager.grantResolved(player,
                new AptitudeSourceResolver.Resolution(true, 50, CLAIM)));
        assertEquals(PlayerEpiphanyData.createDefault(), state.get());
        progress.verify(() -> EpiphanyPlayerData.set(eq(player), any(PlayerEpiphanyData.class)), never());
    }

    @Test
    void baseAndFirstRewardUseSeparateDraws() {
        when(random.nextLong()).thenReturn(0L, 2L);
        try (var ignored = price(10)) {
            assertTrue(AptitudeSourceManager.grantResolved(player, new AptitudeSourceResolver.Resolution(true,
                    new AptitudeAmount(1, 2), new AptitudeAmount(1, 2), CLAIM)));
        }
        assertEquals(3, state.get().aptitude());
        verify(random, times(2)).nextLong();
    }

    @Test
    void zeroFirstRollIsClaimedIfBaseGainSucceeds() {
        when(random.nextLong()).thenReturn(0L);
        try (var ignored = price(10)) {
            assertTrue(AptitudeSourceManager.grantResolved(player, new AptitudeSourceResolver.Resolution(true,
                    AptitudeAmount.fixed(2), new AptitudeAmount(0, 1), CLAIM)));
        }
        assertEquals(2, state.get().aptitude());
        assertTrue(state.get().claimedFirsts().containsKey(CLAIM));
    }

    @Test
    void claimAndFinalPointsAreVisibleBeforeEventsAndCannotBeClaimedAgain() {
        AtomicBoolean observed = new AtomicBoolean();
        Consumer<AptitudeChangedEvent> listener = event -> {
            if (event.getPlayer() != player) return;
            assertEquals(2, state.get().insightPoints());
            assertEquals(5, state.get().aptitude());
            assertTrue(state.get().claimedFirsts().containsKey(CLAIM));
            assertFalse(AptitudeManager.grantAptitude(player, 25, CLAIM));
            observed.set(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AptitudeChangedEvent.class, listener);
        try (var ignored = price(10)) {
            assertTrue(AptitudeSourceManager.grantResolved(player,
                    new AptitudeSourceResolver.Resolution(true, 25, CLAIM)));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        assertTrue(observed.get());
    }

    @Test
    void rejectsOverflowBeforeClaimingOrMutating() {
        state.set(state.get().withAptitude(Long.MAX_VALUE));
        assertFalse(AptitudeSourceManager.grantResolved(player,
                new AptitudeSourceResolver.Resolution(true, 1, CLAIM)));
        state.set(PlayerEpiphanyData.createDefault());
        when(attribute.getValue()).thenReturn(2.0);
        assertFalse(AptitudeSourceManager.grantResolved(player,
                new AptitudeSourceResolver.Resolution(true, Long.MAX_VALUE, CLAIM)));
        assertTrue(state.get().claimedFirsts().isEmpty());
        progress.verify(() -> EpiphanyPlayerData.set(eq(player), any(PlayerEpiphanyData.class)), never());
    }

    @Test
    void preservesExactLongAmountsAtUnitMultiplier() {
        assertEquals(Long.MAX_VALUE, AptitudeManager.scaleAptitudeWithMultiplier(player, Long.MAX_VALUE));
        assertEquals(9007199254740993L, AptitudeManager.scaleAptitudeWithMultiplier(player, 9007199254740993L));
    }

    @Test
    void invalidPriceAndPointOverflowLeaveClaimUnchanged() {
        try (var ignored = price(0)) {
            assertFalse(AptitudeSourceManager.grantResolved(player,
                    new AptitudeSourceResolver.Resolution(true, 1, CLAIM)));
        }
        state.set(state.get().withInsightPoints(Integer.MAX_VALUE));
        try (var ignored = price(1)) {
            assertFalse(AptitudeSourceManager.grantResolved(player,
                    new AptitudeSourceResolver.Resolution(true, 1, CLAIM)));
        }
        assertEquals(Integer.MAX_VALUE, state.get().insightPoints());
        assertTrue(state.get().claimedFirsts().isEmpty());
        progress.verify(() -> EpiphanyPlayerData.set(eq(player), any(PlayerEpiphanyData.class)), never());
    }

    @Test
    void finiteCapSettlesOnlyRemainingPointsAndDiscardsOverflow() {
        ConfigTestSupport.setLimit(3);
        state.set(state.get().withInsightPoints(1).withTotalInsightPointsSpent(1).withAptitude(5));
        AtomicInteger levels = new AtomicInteger();
        var changes = new ArrayList<String>();
        Consumer<AptitudeLevelUpEvent> level = event -> {
            if (event.getPlayer() == player) levels.incrementAndGet();
        };
        Consumer<InsightPointsChangedEvent> points = event -> {
            if (event.getPlayer() == player) changes.add("points");
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AptitudeLevelUpEvent.class, level);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, InsightPointsChangedEvent.class, points);
        try {
            assertTrue(AptitudeSourceManager.grantResolved(player,
                    new AptitudeSourceResolver.Resolution(true, 100, CLAIM)));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(level);
            MinecraftForge.EVENT_BUS.unregister(points);
        }
        assertEquals(2, state.get().insightPoints());
        assertEquals(0, state.get().aptitude());
        assertTrue(state.get().claimedFirsts().containsKey(CLAIM));
        assertEquals(1, levels.get());
        assertEquals(List.of("points"), changes);
        assertFalse(AptitudeManager.canGainInsightPoints(player));
    }

    @Test
    void cappedSourceDoesNotSampleConsumeFirstOrNotify() {
        ConfigTestSupport.setLimit(0);
        var resolution = new AptitudeSourceResolver.Resolution(true,
                new AptitudeAmount(1, 10), new AptitudeAmount(1, 10), CLAIM);
        AtomicInteger events = new AtomicInteger();
        Consumer<AptitudeChangedEvent> listener = event -> {
            if (event.getPlayer() == player) events.incrementAndGet();
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AptitudeChangedEvent.class, listener);
        try {
            assertFalse(AptitudeSourceManager.grantResolved(player, resolution));
            assertEquals(0, AptitudeManager.fillAptitude(player));
            AptitudeManager.addAptitudeWithMultiplier(player, 100);
            AptitudeManager.setAptitude(player, 100);
            AptitudeManager.addInsightPoints(player, 100);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        verifyNoInteractions(random, attribute);
        progress.verify(() -> EpiphanyPlayerData.set(eq(player), any(PlayerEpiphanyData.class)), never());
        assertEquals(0, events.get());
    }

    @Test
    void loweringLimitPreservesOldDataAndRaisingLimitResumesGrowth() {
        state.set(state.get().withInsightPoints(3).withTotalInsightPointsSpent(4).withAptitude(8));
        ConfigTestSupport.setLimit(5);
        PlayerEpiphanyData before = state.get();
        assertFalse(AptitudeManager.grantAptitude(player, 100, CLAIM));
        AptitudeManager.setInsightPoints(player, 10);
        assertEquals(before, state.get());
        AptitudeManager.setInsightPoints(player, 2); // A reduction must not silently clip to L - spent = 1.
        assertEquals(2, state.get().insightPoints());
        assertEquals(8, state.get().aptitude());
        ConfigTestSupport.setLimit(7);
        assertTrue(AptitudeManager.grantAptitude(player, 100, CLAIM));
        assertEquals(3, state.get().insightPoints());
        assertEquals(0, state.get().aptitude());
    }

    @Test
    void spendingAndRefundingCannotReopenBudget() {
        ConfigTestSupport.setLimit(5);
        state.set(state.get().withInsightPoints(5));
        state.set(state.get().withInsightPoints(2).withTotalInsightPointsSpent(3));
        assertFalse(AptitudeManager.canGainInsightPoints(player));
        AptitudeManager.addInsightPoints(player, 10);
        assertEquals(2, state.get().insightPoints());
        state.set(state.get().withInsightPoints(5).withTotalInsightPointsSpent(0));
        assertFalse(AptitudeManager.canGainInsightPoints(player));
        assertEquals(5, AptitudeManager.getTotalInsightPoints(player));
        AptitudeManager.addInsightPoints(player, -1);
        assertTrue(AptitudeManager.canGainInsightPoints(player));
    }

    @Test
    void directPointGrantsClipClearAptitudeAndDoNotOverflow() {
        ConfigTestSupport.setLimit(5);
        state.set(state.get().withTotalInsightPointsSpent(3).withInsightPoints(1).withAptitude(8));
        AptitudeManager.addInsightPoints(player, Integer.MAX_VALUE);
        assertEquals(2, state.get().insightPoints());
        assertEquals(0, state.get().aptitude());
        ConfigTestSupport.setLimit(-1);
        AptitudeManager.addInsightPoints(player, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, state.get().insightPoints());
        assertEquals((long) Integer.MAX_VALUE + 3, AptitudeManager.getTotalInsightPoints(player));
        AptitudeManager.addInsightPoints(player, Integer.MIN_VALUE);
        assertEquals(0, state.get().insightPoints());
    }

    @Test
    void fillRespectsRealLinearPriceAndStopsAtLastPoint() {
        ConfigTestSupport.setLimit(2);
        state.set(state.get().withInsightPoints(1).withAptitude(4));
        assertEquals(7, AptitudeManager.fillAptitude(player));
        assertEquals(2, state.get().insightPoints());
        assertEquals(0, state.get().aptitude());
        assertEquals(0, AptitudeManager.fillAptitude(player));
    }

    @Test
    void unlimitedModeKeepsLinearPricesAndRemainder() {
        AptitudeManager.addAptitude(player, 35);
        assertEquals(3, state.get().insightPoints()); // 10 + 11 + 12
        assertEquals(2, state.get().aptitude());
    }

    @Test
    void partialRewardUndoUsesReceiptEvenIfDefinitionChanged() {
        ConfigTestSupport.setLimit(5);
        state.set(state.get().withInsightPoints(4));
        var source = new RewardSource("module", ResourceLocation.parse("epiphany:test"), "on_select_reward");
        new InsightPointsReward(3).apply(player, source);
        assertEquals(5, state.get().insightPoints());
        new InsightPointsReward(100).remove(player, source);
        assertEquals(4, state.get().insightPoints());
        assertTrue(state.get().insightPointRewards().isEmpty());
        new InsightPointsReward(100).remove(player, source);
        assertEquals(4, state.get().insightPoints());
    }

    @Test
    void rewardReceiptIsVisibleBeforeEventsAndConsumedBeforeReentrantUndo() {
        AtomicInteger observed = new AtomicInteger();
        Consumer<InsightPointsChangedEvent> listener = event -> {
            if (event.getPlayer() != player) return;
            if (observed.getAndIncrement() == 0) {
                assertFalse(state.get().insightPointRewards().isEmpty());
                AptitudeManager.removeInsightPointsReward(player, "test");
            } else {
                assertTrue(state.get().insightPointRewards().isEmpty());
                AptitudeManager.removeInsightPointsReward(player, "test");
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, InsightPointsChangedEvent.class, listener);
        try {
            AptitudeManager.applyInsightPointsReward(player, "test", 3);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        assertEquals(0, state.get().insightPoints());
        assertEquals(2, observed.get());
    }

    @Test
    void clippedZeroRewardsDoNotRemoveUnrelatedPointsAndRepeatedApplicationsUnwindInOrder() {
        ConfigTestSupport.setLimit(5);
        state.set(state.get().withInsightPoints(4));
        AptitudeManager.applyInsightPointsReward(player, "test", 3); // +1
        AptitudeManager.applyInsightPointsReward(player, "test", 3); // +0
        AptitudeManager.removeInsightPointsReward(player, "test");
        assertEquals(5, state.get().insightPoints());
        AptitudeManager.removeInsightPointsReward(player, "test");
        assertEquals(4, state.get().insightPoints());
    }

    @Test
    void negativeRewardTracksActualDeductionAndRestorationObeysChangedLimit() {
        state.set(state.get().withInsightPoints(2));
        AptitudeManager.applyInsightPointsReward(player, "negative", -5);
        assertEquals(0, state.get().insightPoints());
        ConfigTestSupport.setLimit(1);
        AptitudeManager.removeInsightPointsReward(player, "negative");
        assertEquals(1, state.get().insightPoints());
        assertTrue(state.get().insightPointRewards().isEmpty());
    }

    @Test
    void rewardListsKeepOwnersSlotsAndIndicesSeparate() {
        var owner = ResourceLocation.parse("epiphany:test");
        RewardListHelper.applyInsight(List.of(new InsightPointsReward(3), new InsightPointsReward(2)), player, owner);
        RewardListHelper.applyEpiphany(List.of(new InsightPointsReward(7)), player, owner);
        RewardListHelper.applyInsight(List.of(new InsightPointsReward(4)), player, owner, "on_select_reward");
        assertEquals(16, state.get().insightPoints());
        assertEquals(4, state.get().insightPointRewards().size());
        RewardListHelper.removeInsight(List.of(new InsightPointsReward(100), new InsightPointsReward(100)), player, owner);
        assertEquals(11, state.get().insightPoints());
        RewardListHelper.removeEpiphany(List.of(new InsightPointsReward(100)), player, owner);
        assertEquals(4, state.get().insightPoints());
        RewardListHelper.removeInsight(List.of(new InsightPointsReward(100)), player, owner, "on_select_reward");
        assertEquals(0, state.get().insightPoints());
        assertTrue(state.get().insightPointRewards().isEmpty());
    }

    @Test
    void receiptsSurviveSaveSyncAndOldSaveDefaultsAreSafe() {
        AptitudeManager.applyInsightPointsReward(player, "saved", 3);
        var encoded = PlayerEpiphanyData.CODEC.encodeStart(JsonOps.INSTANCE, state.get()).result().orElseThrow();
        assertEquals(state.get(), PlayerEpiphanyData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeWithCodec(NbtOps.INSTANCE, PlayerEpiphanyData.CODEC, state.get());
            assertEquals(state.get(), buffer.readWithCodec(NbtOps.INSTANCE, PlayerEpiphanyData.CODEC));
        } finally {
            buffer.release();
        }
        encoded.getAsJsonObject().remove("insightPointRewards");
        PlayerEpiphanyData legacy = PlayerEpiphanyData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        assertTrue(legacy.insightPointRewards().isEmpty());
        state.set(legacy);
        AptitudeManager.removeInsightPointsReward(player, "saved");
        assertEquals(3, state.get().insightPoints());
    }

    @Test
    void moduleResetKeepsRewardReversalAndRefundsCostsAboveLoweredLimit() throws ReflectiveOperationException {
        var moduleId = ResourceLocation.parse("epiphany:module");
        var reward = new InsightPointsReward(3);
        ModuleData module = mock(ModuleData.class);
        when(module.onSelectReward()).thenReturn(List.of(reward));
        var modules = mockRegistries(null, null);
        when(modules.get(moduleId)).thenReturn(module);
        state.set(state.get().withInsightPoints(3).withTotalInsightPointsSpent(1)
                .withModuleState(moduleId, new ModulePlayerState(true, true, false, Set.of())));
        ConfigTestSupport.setLimit(5);
        RewardListHelper.applyInsight(module.onSelectReward(), player, moduleId, "on_select_reward");
        assertEquals(4, state.get().insightPoints());
        ConfigTestSupport.setLimit(2);
        ModuleManager.resetModule(player, moduleId);
        assertEquals(4, state.get().insightPoints()); // -1 actually granted, +1 cost refund
        assertEquals(0, state.get().totalInsightPointsSpent());
        assertTrue(state.get().insightPointRewards().isEmpty());
        assertFalse(state.get().modules().get(moduleId).selected());
    }

    @Test
    void insightResetKeepsRewardReversalAndRefund() throws ReflectiveOperationException {
        var moduleId = ResourceLocation.parse("epiphany:module");
        var insightId = ResourceLocation.parse("epiphany:insight");
        InsightData insight = mock(InsightData.class);
        when(insight.cost()).thenReturn(1);
        when(insight.reward()).thenReturn(List.of(new InsightPointsReward(3)));
        mockRegistries(insight, null);
        state.set(state.get().withInsightPoints(3).withTotalInsightPointsSpent(1)
                .withModuleState(moduleId, new ModulePlayerState(true, true, false, Set.of(insightId))));
        ConfigTestSupport.setLimit(5);
        RewardListHelper.applyInsight(insight.reward(), player, insightId);
        InsightManager.resetInsight(player, insightId);
        assertEquals(4, state.get().insightPoints());
        assertEquals(0, state.get().totalInsightPointsSpent());
        assertTrue(state.get().insightPointRewards().isEmpty());
        assertTrue(state.get().modules().get(moduleId).unlockedInsights().isEmpty());
    }

    @Test
    void epiphanyResetKeepsRewardReversalAndFreesSlot() throws ReflectiveOperationException {
        var epiphanyId = ResourceLocation.parse("epiphany:ability");
        EpiphanyData epiphany = mock(EpiphanyData.class);
        when(epiphany.reward()).thenReturn(List.of(new InsightPointsReward(3)));
        mockRegistries(null, epiphany);
        state.set(state.get().withEpiphanySlots(1).withUsedEpiphanySlots(1)
                .withEpiphanyState(epiphanyId, new EpiphanyPlayerState(true, true)));
        ConfigTestSupport.setLimit(1);
        RewardListHelper.applyEpiphany(epiphany.reward(), player, epiphanyId);
        EpiphanyManager.resetEpiphany(player, epiphanyId);
        assertEquals(0, state.get().insightPoints());
        assertEquals(0, state.get().usedEpiphanySlots());
        assertTrue(state.get().insightPointRewards().isEmpty());
    }

    @Test
    void orphanModuleCleanupKeepsRewardReversalAndRefund() throws ReflectiveOperationException {
        var moduleId = ResourceLocation.parse("epiphany:module");
        var insightId = ResourceLocation.parse("epiphany:insight");
        InsightData insight = mock(InsightData.class);
        when(insight.cost()).thenReturn(1);
        when(insight.reward()).thenReturn(List.of(new InsightPointsReward(3)));
        mockRegistries(insight, null); // Module absent, Insight definition still present.
        state.set(state.get().withInsightPoints(2).withTotalInsightPointsSpent(2)
                .withModuleState(moduleId, new ModulePlayerState(true, true, false, Set.of(insightId))));
        ConfigTestSupport.setLimit(5);
        RewardListHelper.applyInsight(insight.reward(), player, insightId);
        ModuleManager.cleanupOrphanedData(player);
        assertEquals(4, state.get().insightPoints());
        assertEquals(0, state.get().totalInsightPointsSpent());
        assertTrue(state.get().modules().isEmpty());
        assertTrue(state.get().insightPointRewards().isEmpty());
    }

    @Test
    void selectionResetKeepsCurrencyAfterReversalAndAllResetClearsReceipts() throws ReflectiveOperationException {
        var moduleId = ResourceLocation.parse("epiphany:module");
        ModuleData module = mock(ModuleData.class);
        when(module.onSelectReward()).thenReturn(List.of(new InsightPointsReward(3)));
        var modules = mockRegistries(null, null);
        when(modules.get(moduleId)).thenReturn(module);
        state.set(state.get().withInsightPoints(3).withTotalInsightPointsSpent(1)
                .withModuleState(moduleId, new ModulePlayerState(true, true, false, Set.of())));
        ConfigTestSupport.setLimit(5);
        RewardListHelper.applyInsight(module.onSelectReward(), player, moduleId, "on_select_reward");
        EpiphanyDataUtils.resetSelections(player);
        assertEquals(4, state.get().insightPoints());
        assertEquals(0, state.get().totalInsightPointsSpent());
        assertTrue(state.get().insightPointRewards().isEmpty());
        AptitudeManager.applyInsightPointsReward(player, "other", 3);
        EpiphanyDataUtils.resetAll(player);
        assertEquals(PlayerEpiphanyData.createDefault(), state.get());
    }

    @Test
    void cappedCommandsReturnFailureIncludingSilentVariants() throws Exception {
        ConfigTestSupport.setLimit(0);
        when(player.getGameProfile()).thenReturn(new GameProfile(UUID.randomUUID(), "Test"));
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(anyInt())).thenReturn(true);
        EntityArgument playerArgument = EntityArgument.player();
        try (var arguments = mockStatic(EntityArgument.class)) {
            arguments.when(EntityArgument::player).thenReturn(playerArgument);
            arguments.when(() -> EntityArgument.getPlayer(any(), eq("player"))).thenReturn(player);
            var dispatcher = new CommandDispatcher<CommandSourceStack>();
            EpiphanyCommand.register(dispatcher);
            for (String command : List.of("aptitude add Test 10", "aptitude set Test 10", "aptitude fill Test",
                    "insight points add Test 10", "insight points set Test 10",
                    "aptitude add Test -silent 10", "aptitude set Test -silent 10", "aptitude fill Test -silent",
                    "insight points add Test -silent 10", "insight points set Test -silent 10")) {
                assertEquals(0, dispatcher.execute("epiphany " + command, source), command);
            }
        }
        verify(source, times(5)).sendFailure(any());
        verify(source, never()).sendSuccess(any(), anyBoolean());
        progress.verify(() -> EpiphanyPlayerData.set(eq(player), any(PlayerEpiphanyData.class)), never());
    }

    @Test
    void pointCommandFeedbackReportsActualClippedAmount() throws Exception {
        ConfigTestSupport.setLimit(5);
        state.set(state.get().withInsightPoints(4));
        when(player.getGameProfile()).thenReturn(new GameProfile(UUID.randomUUID(), "Test"));
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.hasPermission(anyInt())).thenReturn(true);
        var messages = new ArrayList<Component>();
        doAnswer(call -> {
            Supplier<Component> message = call.getArgument(0);
            messages.add(message.get());
            return null;
        }).when(source).sendSuccess(any(), anyBoolean());
        EntityArgument playerArgument = EntityArgument.player();
        try (var arguments = mockStatic(EntityArgument.class)) {
            arguments.when(EntityArgument::player).thenReturn(playerArgument);
            arguments.when(() -> EntityArgument.getPlayer(any(), eq("player"))).thenReturn(player);
            var dispatcher = new CommandDispatcher<CommandSourceStack>();
            EpiphanyCommand.register(dispatcher);
            assertEquals(1, dispatcher.execute("epiphany insight points add Test 3", source));
            assertEquals(5, state.get().insightPoints());
            assertEquals("1", ((TranslatableContents) messages.get(0).getContents()).getArgs()[0]);
            state.set(state.get().withInsightPoints(4));
            assertEquals(1, dispatcher.execute("epiphany insight points set Test 100", source));
            assertEquals("5", ((TranslatableContents) messages.get(messages.size() - 1).getContents()).getArgs()[0]);
        }
    }
}
