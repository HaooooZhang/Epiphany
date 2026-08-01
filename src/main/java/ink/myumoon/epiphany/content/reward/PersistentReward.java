package ink.myumoon.epiphany.content.reward;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;

/**
 * Marker interface for rewards that should persist across entity rebuilds
 * (death, end-portal return, etc.).
 * <p>
 * Rewards implementing this interface will be automatically re-applied
 * via {@link #reapplyAll(ServerPlayer)} on {@code PlayerRespawnEvent}.
 * <p>
 * Both {@link InsightReward} and {@link EpiphanyReward} types can opt in
 * by adding {@code implements PersistentReward} alongside their primary interface.
 * <p>
 * <b>Important:</b> {@link #reapplyAll} calls {@code apply()} directly,
 * so reward implementations must ensure {@code apply()} is idempotent
 * (safe to call multiple times without stacking effects).
 */
public interface PersistentReward {

    /**
     * Iterates all active rewards for the given player and re-applies any
     * that implement {@link PersistentReward}.
     * <p>
     * Covers four reward sources:
     * <ol>
     *   <li>Module {@code on_select_reward} — if module is selected</li>
     *   <li>Module {@code on_complete_reward} — if module is completed</li>
     *   <li>Insight rewards — for each unlocked Insight in a selected module</li>
     *   <li>Epiphany rewards — for each selected Epiphany</li>
     * </ol>
     *
     * @param player the server-side player to reapply rewards for
     */
    static void reapplyAll(ServerPlayer player) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);

        Registry<ModuleData> moduleReg = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
        Registry<InsightData> insightReg = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
        Registry<EpiphanyData> epiphanyReg = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);

        int count = 0;

        for (var entry : data.modules().entrySet()) {
            var moduleId = entry.getKey();
            var state = entry.getValue();
            ModuleData module = moduleReg.get(moduleId);
            if (module == null) continue;

            // on_select_reward
            if (state.selected()) {
                count += module.onSelectReward().stream()
                    .filter(RewardListHelper::isReapplicablePersistent).count();
                RewardListHelper.reapplyPersistentInsight(
                    module.onSelectReward(), player, moduleId, "on_select_reward");
            }

            // on_complete_reward
            if (state.completed()) {
                count += module.onCompleteReward().stream()
                    .filter(RewardListHelper::isReapplicablePersistent).count();
                RewardListHelper.reapplyPersistentInsight(
                    module.onCompleteReward(), player, moduleId, "on_complete_reward");
            }

            // Insight rewards
            if (state.selected()) {
                for (var insightId : state.unlockedInsights()) {
                    InsightData insight = insightReg.get(insightId);
                    if (insight != null) {
                        count += insight.reward().stream()
                            .filter(RewardListHelper::isReapplicablePersistent).count();
                        RewardListHelper.reapplyPersistentInsight(insight.reward(), player, insightId);
                    }
                }
            }
        }

        // Epiphany rewards
        for (var entry : data.epiphanies().entrySet()) {
            var epiphanyId = entry.getKey();
            var state = entry.getValue();
            if (!state.selected()) continue;

            EpiphanyData epiphany = epiphanyReg.get(epiphanyId);
            if (epiphany != null) {
                count += epiphany.reward().stream()
                    .filter(RewardListHelper::isReapplicablePersistent).count();
                RewardListHelper.reapplyPersistentEpiphany(epiphany.reward(), player, epiphanyId);
            }
        }

        Epiphany.LOGGER.debug("Reapplied {} persistent rewards for player {}", count, player.getGameProfile().getName());
    }

    /** Rebuilds only persistent effect sources without replaying other reward types. */
    static void reapplyEffects(ServerPlayer player) {
        var previousEffects = EffectReward.clearTrackedSources(player);
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        Registry<ModuleData> moduleReg = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
        Registry<InsightData> insightReg = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
        Registry<EpiphanyData> epiphanyReg = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);

        for (var entry : data.modules().entrySet()) {
            ModuleData module = moduleReg.get(entry.getKey());
            if (module == null) continue;
            var state = entry.getValue();
            if (state.selected()) {
                RewardListHelper.reapplyPersistentEffects(module.onSelectReward(), player,
                        entry.getKey(), "on_select_reward");
                for (var insightId : state.unlockedInsights()) {
                    InsightData insight = insightReg.get(insightId);
                    if (insight != null) RewardListHelper.reapplyPersistentEffects(
                            insight.reward(), player, insightId, "reward");
                }
            }
            if (state.completed()) {
                RewardListHelper.reapplyPersistentEffects(module.onCompleteReward(), player,
                        entry.getKey(), "on_complete_reward");
            }
        }
        for (var entry : data.epiphanies().entrySet()) {
            if (!entry.getValue().selected()) continue;
            EpiphanyData epiphany = epiphanyReg.get(entry.getKey());
            if (epiphany != null) RewardListHelper.reapplyPersistentEffects(
                    epiphany.reward(), player, entry.getKey());
        }
        EffectReward.finishSourceRebuild(player, previousEffects);
    }
}
