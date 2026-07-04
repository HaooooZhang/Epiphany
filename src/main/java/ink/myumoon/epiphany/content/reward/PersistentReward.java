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

        // --- Module-level rewards ---
        for (var entry : data.modules().entrySet()) {
            var moduleId = entry.getKey();
            var state = entry.getValue();
            ModuleData module = moduleReg.get(moduleId);
            if (module == null) continue;

            // on_select_reward (applied when module was selected)
            if (state.selected()) {
                if (module.onSelectReward().isPresent()
                        && module.onSelectReward().get() instanceof PersistentReward) {
                    module.onSelectReward().get().apply(player, moduleId);
                    count++;
                }
            }

            // on_complete_reward (applied when module was completed)
            if (state.completed()) {
                if (module.onCompleteReward().isPresent()
                        && module.onCompleteReward().get() instanceof PersistentReward) {
                    module.onCompleteReward().get().apply(player, moduleId);
                    count++;
                }
            }

            // Insight rewards (applied when each Insight was unlocked)
            if (state.selected()) {
                for (var insightId : state.unlockedInsights()) {
                    InsightData insight = insightReg.get(insightId);
                    if (insight != null && insight.reward().isPresent()
                            && insight.reward().get() instanceof PersistentReward) {
                        insight.reward().get().apply(player, insightId);
                        count++;
                    }
                }
            }
        }

        // --- Epiphany rewards ---
        for (var entry : data.epiphanies().entrySet()) {
            var epiphanyId = entry.getKey();
            var state = entry.getValue();
            if (!state.selected()) continue;

            EpiphanyData epiphany = epiphanyReg.get(epiphanyId);
            if (epiphany != null && epiphany.reward().isPresent()
                    && epiphany.reward().get() instanceof PersistentReward) {
                epiphany.reward().get().apply(player, epiphanyId);
                count++;
            }
        }

        Epiphany.LOGGER.debug("Reapplied {} persistent rewards for player {}", count, player.getGameProfile().getName());
    }
}
