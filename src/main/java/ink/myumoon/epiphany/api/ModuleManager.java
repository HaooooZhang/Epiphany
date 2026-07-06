package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.condition.Condition;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.event.*;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Set;

public final class ModuleManager {

    private ModuleManager() {
    }

    // registry handle
    private static Registry<ModuleData> moduleRegistry(ServerPlayer player) {
        return player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
    }

    private static Registry<InsightData> insightRegistry(ServerPlayer player) {
        return player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
    }

    // query
    public static boolean isUnlocked(ServerPlayer player, ResourceLocation moduleId) {
        ModulePlayerState state = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .modules().get(moduleId);
        if (state != null) return state.unlocked(); // state record first
        ModuleData module = moduleRegistry(player).get(moduleId); // initial state
        return module != null && module.initialState() == InitialState.SELECTABLE;
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

    // unlock
    public static void setUnlocked(ServerPlayer player, ResourceLocation moduleId, boolean unlocked) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModulePlayerState state = data.modules().getOrDefault(moduleId, ModulePlayerState.createDefault());

        if (state.unlocked() == unlocked) return;

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

    // select
    public static void select(ServerPlayer player, ResourceLocation moduleId) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModulePlayerState state = data.modules().getOrDefault(moduleId, ModulePlayerState.createDefault());

        if (!isUnlocked(player, moduleId)) return;
        if (state.selected()) return;

        int cost = Config.MODULE_SELECT_COST.get();
        if (data.insightPoints() < cost) return;

        // Module selection limit
        long selectedCount = data.modules().values().stream()
                .filter(ModulePlayerState::selected).count();
        if (selectedCount >= Config.MAX_SELECTED_MODULES.get()) return;

        ModuleSelectEvent pre = new ModuleSelectEvent(player, moduleId);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;

        int newPoints = data.insightPoints() - cost;
        int newTotalSpent = data.totalInsightPointsSpent() + cost;
        ModulePlayerState newState = new ModulePlayerState(
                state.unlocked(), true, state.completed(), state.unlockedInsights()
        );
        PlayerEpiphanyData newData = data.withInsightPoints(newPoints)
                .withTotalInsightPointsSpent(newTotalSpent)
                .withModuleState(moduleId, newState);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);

        // Apply on_select_reward
        ModuleData module = moduleRegistry(player).get(moduleId);
        if (module != null) {
            module.onSelectReward().ifPresent(r -> r.apply(player, moduleId));
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

        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);

        module.onCompleteReward().ifPresent(r -> r.apply(player, moduleId));
        NeoForge.EVENT_BUS.post(new ModuleCompletedEvent(player, moduleId));
    }

    /**
     * Iterates all modules with {@code initial_state=locked} and a condition,
     * auto-unlocking any whose condition is now satisfied.
     * <p>
     * Only fires {@link ModuleUnlockedEvent} (Post); no Pre event for auto-unlocks.
     */
    /** Evaluate conditions and auto-unlock matching modules. */
    public static void checkAutoUnlock(ServerPlayer player) {
        checkAutoUnlock(player, false);
    }

    /**
     * @param skipEventDriven if true, skip conditions marked {@link Condition#isEventDriven()}
     *                        (used by polling to avoid redundant checks)
     */
    public static void checkAutoUnlock(ServerPlayer player, boolean skipEventDriven) {
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

            Condition cond = module.condition().get();
            if (skipEventDriven && cond.isEventDriven()) continue;
            if (cond.test(player)) {
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

    // select (force)
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

    // complete (force)
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

        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);

        module.onCompleteReward().ifPresent(r -> r.apply(player, moduleId));
        NeoForge.EVENT_BUS.post(new ModuleCompletedEvent(player, moduleId));
    }

    // reset
    public static void resetModule(ServerPlayer player, ResourceLocation moduleId) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModulePlayerState state = data.modules().get(moduleId);
        if (state == null) return;

        Registry<InsightData> iRegistry = insightRegistry(player);
        int refund = 0;
        for (ResourceLocation insightId : state.unlockedInsights()) {
            InsightData insight = iRegistry.get(insightId);
            if (insight != null) {
                refund += insight.cost();
                insight.reward().ifPresent(r -> r.remove(player, insightId));
            }
        }
        if (state.selected()) {
            refund += Config.MODULE_SELECT_COST.get();
        }

        ModulePlayerState newState = ModulePlayerState.createDefault();

        // Selectable modules stay unlocked after reset
        ModuleData module = moduleRegistry(player).get(moduleId);
        if (module != null && module.initialState() == InitialState.SELECTABLE) {
            newState = new ModulePlayerState(true, false, false, Set.of());
        }

        // Remove module rewards
        if (module != null) {
            module.onSelectReward().ifPresent(r -> r.remove(player, moduleId));
            module.onCompleteReward().ifPresent(r -> r.remove(player, moduleId));
        }

        PlayerEpiphanyData newData = data.withModuleState(moduleId, newState)
                .withInsightPoints(data.insightPoints() + refund)
                .withTotalInsightPointsSpent(Math.max(0, data.totalInsightPointsSpent() - refund));
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);
    }
}
