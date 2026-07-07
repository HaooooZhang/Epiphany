package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Public API for granting aptitude from datapack-defined behavior sources.
 * <p>
 * Epiphany ships built-in behaviors (e.g. {@code epiphany:kill_entity},
 * {@code epiphany:mine_block}) wired via its own listeners. Third-party mods can
 * add new behaviors by:
 * <ol>
 *   <li>Defining the source JSON at {@code data/<yourmod>/epiphany/aptitude/<behavior>.json}</li>
 *   <li>Registering a NeoForge event listener in their own mod</li>
 *   <li>Calling {@link #grant} from that listener with the behavior id, the target
 *       id that triggered the event, and the appropriate registry key (or {@code null}
 *       for behaviors with no natural target registry, e.g. "jump")</li>
 * </ol>
 * <p>
 * This class is the single entry point for third parties. The lower-level
 * {@link AptitudeSourceResolver} exposes the same logic as a pure function for
 * callers that need the {@link AptitudeSourceResolver.Resolution} without the
 * side effects (multiplier, first_reward claim, addAptitude).
 */
public final class AptitudeSourceManager {

    private AptitudeSourceManager() {
    }

    /**
     * Pure resolution: figure out the reward for one behavior+target pair without
     * applying any side effect. Useful for inspection, UI hints, etc.
     */
    public static AptitudeSourceResolver.Resolution resolve(
            ServerPlayer sp,
            ResourceLocation behaviorId,
            ResourceLocation targetId,
            @Nullable Registry<?> registry
    ) {
        return AptitudeSourceResolver.resolve(sp, behaviorId, targetId, registry);
    }

    /**
     * Resolve + grant: applies {@link Config#APTITUDE_GAIN_MULTIPLIER}, marks the
     * {@code first_reward} claim if applicable, and finally calls
     * {@link AptitudeManager#addAptitude} which fires the standard aptitude events
     * ({@code AptitudeChanged}, {@code AptitudeLevelUp}, {@code InsightPointsChanged}).
     *
     * @param sp          the player who triggered the behavior
     * @param behaviorId  entry id in {@code epiphany:aptitude} registry (e.g. {@code epiphany:kill_entity})
     * @param targetId    concrete entity/block/item id that triggered the event
     * @param registry    used to resolve {@code #tag} references; pass a static
     *                    {@link net.minecraft.core.registries.BuiltInRegistries}
     *                    instance for the target's vanilla kind, or {@code null} for
     *                    behaviors with no natural target — {@code #tag} entries
     *                    then silently fall back to {@code default}
     * @return true if any aptitude was granted; false if the resolver skipped
     *         (excluded, no config, zero reward, etc.)
     */
    public static boolean grant(
            ServerPlayer sp,
            ResourceLocation behaviorId,
            ResourceLocation targetId,
            @Nullable Registry<?> registry
    ) {
        var res = AptitudeSourceResolver.resolve(sp, behaviorId, targetId, registry);
        if (!res.applies()) return false;

        long scaled = (long) (res.reward() * Config.APTITUDE_GAIN_MULTIPLIER.get());
        if (scaled <= 0) return false;

        // Mark first_reward claim FIRST so downstream consumers (e.g. events fired by
        // AptitudeManager.addAptitude) see a consistent state even if they cancel.
        if (res.claimKey() != null) {
            PlayerEpiphanyData data = sp.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
            sp.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withClaimedFirst(res.claimKey()));
        }

        AptitudeManager.addAptitude(sp, scaled);
        return true;
    }
}
