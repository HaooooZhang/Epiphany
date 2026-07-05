package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.AptitudeManager;
import ink.myumoon.epiphany.content.condition.Comparison;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks the player's insight points against a threshold.
 * <p>
 * JSON: {@code {"type": "epiphany:insight_points", "comparison": ">=", "value": 5}}
 */
public record InsightPointsCondition(
        Comparison comparison,
        int value
) implements Condition {

    public static final MapCodec<InsightPointsCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_OR_EQUAL)
                    .forGetter(InsightPointsCondition::comparison),
            com.mojang.serialization.Codec.INT.fieldOf("value").forGetter(InsightPointsCondition::value)
    ).apply(instance, InsightPointsCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return comparison.test(AptitudeManager.getInsightPoints(player), value);
    }
}
