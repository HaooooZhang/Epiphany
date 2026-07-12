package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.event.EpiphanyUnlockedEvent;
import ink.myumoon.epiphany.event.InsightPointsChangedEvent;
import ink.myumoon.epiphany.event.ModuleUnlockedEvent;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
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
 * Selectable modules/epiphanies are intentionally NOT notified because the
 * {@code *UnlockedEvent} only fires on a false{@code ->}true state transition —
 * SELECTABLE entries are never persisted into the player's attachment state, so
 * they never produce an unlock event in the first place.
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
        Component name = moduleName(sp, event.getModuleId());
        Component msg = Component.translatable("epiphany.notify.module.unlocked", name)
                .withStyle(ChatFormatting.BOLD);
        send(sp, msg);
    }

    @SubscribeEvent
    static void onEpiphanyUnlocked(EpiphanyUnlockedEvent event) {
        if (event.isSilent()) return;
        if (!Config.NOTIFY_EPIPHANY_UNLOCK.get()) return;

        var sp = event.getPlayer();
        Component name = epiphanyName(sp, event.getEpiphanyId());
        Component msg = Component.translatable("epiphany.notify.epiphany.unlocked", name)
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

    /**
     * Looks up the datapack-defined display name for a Module; falls back to the raw
     * registry id as a Component when the entry has no {@code name} field.
     */
    private static Component moduleName(ServerPlayer sp, ResourceLocation id) {
        Registry<ModuleData> registry = sp.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
        ModuleData module = registry.get(id);
        if (module != null) {
            return module.effectiveName(id);
        }
        return Component.literal(id.toString()).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Looks up the datapack-defined display name for an Epiphany; falls back to the
     * raw registry id as a Component when the entry has no {@code name} field.
     */
    private static Component epiphanyName(ServerPlayer sp, ResourceLocation id) {
        Registry<EpiphanyData> registry = sp.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);
        EpiphanyData epiphany = registry.get(id);
        if (epiphany != null) {
            return epiphany.effectiveName(id);
        }
        return Component.literal(id.toString()).withStyle(ChatFormatting.GRAY);
    }
}
