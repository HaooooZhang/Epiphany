package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.event.EpiphanyUnlockedEvent;
import ink.myumoon.epiphany.event.InsightPointsChangedEvent;
import ink.myumoon.epiphany.event.ModuleUnlockedEvent;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Sends in-game notifications (chat message + advancement sound) to the player
 * when noteworthy progression events fire.
 * <p>
 * Note: This is a temporary server-side notification mechanism. The plan is to
 * replace {@code sendSystemMessage} with a client-side Toast popup in a later
 * UI phase; the lang keys and config flags built here will carry over.
 * <p>
 * Unlock notifications are validated against the registry: entries whose
 * {@code initial_state} is {@code selectable} are treated as always-available
 * and NOT notified (an unlock event for them means nothing new for the player).
 * Entries missing from the registry (orphaned data) are also silenced.
 */
@EventBusSubscriber(modid = Epiphany.MODID)
public final class NotificationListener {

    private NotificationListener() {
    }

    @SubscribeEvent
    static void onInsightPointsChanged(InsightPointsChangedEvent event) {
        if (!Config.NOTIFY_INSIGHT_POINTS.get()) return;
        if (!event.isGain()) return; // only notify gains, not spends

        var sp = event.getPlayer();
        Component msg = Component.translatable("epiphany.notify.insight_points.gained",
                        event.getDelta(), event.getNewValue())
                .withStyle(ChatFormatting.WHITE);
        send(sp, msg);
    }

    @SubscribeEvent
    static void onModuleUnlocked(ModuleUnlockedEvent event) {
        if (event.isSilent()) return;
        if (!Config.NOTIFY_MODULE_UNLOCK.get()) return;

        var sp = event.getPlayer();
        ModuleData module = moduleData(sp, event.getModuleId());
        if (module == null || module.initialState() == InitialState.SELECTABLE) return;

        Component msg = Component.translatable("epiphany.notify.module.unlocked",
                        module.effectiveName(event.getModuleId()))
                .withStyle(ChatFormatting.BOLD);
        send(sp, msg);
    }

    @SubscribeEvent
    static void onEpiphanyUnlocked(EpiphanyUnlockedEvent event) {
        if (event.isSilent()) return;
        if (!Config.NOTIFY_EPIPHANY_UNLOCK.get()) return;

        var sp = event.getPlayer();
        EpiphanyData epiphany = epiphanyData(sp, event.getEpiphanyId());
        if (epiphany == null || epiphany.initialState() == InitialState.SELECTABLE) return;

        Component msg = Component.translatable("epiphany.notify.epiphany.unlocked",
                        epiphany.effectiveName(event.getEpiphanyId()))
                .withStyle(ChatFormatting.BOLD);
        send(sp, msg);
    }

    // ============================================================
    // helpers
    // ============================================================

    private static void send(ServerPlayer sp, Component msg) {
        sp.sendSystemMessage(msg);
        sp.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.7F, 1.0F);
    }

    /** Looks up the ModuleData behind an id; {@code null} when not in the registry. */
    private static ModuleData moduleData(ServerPlayer sp, ResourceLocation id) {
        return sp.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY)
                .get(id);
    }

    /** Looks up the EpiphanyData behind an id; {@code null} when not in the registry. */
    private static EpiphanyData epiphanyData(ServerPlayer sp, ResourceLocation id) {
        return sp.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY)
                .get(id);
    }
}
