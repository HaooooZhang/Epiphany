package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
                strictOptionalField("reward", AptitudeAmount.CODEC).forGetter(SpecialEntry::reward),
                strictOptionalField("first_reward", AptitudeAmount.CODEC).forGetter(SpecialEntry::firstReward)
        ).apply(instance, SpecialEntry::new));
    }

    public static final Codec<AptitudeSourceConfig> CODEC = ExtraCodecs.validate(RecordCodecBuilder.<AptitudeSourceConfig>create(instance -> instance.group(
            strictOptionalField("default", AptitudeAmount.CODEC).forGetter(AptitudeSourceConfig::defaultReward),
            strictList("specials", SpecialEntry.CODEC)
                    .forGetter(AptitudeSourceConfig::specials),
            strictList("exclude", Codec.STRING)
                    .forGetter(AptitudeSourceConfig::exclude)
    ).apply(instance, AptitudeSourceConfig::new)), config -> {
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

    private static <A> MapCodec<List<A>> strictList(String name, Codec<A> element) {
        return strictOptionalField(name, element.listOf()).xmap(
                value -> value.orElse(List.of()), value -> value.isEmpty() ? Optional.empty() : Optional.of(value));
    }

    // DFU 6 optionalFieldOf treats an invalid present value as absent.
    private static <A> MapCodec<Optional<A>> strictOptionalField(String name, Codec<A> codec) {
        return new MapCodec<>() {
            @Override
            public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
                T field = input.get(name);
                return field == null ? DataResult.success(Optional.empty())
                        : codec.parse(ops, field).map(Optional::of);
            }

            @Override
            public <T> RecordBuilder<T> encode(Optional<A> value, DynamicOps<T> ops,
                                                RecordBuilder<T> prefix) {
                return value.isEmpty() ? prefix
                        : prefix.add(name, codec.encodeStart(ops, value.orElseThrow()));
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(ops.createString(name));
            }
        };
    }

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
