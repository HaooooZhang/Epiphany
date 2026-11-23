package ink.myumoon.epiphany.content.condition.logic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Logical AND combinator: all child conditions must be satisfied.
 * <p>
 * JSON: {@code {"type": "epiphany:and", "conditions": [...]}}
 */
public record AndCondition(List<Condition> conditions) implements Condition {
    public static final MapCodec<AndCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Condition.CODEC.listOf().fieldOf("conditions").forGetter(AndCondition::conditions)
    ).apply(instance, AndCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return conditions.stream().allMatch(c -> c.test(player));
    }
}
