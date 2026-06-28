package ink.myumoon.epiphany.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.EpiphanyConstants;
import net.minecraft.core.Registry;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Codec wrapper that falls back to a default value when decoding/encoding fails,
 * preventing the game from crashing on malformed datapack JSON.
 * <p>
 * Pattern taken from Origins-NeoForge.
 */
public class DefaultedCodec<A> implements Codec<A> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Codec<A> baseCodec;
    private final Supplier<A> defaultValue;
    private final String name;

    public DefaultedCodec(Codec<A> baseCodec, Supplier<A> defaultValue, String name) {
        this.baseCodec = baseCodec;
        this.defaultValue = defaultValue;
        this.name = name;
    }

    /**
     * Creates a dispatch codec from a registry, using the given type key.
     *
     * @param registry     the registry mapping type keys to MapCodecs
     * @param typeKey      the JSON field name used for dispatch (e.g. "type")
     * @param type         extracts the registry key from a concrete instance
     * @param codec        maps a registry key to its MapCodec
     * @param defaultValue fallback value when decoding fails
     */
    public static <T, A> DefaultedCodec<T> registryDispatch(
            Registry<A> registry,
            String typeKey,
            Function<? super T, ? extends A> type,
            Function<? super A, ? extends MapCodec<? extends T>> codec,
            Supplier<T> defaultValue
    ) {
        return new DefaultedCodec<>(
                registry.byNameCodec().dispatch(typeKey, type, codec),
                defaultValue,
                registry.key().location().toString()
        );
    }

    /**
     * Creates a dispatch codec from a registry, using the default type key {@code "type"}.
     */
    public static <T, A> DefaultedCodec<T> registryDispatch(
            Registry<A> registry,
            Function<? super T, ? extends A> type,
            Function<? super A, ? extends MapCodec<? extends T>> codec,
            Supplier<T> defaultValue
    ) {
        return registryDispatch(registry, EpiphanyConstants.TYPE_KEY, type, codec, defaultValue);
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        DataResult<Pair<A, T>> result = this.baseCodec.decode(ops, input);
        if (result instanceof DataResult.Error<Pair<A, T>> error) {
            LOGGER.error("Failed to decode {}: {}", this.name, error.message());
            result = DataResult.success(new Pair<>(this.defaultValue.get(), input));
        }
        return result;
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        DataResult<T> result = this.baseCodec.encode(input, ops, prefix);
        if (result instanceof DataResult.Error<T> error) {
            LOGGER.error("Failed to encode {}: {}", this.name, error.message());
            result = DataResult.success(prefix);
        }
        return result;
    }
}
