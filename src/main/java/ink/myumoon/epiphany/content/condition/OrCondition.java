package ink.myumoon.epiphany.content.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Logical OR combinator: at least one child condition must be satisfied.
 * <p>
 * JSON: {@code {"type": "epiphany:or", "conditions": [...]}}
 */
public record OrCondition(List<Condition> conditions) implements Condition {
    public static final MapCodec<OrCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Condition.CODEC.listOf().fieldOf("conditions").forGetter(OrCondition::conditions)
    ).apply(instance, OrCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return conditions.stream().anyMatch(c -> c.test(player));
    }
}
