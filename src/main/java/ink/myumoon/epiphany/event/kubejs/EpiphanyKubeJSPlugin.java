package ink.myumoon.epiphany.event.kubejs;

import com.mojang.logging.LogUtils;
import ink.myumoon.epiphany.binding.EpiphanyBinding;
import ink.myumoon.epiphany.event.*;
import net.minecraftforge.common.MinecraftForge;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import org.slf4j.Logger;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * KubeJS Plugin for Epiphany.
 * <p>
 * <b>Events</b> (EpiphanyEvents.*) — all event listeners go through the native
 * KubeJS EventGroup. Pre events support {@code event.cancel()}.
 * <p>
 * <b>Binding</b> (Epiphany.*) — Manager API calls only (query/modify player data).
 * <p>
 * Plugin discovered via {@code kubejs.plugins.txt}.
 */
public final class EpiphanyKubeJSPlugin extends KubeJSPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean bridgesRegistered;

    @Override
    public void registerEvents() {
        KubeEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        if (!event.getType().isServer()) return;
        event.add("Epiphany", EpiphanyBinding.class);
    }

    @Override
    public void init() {
        registerBridges();
    }

    private static synchronized void registerBridges() {
        if (bridgesRegistered) return;
        // ─── Pre events (cancellable) ─────────────────────────────────────────

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModuleUnlockEvent.class, e -> {
            var result = KubeEvents.MODULE_UNLOCK.post(
                    new ModuleUnlockKubeEvent(e.getPlayer(), e.getModuleId()));
            if (result.interruptFalse()) e.setCanceled(true);
        });

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModuleSelectEvent.class, e -> {
            var result = KubeEvents.MODULE_SELECT.post(
                    new ModuleSelectKubeEvent(e.getPlayer(), e.getModuleId()));
            if (result.interruptFalse()) e.setCanceled(true);
        });

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModuleCompleteEvent.class, e -> {
            var result = KubeEvents.MODULE_COMPLETE.post(
                    new ModuleCompleteKubeEvent(e.getPlayer(), e.getModuleId()));
            if (result.interruptFalse()) e.setCanceled(true);
        });

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, InsightSelectEvent.class, e -> {
            var result = KubeEvents.INSIGHT_SELECT.post(
                    new InsightSelectKubeEvent(e.getPlayer(), e.getInsightId(), e.getModuleId()));
            if (result.interruptFalse()) e.setCanceled(true);
        });

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EpiphanyUnlockEvent.class, e -> {
            var result = KubeEvents.EPIPHANY_UNLOCK.post(
                    new EpiphanyUnlockKubeEvent(e.getPlayer(), e.getEpiphanyId()));
            if (result.interruptFalse()) e.setCanceled(true);
        });

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EpiphanySelectEvent.class, e -> {
            var result = KubeEvents.EPIPHANY_SELECT.post(
                    new EpiphanySelectKubeEvent(e.getPlayer(), e.getEpiphanyId()));
            if (result.interruptFalse()) e.setCanceled(true);
        });

        // ─── Post events ───────────────────────────────────────────────────────

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModuleUnlockedEvent.class, e ->
                KubeEvents.MODULE_UNLOCKED.post(
                        new ModuleUnlockedKubeEvent(e.getPlayer(), e.getModuleId(), e.isSilent())));

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModuleSelectedEvent.class, e ->
                KubeEvents.MODULE_SELECTED.post(
                        new ModuleSelectedKubeEvent(e.getPlayer(), e.getModuleId())));

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModuleCompletedEvent.class, e ->
                KubeEvents.MODULE_COMPLETED.post(
                        new ModuleCompletedKubeEvent(e.getPlayer(), e.getModuleId())));

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, InsightSelectedEvent.class, e ->
                KubeEvents.INSIGHT_SELECTED.post(
                        new InsightSelectedKubeEvent(e.getPlayer(), e.getInsightId(), e.getModuleId())));

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EpiphanyUnlockedEvent.class, e ->
                KubeEvents.EPIPHANY_UNLOCKED.post(
                        new EpiphanyUnlockedKubeEvent(e.getPlayer(), e.getEpiphanyId(), e.isSilent())));

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EpiphanySelectedEvent.class, e ->
                KubeEvents.EPIPHANY_SELECTED.post(
                        new EpiphanySelectedKubeEvent(e.getPlayer(), e.getEpiphanyId())));

        // ─── Standalone events ─────────────────────────────────────────────────

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AptitudeChangedEvent.class, e ->
                KubeEvents.APTITUDE_CHANGED.post(
                        new AptitudeChangedKubeEvent(e.getPlayer(), e.getOldAptitude(), e.getNewAptitude())));

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AptitudeLevelUpEvent.class, e ->
                KubeEvents.APTITUDE_LEVEL_UP.post(
                        new AptitudeLevelUpKubeEvent(e.getPlayer(), e.getNewInsightPoints())));

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, InsightPointsChangedEvent.class, e ->
                KubeEvents.INSIGHT_POINTS_CHANGED.post(
                        new InsightPointsChangedKubeEvent(e.getPlayer(), e.getOldValue(), e.getNewValue())));
        bridgesRegistered = true;
        LOGGER.info("Registered Epiphany KubeJS event bridges");
    }
}
