package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.InsightTreeResolver;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.event.InsightSelectEvent;
import ink.myumoon.epiphany.event.InsightSelectedEvent;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages Insight unlock logic, including tree prerequisite checks
 * and automatic Module completion detection.
 */
public final class InsightManager {

    private InsightManager() {
    }

    // ============================================================
    // Registry helpers
    // ============================================================

    private static Registry<InsightData> insightRegistry(ServerPlayer player) {
        return player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
    }

    private static Registry<ModuleData> moduleRegistry(ServerPlayer player) {
        return player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
    }

    // ============================================================
    // Queries
    // ============================================================

    public static boolean isSelected(ServerPlayer player, ResourceLocation insightId) {
        // An Insight is "selected"/unlocked if it appears in some module's unlockedInsights set.
        for (ModulePlayerState ms : player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .modules().values()) {
            if (ms.unlockedInsights().contains(insightId)) return true;
        }
        return false;
    }

    public static boolean isModuleSelected(ServerPlayer player, ResourceLocation insightId) {
        // Look up the Insight's parent module from the registry,
        // then check whether that module is selected.
        for (var entry : moduleRegistry(player).entrySet()) {
            ResourceLocation moduleId = entry.getKey().location();
            ModuleData module = entry.getValue();
            boolean insightInModule = module.insights().stream()
                    .anyMatch(e -> e.id().equals(insightId));
            if (insightInModule) {
                return ModuleManager.isSelected(player, moduleId);
            }
        }
        return false;
    }

    // ============================================================
    // Unlock
    // ============================================================

    /**
     * Unlocks an Insight for a player.
     * <p>
     * Deducts Insight Points, applies the reward,
     * and automatically triggers {@link ModuleManager#complete}
     * if all Insights in the module are now unlocked.
     *
     * @param player    the player
     * @param insightId the Insight registry ID
     * @param moduleId  the parent Module registry ID
     */
    public static void select(ServerPlayer player, ResourceLocation insightId, ResourceLocation moduleId) {
        InsightData insight = insightRegistry(player).get(insightId);
        ModuleData module = moduleRegistry(player).get(moduleId);
        if (insight == null || module == null) return;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);

        if (!InsightTreeResolver.canUnlock(data, moduleId, module, insightId)) return;
        if (data.insightPoints() < insight.cost()) return;

        InsightSelectEvent pre = new InsightSelectEvent(player, insightId, moduleId);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;

        // Deduct points
        int newPoints = data.insightPoints() - insight.cost();
        int newTotalSpent = data.totalInsightPointsSpent() + insight.cost();

        // Add to module's unlockedInsights
        ModulePlayerState moduleState = data.modules()
                .getOrDefault(moduleId, ModulePlayerState.createDefault());
        Set<ResourceLocation> newUnlocked = new HashSet<>(moduleState.unlockedInsights());
        newUnlocked.add(insightId);
        ModulePlayerState newModuleState = new ModulePlayerState(
                moduleState.unlocked(), moduleState.selected(), moduleState.completed(), newUnlocked
        );

        // Apply reward
        insight.reward().ifPresent(r -> r.apply(player, insightId));

        PlayerEpiphanyData newData = data.withInsightPoints(newPoints)
                .withTotalInsightPointsSpent(newTotalSpent)
                .withModuleState(moduleId, newModuleState);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);

        NeoForge.EVENT_BUS.post(new InsightSelectedEvent(player, insightId, moduleId));

        // Auto-complete check: are all Insights in this module now unlocked?
        boolean allUnlocked = module.insights().stream()
                .allMatch(e -> newUnlocked.contains(e.id()));
        if (allUnlocked) {
            ModuleManager.complete(player, moduleId);
        }
    }

    /** Admin: force-unlock an Insight, ignoring cost and tree prerequisites. */
    public static void forceSelect(ServerPlayer player, ResourceLocation insightId, ResourceLocation moduleId) {
        InsightData insight = insightRegistry(player).get(insightId);
        if (insight == null) return;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        ModulePlayerState moduleState = data.modules()
                .getOrDefault(moduleId, ModulePlayerState.createDefault());
        if (moduleState.unlockedInsights().contains(insightId)) return;

        Set<ResourceLocation> newUnlocked = new HashSet<>(moduleState.unlockedInsights());
        newUnlocked.add(insightId);
        ModulePlayerState newModuleState = new ModulePlayerState(
                moduleState.unlocked(), moduleState.selected(), moduleState.completed(), newUnlocked
        );
        insight.reward().ifPresent(r -> r.apply(player, insightId));
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                data.withModuleState(moduleId, newModuleState));

        NeoForge.EVENT_BUS.post(new InsightSelectedEvent(player, insightId, moduleId));
    }

    /** Admin: remove an Insight and refund its cost. */
    public static void resetInsight(ServerPlayer player, ResourceLocation insightId) {
        InsightData insight = insightRegistry(player).get(insightId);
        if (insight == null) return;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        for (var entry : data.modules().entrySet()) {
            ModulePlayerState state = entry.getValue();
            if (state.unlockedInsights().contains(insightId)) {
                insight.reward().ifPresent(r -> r.remove(player, insightId));
                Set<ResourceLocation> newUnlocked = new HashSet<>(state.unlockedInsights());
                newUnlocked.remove(insightId);
                ModulePlayerState newState = new ModulePlayerState(
                        state.unlocked(), state.selected(), state.completed(), newUnlocked
                );
                player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                        data.withModuleState(entry.getKey(), newState)
                                .withInsightPoints(data.insightPoints() + insight.cost())
                                .withTotalInsightPointsSpent(Math.max(0, data.totalInsightPointsSpent() - insight.cost())));
                return;
            }
        }
    }
}
