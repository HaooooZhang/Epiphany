package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.AptitudeAmount;
import ink.myumoon.epiphany.event.AptitudeChangedEvent;
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
import java.util.function.Consumer;

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
}
