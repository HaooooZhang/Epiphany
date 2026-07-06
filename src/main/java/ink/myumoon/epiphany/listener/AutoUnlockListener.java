package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.event.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Epiphany.MODID)
public final class AutoUnlockListener {

    private static int tickCounter;

    private AutoUnlockListener() {
    }

    // ============================================================
    // Periodic fallback — covers all query-type Conditions
    // ============================================================

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter % 10 != 0) return; // twice per second
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            auto(player);
        }
    }

    // ============================================================
    // Event-driven triggers — immediate feedback
    // ============================================================

    @SubscribeEvent
    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    static void onModuleSelected(ModuleSelectedEvent event) {
        auto(event.getPlayer());
    }

    @SubscribeEvent
    static void onModuleCompleted(ModuleCompletedEvent event) {
        auto(event.getPlayer());
    }

    @SubscribeEvent
    static void onInsightSelected(InsightSelectedEvent event) {
        auto(event.getPlayer());
    }

    @SubscribeEvent
    static void onEpiphanySelected(EpiphanySelectedEvent event) {
        auto(event.getPlayer());
    }

    private static void auto(ServerPlayer player) {
        ModuleManager.checkAutoUnlock(player);
        EpiphanyManager.checkAutoUnlock(player);
    }
}
