package ink.myumoon.epiphany.event.kubejs;

import com.mojang.logging.LogUtils;
import ink.myumoon.epiphany.binding.EpiphanyBinding;
import ink.myumoon.epiphany.event.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * KubeJS Plugin for Epiphany — Origins-JS pattern precisely.
 * <p>
 * Bridges in static{} (class-load time, before any KubeJS lifecycle).
 * Callbacks cleared in clearCaches() (KubeJS calls this before reload).
 * No registerEvents() — KubeJS 2101.7.2 does not call it.
 */
public final class EpiphanyKubeJSPlugin implements KubeJSPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean bridgesRegistered;

    public static final List<BiConsumer<ServerPlayer, ResourceLocation>> MODULE_UNLOCKED = new CopyOnWriteArrayList<>();
    public static final List<BiConsumer<ServerPlayer, ResourceLocation>> MODULE_SELECTED = new CopyOnWriteArrayList<>();
    public static final List<BiConsumer<ServerPlayer, ResourceLocation>> MODULE_COMPLETED = new CopyOnWriteArrayList<>();
    public static final List<BiConsumer<ServerPlayer, ResourceLocation>> EPIPHANY_UNLOCKED = new CopyOnWriteArrayList<>();
    public static final List<BiConsumer<ServerPlayer, ResourceLocation>> EPIPHANY_SELECTED = new CopyOnWriteArrayList<>();
    public static final List<Consumer<ServerPlayer>> APTITUDE_CHANGED = new CopyOnWriteArrayList<>();
    public static final List<Consumer<ServerPlayer>> INSIGHT_POINTS_CHANGED = new CopyOnWriteArrayList<>();

    @Override public void init() {}

    @Override
    public void registerBindings(BindingRegistry registry) {
        if (!registry.type().isServer()) return;
        registry.add("Epiphany", EpiphanyBinding.class);
        if (!bridgesRegistered) {
            registerBridges();
            bridgesRegistered = true;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void clearCaches() {}

    @Override
    public void beforeScriptsLoaded(dev.latvian.mods.kubejs.script.ScriptManager manager) {
        MODULE_UNLOCKED.clear();
        MODULE_SELECTED.clear();
        MODULE_COMPLETED.clear();
        EPIPHANY_UNLOCKED.clear();
        EPIPHANY_SELECTED.clear();
        APTITUDE_CHANGED.clear();
        INSIGHT_POINTS_CHANGED.clear();
    }

    private void registerBridges() {
        NeoForge.EVENT_BUS.addListener(ModuleUnlockedEvent.class, e ->
                MODULE_UNLOCKED.forEach(cb -> cb.accept(e.getPlayer(), e.getModuleId())));
        NeoForge.EVENT_BUS.addListener(ModuleSelectedEvent.class, e ->
                MODULE_SELECTED.forEach(cb -> cb.accept(e.getPlayer(), e.getModuleId())));
        NeoForge.EVENT_BUS.addListener(ModuleCompletedEvent.class, e ->
                MODULE_COMPLETED.forEach(cb -> cb.accept(e.getPlayer(), e.getModuleId())));
        NeoForge.EVENT_BUS.addListener(EpiphanyUnlockedEvent.class, e ->
                EPIPHANY_UNLOCKED.forEach(cb -> cb.accept(e.getPlayer(), e.getEpiphanyId())));
        NeoForge.EVENT_BUS.addListener(EpiphanySelectedEvent.class, e ->
                EPIPHANY_SELECTED.forEach(cb -> cb.accept(e.getPlayer(), e.getEpiphanyId())));
        NeoForge.EVENT_BUS.addListener(AptitudeChangedEvent.class, e ->
                APTITUDE_CHANGED.forEach(cb -> cb.accept(e.getPlayer())));
        NeoForge.EVENT_BUS.addListener(InsightPointsChangedEvent.class, e ->
                INSIGHT_POINTS_CHANGED.forEach(cb -> cb.accept(e.getPlayer())));
    }
}
