package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.event.*;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Set;

/**
 * Manages Module lifecycle: unlock, select, complete, and auto-unlock via conditions.
 */
public final class ModuleManager {

    private ModuleManager() {
    }

    // ============================================================
    // Registry helper
    // ============================================================

    private static Registry<ModuleData> moduleRegistry(ServerPlayer player) {
        return player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
    }

    private static Registry<InsightData> insightRegistry(ServerPlayer player) {
        return player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
    }

    /**
     * Gets or creates state. Selectable modules are initialized with unlocked=true.
     */
    private static ModulePlayerState ensureState(PlayerEpiphanyData data, ServerPlayer player,
                                                  ResourceLocation id, ModuleData module) {
        ModulePlayerState state = data.modules().get(id);
        if (state == null && module.initialState() == InitialState.SELECTABLE) {
            state = new ModulePlayerState(true, false, false, java.util.Set.of());
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withModuleState(id, state));
        }
        return state != null ? state : ModulePlayerState.createDefault();
    }

    // ============================================================
    // State queries
    // ============================================================

    public static boolean isUnlocked(ServerPlayer player, ResourceLocation moduleId) {
        ModulePlayerState state = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .modules().get(moduleId);
        return state != null && state.unlocked();
    }

    public static boolean isSelected(ServerPlayer player, ResourceLocation moduleId) {
        ModulePlayerState state = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .modules().get(moduleId);
        return state != null && state.selected();
    }

    public static boolean isCompleted(ServerPlayer player, ResourceLocation moduleId) {
        ModulePlayerState state = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .modules().get(moduleId);
        return state != null && state.completed();
    }

    // ============================================================
    // Mutations
    // ============================================================

    /**
     * Manually set the unlocked state, firing Pre/Post events.
     */
    public static void setUnlocked(ServerPlayer player, ResourceLocation moduleId, boolean unlocked) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModulePlayerState state = data.modules().getOrDefault(moduleId, ModulePlayerState.createDefault());

        if (state.unlocked() == unlocked) return; // No change

        if (unlocked) {
            ModuleUnlockEvent pre = new ModuleUnlockEvent(player, moduleId);
            NeoForge.EVENT_BUS.post(pre);
            if (pre.isCanceled()) return;
        }

        ModulePlayerState newState = new ModulePlayerState(
                unlocked, state.selected(), state.completed(), state.unlockedInsights()
        );
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withModuleState(moduleId, newState));

        if (unlocked) {
            NeoForge.EVENT_BUS.post(new ModuleUnlockedEvent(player, moduleId));
        }
    }

    /**
     * Select a module. Deducts Insight Points and applies {@code on_select_reward}.
     */
    public static void select(ServerPlayer player, ResourceLocation moduleId) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModuleData module = moduleRegistry(player).get(moduleId);
        if (module == null) return;

        // Ensure selectable modules start with unlocked=true
        ModulePlayerState state = ensureState(data, player, moduleId, module);

        if (!state.unlocked()) return;
        if (state.selected()) return;

        int cost = Config.MODULE_SELECT_COST.get();
        if (data.insightPoints() < cost) return;

        ModuleSelectEvent pre = new ModuleSelectEvent(player, moduleId);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;

        int newPoints = data.insightPoints() - cost;
        ModulePlayerState newState = new ModulePlayerState(
                state.unlocked(), true, state.completed(), state.unlockedInsights()
        );
        PlayerEpiphanyData newData = data.withInsightPoints(newPoints)
                .withModuleState(moduleId, newState);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);

        // Apply on_select_reward
        if (module != null) {
            module.onSelectReward().ifPresent(r -> r.apply(player));
        }

        NeoForge.EVENT_BUS.post(new ModuleSelectedEvent(player, moduleId));
    }

    /**
     * Completes a module: grants an Epiphany slot and applies {@code on_complete_reward}.
     * <p>
     * If the Pre event is cancelled, no reward is granted, no slot is added,
     * the Module stays in the "all Insights unlocked, not completed" state.
     */
    public static void complete(ServerPlayer player, ResourceLocation moduleId) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModulePlayerState state = data.modules().get(moduleId);
        if (state == null || !state.selected() || state.completed()) return;

        ModuleData module = moduleRegistry(player).get(moduleId);
        if (module == null) return;

        // Verify all Insights in this module are unlocked
        for (var entry : module.insights()) {
            if (!state.unlockedInsights().contains(entry.id())) return;
        }

        ModuleCompleteEvent pre = new ModuleCompleteEvent(player, moduleId);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;

        int newSlots = Math.min(data.epiphanySlots() + 1, Config.MAX_EPIPHANY_SLOTS.get());
        ModulePlayerState newState = new ModulePlayerState(
                state.unlocked(), state.selected(), true, state.unlockedInsights()
        );
        PlayerEpiphanyData newData = data.withEpiphanySlots(newSlots)
                .withModuleState(moduleId, newState);

        module.onCompleteReward().ifPresent(r -> r.apply(player));

        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);
        NeoForge.EVENT_BUS.post(new ModuleCompletedEvent(player, moduleId));
    }

    /**
     * Iterates all modules with {@code initial_state=locked} and a condition,
     * auto-unlocking any whose condition is now satisfied.
     * <p>
     * Only fires {@link ModuleUnlockedEvent} (Post); no Pre event for auto-unlocks.
     */
    public static void checkAutoUnlock(ServerPlayer player) {
        Registry<ModuleData> registry = moduleRegistry(player);
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        boolean changed = false;

        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            ModuleData module = entry.getValue();

            if (module.initialState() != InitialState.LOCKED) continue;
            if (module.condition().isEmpty()) continue;

            ModulePlayerState state = data.modules().get(id);
            if (state != null && state.unlocked()) continue;

            if (module.condition().get().test(player)) {
                state = state != null ? state : ModulePlayerState.createDefault();
                ModulePlayerState newState = new ModulePlayerState(
                        true, state.selected(), state.completed(), state.unlockedInsights()
                );
                data = data.withModuleState(id, newState);
                NeoForge.EVENT_BUS.post(new ModuleUnlockedEvent(player, id));
                changed = true;
            }
        }

        if (changed) {
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data);
        }
    }

    /** Admin: force-select a module, ignoring unlock state and point cost. */
    public static void forceSelect(ServerPlayer player, ResourceLocation moduleId) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModulePlayerState state = data.modules().getOrDefault(moduleId, ModulePlayerState.createDefault());
        if (state.selected()) return;

        ModulePlayerState newState = new ModulePlayerState(
                true, true, state.completed(), state.unlockedInsights()
        );
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withModuleState(moduleId, newState));
        NeoForge.EVENT_BUS.post(new ModuleSelectedEvent(player, moduleId));
    }

    /** Admin: force-complete a module, unlocking all Insights and granting the slot. */
    public static void forceComplete(ServerPlayer player, ResourceLocation moduleId) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModuleData module = moduleRegistry(player).get(moduleId);
        if (module == null) return;
        if (isCompleted(player, moduleId)) return;

        Set<ResourceLocation> allIds = new java.util.HashSet<>();
        for (var e : module.insights()) allIds.add(e.id());

        ModulePlayerState newState = new ModulePlayerState(true, true, true, allIds);
        int newSlots = Math.min(data.epiphanySlots() + 1, Config.MAX_EPIPHANY_SLOTS.get());
        PlayerEpiphanyData newData = data.withEpiphanySlots(newSlots)
                .withModuleState(moduleId, newState);

        module.onCompleteReward().ifPresent(r -> r.apply(player));
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);
        NeoForge.EVENT_BUS.post(new ModuleCompletedEvent(player, moduleId));
    }

    /**
     * Resets a module: clears unlocked Insights and refunds points.
     * Admin use only (via commands in phase 5).
     */
    public static void resetModule(ServerPlayer player, ResourceLocation moduleId) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModulePlayerState state = data.modules().get(moduleId);
        if (state == null) return;

        // Refund actual Insight Points for each unlocked insight
        Registry<InsightData> iRegistry = insightRegistry(player);
        int refund = 0;
        for (ResourceLocation insightId : state.unlockedInsights()) {
            InsightData insight = iRegistry.get(insightId);
            if (insight != null) {
                refund += insight.cost();
            }
        }
        // Also refund the module selection cost
        if (state.selected()) {
            refund += Config.MODULE_SELECT_COST.get();
        }

        ModulePlayerState newState = ModulePlayerState.createDefault();
        PlayerEpiphanyData newData = data.withModuleState(moduleId, newState)
                .withInsightPoints(data.insightPoints() + refund);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);
    }
}
