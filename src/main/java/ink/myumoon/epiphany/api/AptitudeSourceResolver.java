package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.content.AptitudeSourceConfig;
import ink.myumoon.epiphany.content.AptitudeAmount;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Resolves a single behavior+target pair to an aptitude reward.
 * <p>
 * Pure function: reads the {@code epiphany:aptitude} datapack registry,
 * evaluates {@code exclude} and {@code specials} (both accept plain ids or
 * {@code #tag} references), and decides the reward + whether {@code first_reward}
 * should be claimed. Does not mutate any state.
 * <p>
 * Pipeline:
 * <ol>
 *   <li>Look up {@link AptitudeSourceConfig} by behavior id in the
 *       {@code epiphany:aptitude} datapack registry.</li>
 *   <li>If {@code exclude} contains either the target id or a {@code #tag} that
 *       the target belongs to (per {@code registryKey}) → SKIP (apply = false).</li>
 *   <li>If {@code specials} contains the target → reward = special.reward.orElse(default).</li>
 *   <li>Otherwise reward = default. No positive possible base or first reward → SKIP.</li>
 *   <li>If the matched special defines {@code first_reward} and the player hasn't
 *       claimed it yet → describe its range separately and request a claim mark.</li>
 * </ol>
 * <p>
 * Tag resolution: pass a {@code registryKey} (e.g. {@link net.minecraft.core.registries.Registries#ENTITY_TYPE})
 * so {@code #tag} references can be resolved against the live server registry. Pass
 * {@code null} for behaviors whose targets have no natural registry (e.g. "jump") —
 * {@code #tag} entries will simply never match, falling back to {@code default}.
 * <p>
 * Graceful degradation: malformed ids, unknown tags, or tag references when
 * {@code registryKey == null} are all treated as "this rule does not match" — never
 * throws, never skips the whole resolve.
 */
public final class AptitudeSourceResolver {

    private AptitudeSourceResolver() {
    }

    /** Pure reward description. Base and first reward are sampled independently by grant. */
    public record Resolution(
            boolean applies,
            AptitudeAmount baseReward,
            AptitudeAmount firstReward,
            @Nullable ResourceLocation claimKey // non-null iff a first reward is available
    ) {
        /** Retains the old constructor for callers describing a fixed reward. */
        public Resolution(boolean applies, long reward, @Nullable ResourceLocation claimKey) {
            this(applies, AptitudeAmount.fixed(reward), AptitudeAmount.ZERO, claimKey);
        }

        public static Resolution skip() {
            return new Resolution(false, AptitudeAmount.ZERO, AptitudeAmount.ZERO, null);
        }

        public boolean isFixed() {
            return baseReward.isFixed() && firstReward.isFixed();
        }

        public long minReward() {
            return Math.addExact(baseReward.min(), firstReward.min());
        }

        public long maxReward() {
            return Math.addExact(baseReward.max(), firstReward.max());
        }

        /**
         * Legacy exact query. Fixed configurations retain their previous result.
         * Random configurations must inspect baseReward/firstReward or minReward/maxReward.
         */
        @Deprecated
        public long reward() {
            return Math.addExact(baseReward.fixedValue(), firstReward.fixedValue());
        }
    }

    /**
     * Resolve a single behavior+target pair.
     *
     * @param sp          the player whose data is inspected (for claim lookup only)
     * @param behaviorId  entry id in {@code epiphany:aptitude} registry (e.g. {@code epiphany:kill_entity})
     * @param targetId    concrete entity/block/item id that triggered the event
     * @param registry    used to resolve {@code #tag} references; pass {@code null} for
     *                    behaviors whose targets have no natural registry (then
     *                    {@code #tag} entries never match). For vanilla kinds use the
     *                    {@link BuiltInRegistries} instance (ENTITY_TYPE / BLOCK / ...):
     *                    on NeoForge 1.21.1 the static registries hold the actual
     *                    datapack-loaded tag bindings.
     */
    public static Resolution resolve(
            ServerPlayer sp,
            ResourceLocation behaviorId,
            ResourceLocation targetId,
            @Nullable Registry<?> registry
    ) {
        Optional<AptitudeSourceConfig> configOpt = sp.server.registryAccess()
                .registry(EpiphanyRegistries.APTITUDE_SOURCE_REGISTRY_KEY)
                .map(reg -> reg.get(behaviorId));
        if (configOpt.isEmpty()) return Resolution.skip();
        return resolve(configOpt.get(), behaviorId, targetId, registry,
                key -> sp.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA).claimedFirsts().containsKey(key));
    }

    /** Shared pure matching logic; the predicate only inspects existing first-reward claims. */
    static Resolution resolve(AptitudeSourceConfig config, ResourceLocation behaviorId,
                              ResourceLocation targetId, @Nullable Registry<?> registry,
                              Predicate<ResourceLocation> alreadyClaimed) {

        // (2) exclude — short-circuit on first match
        for (String ex : config.exclude()) {
            if (matchesReference(registry, ex, targetId)) return Resolution.skip();
        }

        // (3) specials override + (5) first_reward gating
        AptitudeAmount reward;
        Optional<AptitudeAmount> optFirst = Optional.empty();
        AptitudeSourceConfig.SpecialEntry matched = null;
        for (AptitudeSourceConfig.SpecialEntry s : config.specials()) {
            if (matchesReference(registry, s.target(), targetId)) {
                matched = s;
                break;
            }
        }
        if (matched != null) {
            reward = matched.reward().orElse(config.defaultAmountOrZero());
            optFirst = matched.firstReward();
        } else {
            reward = config.defaultAmountOrZero();
        }
        if (optFirst.isPresent()) {
            ResourceLocation claimKey = claimKey(behaviorId, targetId);
            if (!alreadyClaimed.test(claimKey)) {
                AptitudeAmount first = optFirst.get();
                return new Resolution(reward.max() > 0 || first.max() > 0, reward, first, claimKey);
            }
        }
        return new Resolution(reward.max() > 0, reward, AptitudeAmount.ZERO, null);
    }

    /**
     * Check whether a reference string (plain id or {@code #tag}) matches the target.
     * <p>
     * Graceful degradation: malformed refs, unknown tags, missing registries, and
     * {@code #tag} refs when {@code registry == null} all return {@code false}
     * (treated as "this rule does not match"), letting the overall resolve fall
     * through to other rules or {@code default}.
     */
    private static boolean matchesReference(
            @Nullable Registry<?> registry,
            String reference,
            ResourceLocation targetId
    ) {
        if (reference.startsWith("#")) {
            if (registry == null) return false;
            ResourceLocation tagLoc = ResourceLocation.tryParse(reference.substring(1));
            if (tagLoc == null) return false;
            return registryContainsTagId(registry, tagLoc, targetId);
        }
        ResourceLocation parsed = ResourceLocation.tryParse(reference);
        return parsed != null && parsed.equals(targetId);
    }

    /**
     * Returns true iff the target id is a member of the given tag in the supplied
     * registry. Callers pass a static {@link BuiltInRegistries} instance for the
     * relevant vanilla registry kind — on NeoForge 1.21.1 those hold the
     * datapack-loaded tag bindings.
     * <p>
     * Implementation note: iterates the tag's holder set, extracting each holder's
     * ResourceKey via {@code Holder#unwrapKey()} (registry-agnostic, avoids raw
     * {@code Registry#getResourceKey(value)} lookup pitfalls).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean registryContainsTagId(
            Registry<?> registry, ResourceLocation tagLoc, ResourceLocation targetId) {
        TagKey tagKey = TagKey.create(registry.key(), tagLoc);
        for (Object holderObj : (Iterable) registry.getTagOrEmpty(tagKey)) {
            net.minecraft.core.Holder holder = (net.minecraft.core.Holder) holderObj;
            Optional<net.minecraft.resources.ResourceKey<?>> keyOpt = holder.unwrapKey();
            if (keyOpt.isPresent() && keyOpt.get().location().equals(targetId)) return true;
        }
        return false;
    }

    /**
     * Standard claim-key layout. ResourceLocation constraints forbid ':' inside
     * the path, so the target's namespace is escaped as '.' in the path segment.
     * Example: behavior {@code epiphany:kill_entity} + target {@code minecraft:zombie}
     * → {@code epiphany:kill_entity/minecraft.zombie}
     */
    public static ResourceLocation claimKey(ResourceLocation behaviorId, ResourceLocation targetId) {
        String path = behaviorId.getPath() + "/" + targetId.getNamespace() + "." + targetId.getPath();
        return ResourceLocation.fromNamespaceAndPath(behaviorId.getNamespace(), path);
    }
}
