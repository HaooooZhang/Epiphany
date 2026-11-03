package ink.myumoon.epiphany.content;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

import java.math.BigDecimal;
import java.util.List;

/** A fixed amount or an inclusive, uniformly sampled non-negative long range. */
public record AptitudeAmount(long min, long max) {
    public static final AptitudeAmount ZERO = fixed(0);

    // Codec.LONG uses Number.longValue(), which silently truncates JSON fractions
    // and wraps oversized numbers. Amounts must be exact, non-negative longs.
    private static final Codec<Long> INTEGER_CODEC = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Long> read(DynamicOps<T> ops, T input) {
            return ops.getNumberValue(input).flatMap(number -> {
                try {
                    long value = new BigDecimal(number.toString()).longValueExact();
                    return value >= 0 ? DataResult.success(value)
                            : DataResult.error(() -> "Aptitude amount must be non-negative");
                } catch (NumberFormatException | ArithmeticException e) {
                    return DataResult.error(() -> "Aptitude amount must be an integer in [0, Long.MAX_VALUE]");
                }
            });
        }

        @Override
        public <T> T write(DynamicOps<T> ops, Long value) {
            return ops.createLong(value);
        }
    };

    private static final Codec<AptitudeAmount> ARRAY_CODEC = INTEGER_CODEC.listOf().comapFlatMap(values -> {
        if (values.size() != 2) {
            return DataResult.error(() -> "Aptitude range must contain exactly two integers: [min, max]");
        }
        return checked(values.get(0), values.get(1));
    }, amount -> List.of(amount.min(), amount.max()));

    private static final Codec<AptitudeAmount> OBJECT_CODEC = RecordCodecBuilder.<Bounds>create(instance -> instance.group(
            INTEGER_CODEC.fieldOf("min").forGetter(Bounds::min),
            INTEGER_CODEC.fieldOf("max").forGetter(Bounds::max)
    ).apply(instance, Bounds::new)).flatXmap(
            bounds -> checked(bounds.min(), bounds.max()),
            amount -> DataResult.success(new Bounds(amount.min(), amount.max())));

    /** Accepts a number, [min, max], or {min, max}; writes a number or [min, max]. */
    public static final Codec<AptitudeAmount> CODEC = Codec.either(INTEGER_CODEC,
            Codec.either(ARRAY_CODEC, OBJECT_CODEC)).xmap(
            value -> value.map(AptitudeAmount::fixed, range -> range.map(a -> a, a -> a)),
            amount -> amount.isFixed() ? Either.left(amount.min()) : Either.right(Either.left(amount)));

    public AptitudeAmount {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Expected 0 <= min <= max for aptitude amount");
        }
    }

    private record Bounds(long min, long max) {}

    private static DataResult<AptitudeAmount> checked(long min, long max) {
        return min <= max ? DataResult.success(new AptitudeAmount(min, max))
                : DataResult.error(() -> "Aptitude range min must not exceed max");
    }

    public static AptitudeAmount fixed(long value) {
        return new AptitudeAmount(value, value);
    }

    public boolean isFixed() {
        return min == max;
    }

    /** Does not consume randomness for a fixed amount. */
    public long sample(RandomSource random) {
        if (isFixed()) return min;
        if (min == 0 && max == Long.MAX_VALUE) return random.nextLong() >>> 1;

        long bound = max - min + 1;
        long bits;
        long offset;
        // Rejection sampling avoids modulo bias, including ranges near Long.MAX_VALUE.
        do {
            bits = random.nextLong() >>> 1;
            offset = bits % bound;
        } while (bits - offset + (bound - 1) < 0);
        return min + offset;
    }

    /** For legacy exact-value queries; a range cannot be queried as an actual gain. */
    public long fixedValue() {
        if (!isFixed()) {
            throw new IllegalStateException("Random aptitude amount has no fixed value; use min()/max() for queries");
        }
        return min;
    }
}
