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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
 *   <li>{@code epiphany:pickup_item}          — pick up an item (PlayerEvent.ItemPickupEvent and the partial-pickup fallback)</li>
 *   <li>{@code epiphany:craft_item}           — craft an item (PlayerEvent.ItemCraftedEvent)</li>
 *   <li>{@code epiphany:smelt_item}           — take a smelted item (PlayerEvent.ItemSmeltedEvent)</li>
 * </ul>
 * Biome-enter behavior ({@code epiphany:enter_biome}) lives in {@link AptitudeStateListener}
 * (requires persistent state tracking + chunk move + tick polling).
 * <p>
 * FTBQ-related behaviors ({@code ftbq_quest_complete}/{@code ftbq_chapter_complete}) are
 * wired in {@code FTBQInternal} as a soft dependency.
 * <p>
 * <strong>Third-party mods</strong>: do NOT add methods here. Instead register your
 * own Forge listener and call {@link AptitudeSourceManager#grant}. See {@code Docs/README.md}.
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
    public static final ResourceLocation PICKUP_ITEM =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "pickup_item");
    public static final ResourceLocation CRAFT_ITEM =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "craft_item");
    public static final ResourceLocation SMELT_ITEM =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "smelt_item");

    private static final Map<ServerPlayer, Map<ItemEntity, PickupAttempt>> PENDING_PICKUPS = new IdentityHashMap<>();

    private AptitudeGainListener() {
    }

    // ============================================================
    // Vanilla / Forge events
    // ============================================================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;  // ignore automation mods (Create block-breakers, etc.)

        EntityType<?> type = event.getEntity().getType();
        ResourceLocation targetId = BuiltInRegistries.ENTITY_TYPE.getKey(type);

        AptitudeSourceManager.grant(sp, KILL_ENTITY, targetId, BuiltInRegistries.ENTITY_TYPE);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;  // ignore automation mods (Create block-breakers, etc.)

        ResourceLocation targetId = event.getState().getBlockHolder().unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);
        if (targetId == null) return;

        AptitudeSourceManager.grant(sp, MINE_BLOCK, targetId, BuiltInRegistries.BLOCK);
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;

        ResourceLocation targetId = event.getAdvancement().getId();
        AptitudeSourceManager.grant(sp, ADVANCEMENT_EARN, targetId, null);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;

        ResourceLocation targetId = event.getTo().location();
        AptitudeSourceManager.grant(sp, ENTER_DIMENSION, targetId, null);
    }

    @SubscribeEvent
    public static void onXpLevelChange(PlayerXpEvent.LevelChange event) {
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPickupAttempt(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || isFakePlayer(player)) return;
        ItemEntity item = event.getItem();
        ItemStack stack = item.getItem();
        if (stack.isEmpty()) return;
        PENDING_PICKUPS.computeIfAbsent(player, ignored -> new LinkedHashMap<>())
                .put(item, new PickupAttempt(event, stack, stack.getCount()));
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || isFakePlayer(player)) return;
        var attempts = PENDING_PICKUPS.get(player);
        if (attempts != null) attempts.remove(event.getOriginalEntity());

        ItemStack pickedUp = event.getStack();
        if (!pickedUp.isEmpty()) grantPickup(player, pickedUp);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var attempts = PENDING_PICKUPS.remove(player);
        if (attempts == null) return;

        // Forge 47.4.28 skips ItemPickupEvent when Inventory.add moves some items and
        // returns false on the remaining stack. The player's collision scan ends before
        // this tick event, so check the remaining stack here before another player can pick it up.
        var partialPickups = attempts.values().stream()
                .filter(PickupAttempt::wasPartialPickup)
                .map(PickupAttempt::stack)
                .toList();
        for (ItemStack stack : partialPickups) grantPickup(player, stack);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING_PICKUPS.clear();
    }

    private static void grantPickup(ServerPlayer player, ItemStack stack) {
        ResourceLocation targetId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        AptitudeSourceManager.grant(player, PICKUP_ITEM, targetId, BuiltInRegistries.ITEM);
    }

    private record PickupAttempt(EntityItemPickupEvent event, ItemStack stack, int countBefore) {
        boolean wasPartialPickup() {
            return !event.isCanceled() && event.getResult() != Event.Result.ALLOW
                    && !event.getItem().isRemoved() && event.getItem().getItem() == stack
                    && !stack.isEmpty() && stack.getCount() < countBefore;
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;

        ItemStack stack = event.getCrafting();
        if (stack.isEmpty()) return;

        ResourceLocation targetId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        AptitudeSourceManager.grant(sp, CRAFT_ITEM, targetId, BuiltInRegistries.ITEM);
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (isFakePlayer(sp)) return;

        ItemStack stack = event.getSmelting();
        if (stack.isEmpty()) return;

        ResourceLocation targetId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        AptitudeSourceManager.grant(sp, SMELT_ITEM, targetId, BuiltInRegistries.ITEM);
    }

    /**
     * Identifies NeoForge {@link net.minecraftforge.common.util.FakePlayer} instances
     * so they don't trigger aptitude grants from automation mods (Create block-breakers,
     * Quartz automation, mob grinders driven by FakePlayer, etc.).
     * <p>
     */
    private static boolean isFakePlayer(ServerPlayer sp) {
        return sp instanceof FakePlayer;
    }

    // ============================================================
    // Epiphany-internal events
    // ============================================================

    @SubscribeEvent
    public static void onModuleSelected(ModuleSelectedEvent event) {
        AptitudeSourceManager.grant(event.getPlayer(), MODULE_SELECTED, event.getModuleId(), null);
    }

    @SubscribeEvent
    public static void onModuleCompleted(ModuleCompletedEvent event) {
        AptitudeSourceManager.grant(event.getPlayer(), MODULE_COMPLETED, event.getModuleId(), null);
    }

    @SubscribeEvent
    public static void onInsightSelected(InsightSelectedEvent event) {
        AptitudeSourceManager.grant(event.getPlayer(), INSIGHT_SELECTED, event.getInsightId(), null);
    }

    @SubscribeEvent
    public static void onEpiphanySelected(EpiphanySelectedEvent event) {
        AptitudeSourceManager.grant(event.getPlayer(), EPIPHANY_SELECTED, event.getEpiphanyId(), null);
    }
}
