package ink.myumoon.epiphany.content.reward;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.List;
import java.util.function.Function;

/**
 * Decodes a reward field from either the legacy single object form or the list form.
 */
public final class RewardListCodec {
    private RewardListCodec() {
    }

    public static <T> Codec<List<T>> objectOrList(Codec<T> elementCodec) {
        Codec<Either<List<T>, T>> objectOrList = Codec.either(elementCodec.listOf(), elementCodec);
        return objectOrList.xmap(
            value -> value.map(Function.identity(), List::of),
                value -> value.size() == 1
                ? Either.right(value.get(0))
                : Either.left(value)
        );
    }
}