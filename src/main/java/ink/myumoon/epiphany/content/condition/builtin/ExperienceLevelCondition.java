package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Comparison;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks the player's experience level against a threshold.
 * <p>
 * JSON: {@code {"type": "epiphany:experience_level", "comparison": ">=", "value": 30}}
 */
public record ExperienceLevelCondition(
        Comparison comparison,
        int value
) implements Condition {

    public static final MapCodec<ExperienceLevelCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_OR_EQUAL)
                    .forGetter(ExperienceLevelCondition::comparison),
            com.mojang.serialization.Codec.INT.fieldOf("value").forGetter(ExperienceLevelCondition::value)
    ).apply(instance, ExperienceLevelCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return comparison.test(player.experienceLevel, value);
    }
}
