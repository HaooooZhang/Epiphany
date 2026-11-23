package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.event.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;

@EventBusSubscriber(modid = Epiphany.MODID)
public final class AutoUnlockListener {

    private static int tickCounter;

    private AutoUnlockListener() {
    }

    // ============================================================
    // Periodic fallback — covers all query-type Conditions
    // ============================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter % 10 != 0) return; // twice per second
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            auto(player, true); // skip event-driven conditions
        }
    }

    // ============================================================
    // Event-driven triggers — immediate feedback
    // ============================================================

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    public static void onItemBroken(PlayerDestroyItemEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer sp) auto(sp);
    }

    @SubscribeEvent
    public static void onModuleSelected(ModuleSelectedEvent event) {
        auto(event.getPlayer());
    }

    @SubscribeEvent
    public static void onModuleCompleted(ModuleCompletedEvent event) {
        auto(event.getPlayer());
    }

    @SubscribeEvent
    public static void onInsightSelected(InsightSelectedEvent event) {
        auto(event.getPlayer());
    }

    @SubscribeEvent
    public static void onEpiphanySelected(EpiphanySelectedEvent event) {
        auto(event.getPlayer());
    }

    private static void auto(ServerPlayer player) {
        auto(player, false);
    }

    private static void auto(ServerPlayer player, boolean skipEventDriven) {
        ModuleManager.checkAutoUnlock(player, skipEventDriven);
        EpiphanyManager.checkAutoUnlock(player, skipEventDriven);
    }
}
