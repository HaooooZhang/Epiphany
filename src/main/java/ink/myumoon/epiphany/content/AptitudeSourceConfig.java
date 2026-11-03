package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Optional;

/**
 * Data-pack defined mapping from a single game behavior (e.g. "kill_entity",
 * "mine_block") to an aptitude reward.
 * <p>
 * Each behavior is a single entry inside the {@code epiphany:aptitude} datapack
 * registry; the entry id (e.g. {@code epiphany:kill_entity}) is the behavior
 * identifier that a listener looks up when an event fires.
 * <p>
 * Amount fields accept a fixed number, [min, max], or {min, max}.
 * Ranges are inclusive non-negative integers; missing reward falls back to default.
 * JSON shape (fixed amounts remain supported):
 * <pre>{@code
 * {
 *   "default": 3,                       // reward applied when target is not in specials/exclude
 *   "specials": [                       // per-target overrides; target may be id or "#tag"
 *     {
 *       "target": "minecraft:ender_dragon",
 *       "reward": 200,                  // optional, falls back to default if missing
 *       "first_reward": 1000            // optional, granted once per player; tracked in attachment
 *     }
 *   ],
 *   "exclude": ["#epiphany:friendly"]   // blacklist: ids or "#tag" references; matched = no reward
 * }
 * }</pre>
 * <p>
 * {@code exclude} entries may be a plain id ({@code minecraft:cow}) or a tag
 * reference prefixed with {@code #} (e.g. {@code #epiphany:friendly}). The list
 * is typed as {@code List<String>} so the {@code #} character survives codec
 * parsing; the resolver interprets the prefix per registry at runtime.
 */
public record AptitudeSourceConfig(
        Optional<AptitudeAmount> defaultReward,
        List<SpecialEntry> specials,
        List<String> exclude
) {

    public static final AptitudeSourceConfig EMPTY =
            new AptitudeSourceConfig(Optional.empty(), List.of(), List.of());

    public record SpecialEntry(
            String target,
            Optional<AptitudeAmount> reward,
            Optional<AptitudeAmount> firstReward
    ) {
        public static final Codec<SpecialEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("target").forGetter(SpecialEntry::target),
                AptitudeAmount.CODEC.optionalFieldOf("reward").forGetter(SpecialEntry::reward),
                AptitudeAmount.CODEC.optionalFieldOf("first_reward").forGetter(SpecialEntry::firstReward)
        ).apply(instance, SpecialEntry::new));
    }

    public static final Codec<AptitudeSourceConfig> CODEC = RecordCodecBuilder.<AptitudeSourceConfig>create(instance -> instance.group(
            AptitudeAmount.CODEC.optionalFieldOf("default").forGetter(AptitudeSourceConfig::defaultReward),
            SpecialEntry.CODEC.listOf().optionalFieldOf("specials", List.of())
                    .forGetter(AptitudeSourceConfig::specials),
            Codec.STRING.listOf().optionalFieldOf("exclude", List.of())
                    .forGetter(AptitudeSourceConfig::exclude)
    ).apply(instance, AptitudeSourceConfig::new)).validate(config -> {
        for (SpecialEntry special : config.specials()) {
            if (special.firstReward().isPresent()) {
                long regularMax = special.reward().orElse(config.defaultAmountOrZero()).max();
                if (regularMax > Long.MAX_VALUE - special.firstReward().get().max()) {
                    return DataResult.error(() -> "Aptitude reward + first_reward exceeds Long.MAX_VALUE for "
                            + special.target());
                }
            }
        }
        return DataResult.success(config);
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, AptitudeSourceConfig> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** Range used when no special supplies a reward. No randomness is consumed. */
    public AptitudeAmount defaultAmountOrZero() {
        return defaultReward.orElse(AptitudeAmount.ZERO);
    }

    /**
     * Exact-value compatibility helper for fixed configurations.
     * @throws IllegalStateException when the default is a random range
     */
    @Deprecated
    public long defaultOrZero() {
        return defaultAmountOrZero().fixedValue();
    }
}
