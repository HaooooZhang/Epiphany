package ink.myumoon.epiphany.content.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Logical NOT combinator: inverts the result of the child condition.
 * <p>
 * JSON: {@code {"type": "epiphany:not", "condition": {...}}}
 */
public record NotCondition(Condition condition) implements Condition {
    public static final MapCodec<NotCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Condition.CODEC.fieldOf("condition").forGetter(NotCondition::condition)
    ).apply(instance, NotCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return !condition.test(player);
    }
}
