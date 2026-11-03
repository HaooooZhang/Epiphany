package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerState;
import ink.myumoon.epiphany.Config;
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
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.loading.LoadingModList;
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

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        // Vanilla registries query the loading mod list while collecting feature flags.
        // These isolated JVM tests deliberately load no mods.
        try (var loading = mockStatic(LoadingModList.class)) {
            loading.when(LoadingModList::get).thenReturn(mock(LoadingModList.class));
            Bootstrap.bootStrap();
        }
        NeoForge.EVENT_BUS.start();
    }

    @BeforeEach
    void setUp() {
        ConfigTestSupport.reset();
        player = mock(ServerPlayer.class);
        random = mock(RandomSource.class);
        attribute = mock(AttributeInstance.class);
        state = new AtomicReference<>(PlayerEpiphanyData.createDefault());
        when(player.getRandom()).thenReturn(random);
        when(player.getAttribute(EpiphanyAttributes.APTITUDE_GAIN_MULTIPLIER)).thenReturn(attribute);
        when(attribute.getValue()).thenReturn(1.0);
        when(player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)).thenAnswer(call -> state.get());
        doAnswer(call -> state.getAndSet(call.getArgument(1)))
                .when(player).setData(eq(EpiphanyAttachmentTypes.EPIPHANY_DATA), any(PlayerEpiphanyData.class));
    }

    private MockedStatic<AptitudeFormula> price(long required) {
        MockedStatic<AptitudeFormula> formula = mockStatic(AptitudeFormula.class);
        formula.when(() -> AptitudeFormula.calcRequiredAptitude(anyLong(), anyInt())).thenReturn(required);
        return formula;
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
        verify(player, times(1)).setData(eq(EpiphanyAttachmentTypes.EPIPHANY_DATA), any(PlayerEpiphanyData.class));
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
        verify(player, never()).setData(eq(EpiphanyAttachmentTypes.EPIPHANY_DATA), any(PlayerEpiphanyData.class));
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
        NeoForge.EVENT_BUS.addListener(AptitudeChangedEvent.class, listener);
        try (var ignored = price(10)) {
            assertTrue(AptitudeSourceManager.grantResolved(player,
                    new AptitudeSourceResolver.Resolution(true, 25, CLAIM)));
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
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
        verify(player, never()).setData(eq(EpiphanyAttachmentTypes.EPIPHANY_DATA), any(PlayerEpiphanyData.class));
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
        verify(player, never()).setData(eq(EpiphanyAttachmentTypes.EPIPHANY_DATA), any(PlayerEpiphanyData.class));
    }

    @Test
    void finiteCapSettlesOnlyRemainingPointsAndDiscardsOverflow() {
        Config.MAX_TOTAL_INSIGHT_POINTS.set(3);
        state.set(state.get().withInsightPoints(1).withTotalInsightPointsSpent(1).withAptitude(5));
        AtomicInteger levels = new AtomicInteger();
        var changes = new ArrayList<String>();
        Consumer<AptitudeLevelUpEvent> level = event -> {
            if (event.getPlayer() == player) levels.incrementAndGet();
        };
        Consumer<InsightPointsChangedEvent> points = event -> {
            if (event.getPlayer() == player) changes.add("points");
        };
        NeoForge.EVENT_BUS.addListener(AptitudeLevelUpEvent.class, level);
        NeoForge.EVENT_BUS.addListener(InsightPointsChangedEvent.class, points);
        try {
            assertTrue(AptitudeSourceManager.grantResolved(player,
                    new AptitudeSourceResolver.Resolution(true, 100, CLAIM)));
        } finally {
            NeoForge.EVENT_BUS.unregister(level);
            NeoForge.EVENT_BUS.unregister(points);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(0);
        var resolution = new AptitudeSourceResolver.Resolution(true,
                new AptitudeAmount(1, 10), new AptitudeAmount(1, 10), CLAIM);
        AtomicInteger events = new AtomicInteger();
        Consumer<AptitudeChangedEvent> listener = event -> {
            if (event.getPlayer() == player) events.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(AptitudeChangedEvent.class, listener);
        try {
            assertFalse(AptitudeSourceManager.grantResolved(player, resolution));
            assertEquals(0, AptitudeManager.fillAptitude(player));
            AptitudeManager.addAptitudeWithMultiplier(player, 100);
            AptitudeManager.setAptitude(player, 100);
            AptitudeManager.addInsightPoints(player, 100);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        verifyNoInteractions(random, attribute);
        verify(player, never()).setData(eq(EpiphanyAttachmentTypes.EPIPHANY_DATA), any(PlayerEpiphanyData.class));
        assertEquals(0, events.get());
    }

    @Test
    void loweringLimitPreservesOldDataAndRaisingLimitResumesGrowth() {
        state.set(state.get().withInsightPoints(3).withTotalInsightPointsSpent(4).withAptitude(8));
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
        PlayerEpiphanyData before = state.get();
        assertFalse(AptitudeManager.grantAptitude(player, 100, CLAIM));
        AptitudeManager.setInsightPoints(player, 10);
        assertEquals(before, state.get());
        AptitudeManager.setInsightPoints(player, 2); // A reduction must not silently clip to L - spent = 1.
        assertEquals(2, state.get().insightPoints());
        assertEquals(8, state.get().aptitude());
        Config.MAX_TOTAL_INSIGHT_POINTS.set(7);
        assertTrue(AptitudeManager.grantAptitude(player, 100, CLAIM));
        assertEquals(3, state.get().insightPoints());
        assertEquals(0, state.get().aptitude());
    }

    @Test
    void spendingAndRefundingCannotReopenBudget() {
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
        state.set(state.get().withTotalInsightPointsSpent(3).withInsightPoints(1).withAptitude(8));
        AptitudeManager.addInsightPoints(player, Integer.MAX_VALUE);
        assertEquals(2, state.get().insightPoints());
        assertEquals(0, state.get().aptitude());
        Config.MAX_TOTAL_INSIGHT_POINTS.set(-1);
        AptitudeManager.addInsightPoints(player, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, state.get().insightPoints());
        assertEquals((long) Integer.MAX_VALUE + 3, AptitudeManager.getTotalInsightPoints(player));
        AptitudeManager.addInsightPoints(player, Integer.MIN_VALUE);
        assertEquals(0, state.get().insightPoints());
    }

    @Test
    void fillRespectsRealLinearPriceAndStopsAtLastPoint() {
        Config.MAX_TOTAL_INSIGHT_POINTS.set(2);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
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
        NeoForge.EVENT_BUS.addListener(InsightPointsChangedEvent.class, listener);
        try {
            AptitudeManager.applyInsightPointsReward(player, "test", 3);
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        assertEquals(0, state.get().insightPoints());
        assertEquals(2, observed.get());
    }

    @Test
    void clippedZeroRewardsDoNotRemoveUnrelatedPointsAndRepeatedApplicationsUnwindInOrder() {
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(1);
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
        var encoded = PlayerEpiphanyData.CODEC.encodeStart(JsonOps.INSTANCE, state.get()).getOrThrow();
        assertEquals(state.get(), PlayerEpiphanyData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            PlayerEpiphanyData.STREAM_CODEC.encode(buffer, state.get());
            assertEquals(state.get(), PlayerEpiphanyData.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
        encoded.getAsJsonObject().remove("insightPointRewards");
        PlayerEpiphanyData legacy = PlayerEpiphanyData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
        RewardListHelper.applyInsight(module.onSelectReward(), player, moduleId, "on_select_reward");
        assertEquals(4, state.get().insightPoints());
        Config.MAX_TOTAL_INSIGHT_POINTS.set(2);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(1);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
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
        Config.MAX_TOTAL_INSIGHT_POINTS.set(0);
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
        verify(player, never()).setData(eq(EpiphanyAttachmentTypes.EPIPHANY_DATA), any(PlayerEpiphanyData.class));
    }

    @Test
    void pointCommandFeedbackReportsActualClippedAmount() throws Exception {
        Config.MAX_TOTAL_INSIGHT_POINTS.set(5);
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
            assertEquals("1", ((TranslatableContents) messages.getFirst().getContents()).getArgs()[0]);
            state.set(state.get().withInsightPoints(4));
            assertEquals(1, dispatcher.execute("epiphany insight points set Test 100", source));
            assertEquals("5", ((TranslatableContents) messages.getLast().getContents()).getArgs()[0]);
        }
    }
}
