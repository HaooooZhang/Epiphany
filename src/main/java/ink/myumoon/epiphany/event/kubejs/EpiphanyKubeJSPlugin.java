package ink.myumoon.epiphany.event.kubejs;

import com.mojang.logging.LogUtils;
import ink.myumoon.epiphany.binding.EpiphanyBinding;
import ink.myumoon.epiphany.event.*;
import net.neoforged.neoforge.common.NeoForge;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import org.slf4j.Logger;

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
public final class EpiphanyKubeJSPlugin implements KubeJSPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean bridgesRegistered;

    @Override public void init() {}

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(KubeEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry registry) {
        if (!registry.type().isServer()) return;
        registry.add("Epiphany", EpiphanyBinding.class);
        if (!bridgesRegistered) {
            registerBridges();
            bridgesRegistered = true;
        }
    }

    @Override
    public void clearCaches() {}

    private void registerBridges() {
        // ─── Pre events (cancellable) ─────────────────────────────────────────

        NeoForge.EVENT_BUS.addListener(ModuleUnlockEvent.class, e -> {
            var result = KubeEvents.MODULE_UNLOCK.post(
                    new ModuleUnlockKubeEvent(e.getPlayer(), e.getModuleId()));
            result.applyCancel(e);
        });

        NeoForge.EVENT_BUS.addListener(ModuleSelectEvent.class, e -> {
            var result = KubeEvents.MODULE_SELECT.post(
                    new ModuleSelectKubeEvent(e.getPlayer(), e.getModuleId()));
            result.applyCancel(e);
        });

        NeoForge.EVENT_BUS.addListener(ModuleCompleteEvent.class, e -> {
            var result = KubeEvents.MODULE_COMPLETE.post(
                    new ModuleCompleteKubeEvent(e.getPlayer(), e.getModuleId()));
            result.applyCancel(e);
        });

        NeoForge.EVENT_BUS.addListener(InsightSelectEvent.class, e -> {
            var result = KubeEvents.INSIGHT_SELECT.post(
                    new InsightSelectKubeEvent(e.getPlayer(), e.getInsightId(), e.getModuleId()));
            result.applyCancel(e);
        });

        NeoForge.EVENT_BUS.addListener(EpiphanyUnlockEvent.class, e -> {
            var result = KubeEvents.EPIPHANY_UNLOCK.post(
                    new EpiphanyUnlockKubeEvent(e.getPlayer(), e.getEpiphanyId()));
            result.applyCancel(e);
        });

        NeoForge.EVENT_BUS.addListener(EpiphanySelectEvent.class, e -> {
            var result = KubeEvents.EPIPHANY_SELECT.post(
                    new EpiphanySelectKubeEvent(e.getPlayer(), e.getEpiphanyId()));
            result.applyCancel(e);
        });

        // ─── Post events ───────────────────────────────────────────────────────

        NeoForge.EVENT_BUS.addListener(ModuleUnlockedEvent.class, e ->
                KubeEvents.MODULE_UNLOCKED.post(
                        new ModuleUnlockedKubeEvent(e.getPlayer(), e.getModuleId(), e.isSilent())));

        NeoForge.EVENT_BUS.addListener(ModuleSelectedEvent.class, e ->
                KubeEvents.MODULE_SELECTED.post(
                        new ModuleSelectedKubeEvent(e.getPlayer(), e.getModuleId())));

        NeoForge.EVENT_BUS.addListener(ModuleCompletedEvent.class, e ->
                KubeEvents.MODULE_COMPLETED.post(
                        new ModuleCompletedKubeEvent(e.getPlayer(), e.getModuleId())));

        NeoForge.EVENT_BUS.addListener(InsightSelectedEvent.class, e ->
                KubeEvents.INSIGHT_SELECTED.post(
                        new InsightSelectedKubeEvent(e.getPlayer(), e.getInsightId(), e.getModuleId())));

        NeoForge.EVENT_BUS.addListener(EpiphanyUnlockedEvent.class, e ->
                KubeEvents.EPIPHANY_UNLOCKED.post(
                        new EpiphanyUnlockedKubeEvent(e.getPlayer(), e.getEpiphanyId(), e.isSilent())));

        NeoForge.EVENT_BUS.addListener(EpiphanySelectedEvent.class, e ->
                KubeEvents.EPIPHANY_SELECTED.post(
                        new EpiphanySelectedKubeEvent(e.getPlayer(), e.getEpiphanyId())));

        // ─── Standalone events ─────────────────────────────────────────────────

        NeoForge.EVENT_BUS.addListener(AptitudeChangedEvent.class, e ->
                KubeEvents.APTITUDE_CHANGED.post(
                        new AptitudeChangedKubeEvent(e.getPlayer(), e.getOldAptitude(), e.getNewAptitude())));

        NeoForge.EVENT_BUS.addListener(AptitudeLevelUpEvent.class, e ->
                KubeEvents.APTITUDE_LEVEL_UP.post(
                        new AptitudeLevelUpKubeEvent(e.getPlayer(), e.getNewInsightPoints())));

        NeoForge.EVENT_BUS.addListener(InsightPointsChangedEvent.class, e ->
                KubeEvents.INSIGHT_POINTS_CHANGED.post(
                        new InsightPointsChangedKubeEvent(e.getPlayer(), e.getOldValue(), e.getNewValue())));
    }
}
