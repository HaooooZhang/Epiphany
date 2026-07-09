package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.AptitudeSourceManager;
import ink.myumoon.epiphany.event.EpiphanySelectedEvent;
import ink.myumoon.epiphany.event.InsightSelectedEvent;
import ink.myumoon.epiphany.event.ModuleCompletedEvent;
import ink.myumoon.epiphany.event.ModuleSelectedEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Epiphany's built-in game-event listeners for aptitude sources.
 * <p>
 * Wires built-in behaviors defined in {@code data/epiphany/epiphany/aptitude/}:
 * <ul>
 *   <li>{@code epiphany:kill_entity}          — kill entity (LivingDeathEvent)</li>
 *   <li>{@code epiphany:mine_block}           — break block (BlockBreakEvent)</li>
 *   <li>{@code epiphany:advancement_earn}     — earn advancement (AdvancementEarnEvent)</li>
 *   <li>{@code epiphany:enter_dimension}      — change dimension (PlayerChangedDimensionEvent)</li>
 *   <li>{@code epiphany:experience_level_up}  — earn XP levels (PlayerXpEvent.LevelChange)</li>
 *   <li>{@code epiphany:module_selected}      — select module (ModuleSelectedEvent)</li>
 *   <li>{@code epiphany:module_completed}     — complete module (ModuleCompletedEvent)</li>
 *   <li>{@code epiphany:insight_selected}     — select insight (InsightSelectedEvent)</li>
 *   <li>{@code epiphany:epiphany_selected}    — select epiphany (EpiphanySelectedEvent)</li>
 * </ul>
 * Biome-enter behavior ({@code epiphany:enter_biome}) lives in {@link AptitudeStateListener}
 * (requires persistent state tracking + chunk move + tick polling).
 * <p>
 * FTBQ-related behaviors ({@code ftbq_quest_complete}/{@code ftbq_chapter_complete}) are
 * wired in {@code FTBQInternal} as a soft dependency.
 * <p>
 * <strong>Third-party mods</strong>: do NOT add methods here. Instead register your
 * own NeoForge listener and call {@link AptitudeSourceManager#grant}. See {@code Docs/README.md}.
 */
@EventBusSubscriber(modid = Epiphany.MODID)
public final class AptitudeGainListener {

    public static final ResourceLocation KILL_ENTITY =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "kill_entity");
    public static final ResourceLocation MINE_BLOCK =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "mine_block");
    public static final ResourceLocation ADVANCEMENT_EARN =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "advancement_earn");
    public static final ResourceLocation ENTER_DIMENSION =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "enter_dimension");
    public static final ResourceLocation MODULE_SELECTED =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "module_selected");
    public static final ResourceLocation MODULE_COMPLETED =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "module_completed");
    public static final ResourceLocation INSIGHT_SELECTED =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "insight_selected");
    public static final ResourceLocation EPIPHANY_SELECTED =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "epiphany_selected");
    public static final ResourceLocation EXPERIENCE_LEVEL_UP =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "experience_level_up");

    private AptitudeGainListener() {
    }

    // ============================================================
    // Vanilla / NeoForge events
    // ============================================================

    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;  // ignore automation mods (Create block-breakers, etc.)

        EntityType<?> type = event.getEntity().getType();
        ResourceLocation targetId = BuiltInRegistries.ENTITY_TYPE.getKey(type);

        AptitudeSourceManager.grant(sp, KILL_ENTITY, targetId, BuiltInRegistries.ENTITY_TYPE);
    }

    @SubscribeEvent
    static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;  // ignore automation mods (Create block-breakers, etc.)

        ResourceLocation targetId = event.getState().getBlockHolder().unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);
        if (targetId == null) return;

        AptitudeSourceManager.grant(sp, MINE_BLOCK, targetId, BuiltInRegistries.BLOCK);
    }

    @SubscribeEvent
    static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;

        ResourceLocation targetId = event.getAdvancement().id();
        AptitudeSourceManager.grant(sp, ADVANCEMENT_EARN, targetId, null);
    }

    @SubscribeEvent
    static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;

        ResourceLocation targetId = event.getTo().location();
        AptitudeSourceManager.grant(sp, ENTER_DIMENSION, targetId, null);
    }

    @SubscribeEvent
    static void onXpLevelChange(PlayerXpEvent.LevelChange event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;

        int levels = event.getLevels();
        if (levels <= 0) return;                       // 只发放升级，降级不退阅历
        // JSON 只用 "default"；每升 1 级发放一次 default。target 占位、registry = null。
        ResourceLocation noTarget = ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "_");
        for (int i = 0; i < levels; i++) {
            AptitudeSourceManager.grant(sp, EXPERIENCE_LEVEL_UP, noTarget, null);
        }
    }

    /**
     * Identifies NeoForge {@link net.neoforged.neoforge.common.util.FakePlayer} instances
     * so they don't trigger aptitude grants from automation mods (Create block-breakers,
     * Quartz automation, mob grinders driven by FakePlayer, etc.).
     * <p>
     * Real players always have a network connection; FakePlayer gets a synthetic
     * {@link ServerPlayer#getConnection()} that is a {@code FakePlayerNetHandler},
     * but the simplest cross-version-stable check is the explicit type test.
     */
    private static boolean isFakePlayer(ServerPlayer sp) {
        return sp instanceof FakePlayer;
    }

    // ============================================================
    // Epiphany-internal events
    // ============================================================

    @SubscribeEvent
    static void onModuleSelected(ModuleSelectedEvent event) {
        AptitudeSourceManager.grant(event.getPlayer(), MODULE_SELECTED, event.getModuleId(), null);
    }

    @SubscribeEvent
    static void onModuleCompleted(ModuleCompletedEvent event) {
        AptitudeSourceManager.grant(event.getPlayer(), MODULE_COMPLETED, event.getModuleId(), null);
    }

    @SubscribeEvent
    static void onInsightSelected(InsightSelectedEvent event) {
        AptitudeSourceManager.grant(event.getPlayer(), INSIGHT_SELECTED, event.getInsightId(), null);
    }

    @SubscribeEvent
    static void onEpiphanySelected(EpiphanySelectedEvent event) {
        AptitudeSourceManager.grant(event.getPlayer(), EPIPHANY_SELECTED, event.getEpiphanyId(), null);
    }
}
