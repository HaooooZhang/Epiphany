package ink.myumoon.epiphany.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.logging.LogUtils;
import ink.myumoon.epiphany.EpiphanyConstants;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.registries.IForgeRegistry;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.Supplier;

/** Supplies the baseline fallback for malformed condition or reward definitions. */
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

    public static <T, A> Codec<T> registryDispatch(
            Supplier<IForgeRegistry<A>> registrySupplier,
            Function<? super T, ? extends A> type,
            Function<? super A, ? extends MapCodec<? extends T>> codec,
            Supplier<T> defaultValue
    ) {
        // DataPackRegistryEvent supplies content codecs before Forge creates the type registries.
        return ExtraCodecs.lazyInitializedCodec(() -> {
            IForgeRegistry<A> registry = registrySupplier.get();
            Codec<T> dispatch = registry.getCodec().dispatch(EpiphanyConstants.TYPE_KEY, type,
                    entry -> codec.apply(entry).codec());
            return new DefaultedCodec<>(dispatch, defaultValue, registry.getRegistryName().toString());
        });
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        DataResult<Pair<A, T>> result = baseCodec.decode(ops, input);
        if (result.error().isPresent()) {
            LOGGER.error("Failed to decode {}: {}", name, result.error().orElseThrow().message());
            return DataResult.success(Pair.of(defaultValue.get(), input));
        }
        return result;
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        DataResult<T> result = baseCodec.encode(input, ops, prefix);
        if (result.error().isPresent()) {
            LOGGER.error("Failed to encode {}: {}", name, result.error().orElseThrow().message());
            return DataResult.success(prefix);
        }
        return result;
    }
}
