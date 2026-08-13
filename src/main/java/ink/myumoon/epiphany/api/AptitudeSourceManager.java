package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyAttributes;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
 * <p>
 * <b>Multiplier source:</b> the player's {@code epiphany:aptitude_gain_multiplier}
 * attribute value (base + modifiers) is applied here. The attribute defaults to
 * {@code 1.0} (no change). Server admins can adjust base values with the vanilla
 * {@code /attribute} command, and other mods/items can extend per-player gains
 * through the standard AttributeModifier system.
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
     * Resolve + grant: applies the player's {@code aptitude_gain_multiplier}
     * attribute, marks the {@code first_reward} claim if applicable, and finally
     * calls {@link AptitudeManager#addAptitude} which fires the standard aptitude
     * events ({@code AptitudeChanged}, {@code AptitudeLevelUp}, {@code InsightPointsChanged}).
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

        long scaled = (long) (res.reward() * effectiveMultiplier(sp));
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

    /**
     * Returns the effective aptitude-gain multiplier for the player: the value of
     * {@link EpiphanyAttributes#APTITUDE_GAIN_MULTIPLIER} (base + all modifiers).
     * Returns 1.0 (no change) if the attribute is unexpectedly absent from the
     * player — should not normally happen since PLAYER attribute modification
     * is wired in {@code Epiphany#Epiphany} via {@code EntityAttributeModificationEvent}.
     */
    private static double effectiveMultiplier(ServerPlayer sp) {
        AttributeInstance attr = sp.getAttribute(EpiphanyAttributes.APTITUDE_GAIN_MULTIPLIER);
        return attr != null ? attr.getValue() : 1.0;
    }
}
