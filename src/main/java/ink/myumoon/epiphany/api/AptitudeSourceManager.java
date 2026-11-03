package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Epiphany;
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
     * Pure resolution: describes base and first-reward ranges without sampling.
     * Use the result's minReward/maxReward for bounds, or baseReward/firstReward
     * for the independently sampled components. Legacy reward() is fixed-only.
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
     * Resolve + grant: samples base and first reward independently on the server,
     * applies the player's multiplier once to their sum, then atomically commits
     * the gain and first-reward claim before posting the standard aptitude events.
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
        return grantResolved(sp, AptitudeSourceResolver.resolve(sp, behaviorId, targetId, registry));
    }

    /** Package-private to verify settlement independently of registry lookup. */
    static boolean grantResolved(ServerPlayer sp, AptitudeSourceResolver.Resolution res) {
        if (!res.applies()) return false;

        final long scaled;
        try {
            long sampled = Math.addExact(res.baseReward().sample(sp.getRandom()),
                    res.firstReward().sample(sp.getRandom()));
            scaled = AptitudeManager.scaleAptitudeWithMultiplier(sp, sampled);
        } catch (ArithmeticException e) {
            Epiphany.LOGGER.warn("Rejected aptitude source gain: {}", e.getMessage());
            return false;
        }
        if (scaled <= 0) return false;

        return AptitudeManager.grantAptitude(sp, scaled, res.claimKey());
    }
}
