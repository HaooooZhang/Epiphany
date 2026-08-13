package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.reward.RewardListHelper;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;

/**
 * Cross-subsystem operations that don't naturally belong to a single {@code *Manager}.
 * <p>
 * Hosts the two whole-data reset paths ({@link #resetAll} and {@link #resetSelections}) plus the
 * shared helpers they depend on ({@link #removeAllRewards} and {@link #refundInsightCosts}).
 * These were previously inlined as private methods inside {@code EpiphanyCommand}; they are now
 * public so that UI code paths and KubeJS scripts can reuse them without going through a command.
 * <p>
 * <b>Event behavior</b>: the reset methods here intentionally fire <b>no</b> events
 * ({@code AptitudeChangedEvent}, {@code InsightPointsChangedEvent}, etc.) to match the historical
 * behavior of the {@code /epiphany reset} sub-commands. Third-party listeners will not observe
 * these resets. This is a deliberate "equivalent migration" decision — introduce reset events
 * separately if needed.
 */
public final class EpiphanyDataUtils {

    private EpiphanyDataUtils() {
    }

    // ─── Shared internal helpers ───────────────────────────────────

    /**
     * Sums the cost of every unlockable that the player currently holds, for refund purposes:
     * {@link Config#MODULE_SELECT_COST} per selected Module, plus each unlocked Insight's
     * {@code cost()} value (looked up from the datapack registry).
     * <p>
     * Pure read — does not mutate player data.
     */
    public static int refundInsightCosts(ServerPlayer player, PlayerEpiphanyData data) {
        Registry<InsightData> iReg = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
        int total = 0;
        for (var moduleState : data.modules().values()) {
            if (moduleState.selected()) total += Config.MODULE_SELECT_COST.get();
            for (ResourceLocation insightId : moduleState.unlockedInsights()) {
                var insight = iReg.get(insightId);
                if (insight != null) total += insight.cost();
            }
        }
        return total;
    }

    /**
     * Strips every reward (Module on_select/on_complete, every Insight reward, every selected
     * Epiphany reward) from the player by delegating to {@link RewardListHelper#removeInsight} and
     * {@link RewardListHelper#removeEpiphany}. Reads registry definitions so removed-datum entries
     * are silently skipped.
     * <p>
     * Does not modify the {@link PlayerEpiphanyData} record itself — callers are expected to write
     * new state via {@code player.setData(...)} afterwards (see {@link #resetAll} / {@link #resetSelections}).
     */
    public static void removeAllRewards(ServerPlayer player) {
        var access = player.server.registryAccess();
        var iReg = access.registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
        var mReg = access.registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
        var eReg = access.registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);

        // Modules: on_select, on_complete, and all unlocked insight rewards
        for (var me : data.modules().entrySet()) {
            var module = mReg.get(me.getKey());
            if (module != null) {
                if (me.getValue().selected()) {
                    RewardListHelper.removeInsight(module.onSelectReward(), player, me.getKey(), "on_select_reward");
                }
                if (me.getValue().completed()) {
                    RewardListHelper.removeInsight(module.onCompleteReward(), player, me.getKey(), "on_complete_reward");
                }
            }
            for (ResourceLocation iId : me.getValue().unlockedInsights()) {
                var insight = iReg.get(iId);
                if (insight != null) RewardListHelper.removeInsight(insight.reward(), player, iId);
            }
        }

        // Epiphanies
        for (var ee : data.epiphanies().entrySet()) {
            if (ee.getValue().selected()) {
                var epiphany = eReg.get(ee.getKey());
                if (epiphany != null) RewardListHelper.removeEpiphany(epiphany.reward(), player, ee.getKey());
            }
        }
    }

    // ─── resets ─────────────────────────────────────────

    /**
     * Full wipe: removes all rewards, resets the attachment to {@link PlayerEpiphanyData#createDefault()},
     * and re-runs {@link ModuleManager#checkAutoUnlock} / {@link EpiphanyManager#checkAutoUnlock}
     * (silent + skip-event-driven) to re-grant anything whose initial state is {@code SELECTABLE}.
     * <p>
     * Equivalent to the {@code /epiphany reset all} command. <b>Fires no events.</b>
     */
    public static void resetAll(ServerPlayer player) {
        removeAllRewards(player);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, PlayerEpiphanyData.createDefault());
        ModuleManager.checkAutoUnlock(player, false, true);
        EpiphanyManager.checkAutoUnlock(player, false, true);
    }

    /**
     * Clears the player's selections/selectables while preserving their accumulated currency:
     * <ul>
     *   <li>all rewards removed (see {@link #removeAllRewards})</li>
     *   <li>{@code aptitude} preserved</li>
     *   <li>{@code insightPoints} refunded by the total cost of every selected Module and unlocked Insight</li>
     *   <li>{@code totalInsightPointsSpent} reduced by the same refund (clamped to {@code >= 0})</li>
     *   <li>{@code modules} / {@code insights} / {@code epiphanies} / {@code epiphanySlots} / {@code usedEpiphanySlots}
     *       / {@code claimedFirsts} all cleared</li>
     *   <li>{@link ModuleManager#checkAutoUnlock} / {@link EpiphanyManager#checkAutoUnlock} re-run
     *       (silent + skip-event-driven) to re-grant {@code SELECTABLE} entries</li>
     * </ul>
     * Equivalent to the {@code /epiphany reset select} command. <b>Fires no events.</b>
     */
    public static void resetSelections(ServerPlayer player) {
        removeAllRewards(player);
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);

        int refund = refundInsightCosts(player, data);
        PlayerEpiphanyData cleaned = new PlayerEpiphanyData(
                data.aptitude(),
                data.insightPoints() + refund,
                Math.max(0, data.totalInsightPointsSpent() - refund),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                0, 0,
                Collections.emptyMap()
        );
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, cleaned);
        ModuleManager.checkAutoUnlock(player, false, true);
        EpiphanyManager.checkAutoUnlock(player, false, true);
    }
}
