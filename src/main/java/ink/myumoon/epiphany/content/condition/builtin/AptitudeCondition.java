package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.AptitudeManager;
import ink.myumoon.epiphany.content.condition.Comparison;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks the player's aptitude against a threshold.
 * <p>
 * JSON: {@code {"type": "epiphany:aptitude", "comparison": ">=", "value": 500}}
 */
public record AptitudeCondition(
        Comparison comparison,
        long value
) implements Condition {

    public static final MapCodec<AptitudeCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_OR_EQUAL)
                    .forGetter(AptitudeCondition::comparison),
            com.mojang.serialization.Codec.LONG.fieldOf("value").forGetter(AptitudeCondition::value)
    ).apply(instance, AptitudeCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return comparison.test(AptitudeManager.getAptitude(player), value);
    }
}
