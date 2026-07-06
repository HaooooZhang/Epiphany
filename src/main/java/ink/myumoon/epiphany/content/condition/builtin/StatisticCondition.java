package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Comparison;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;

/**
 * Checks a Minecraft statistic value against a threshold.
 * <p>
 * JSON: {@code {"type": "epiphany:statistic", "stat": "minecraft:walk_one_cm",
 * "comparison": ">=", "value": 100000}}
 */
public record StatisticCondition(
        ResourceLocation statId,
        Comparison comparison,
        int value
) implements Condition {

    public static final MapCodec<StatisticCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("stat").forGetter(StatisticCondition::statId),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_OR_EQUAL)
                    .forGetter(StatisticCondition::comparison),
            Codec.INT.fieldOf("value").forGetter(StatisticCondition::value)
    ).apply(instance, StatisticCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        if (!Stats.CUSTOM.contains(statId)) return false;
        Stat<?> stat = Stats.CUSTOM.get(statId);
        return comparison.test(player.getStats().getValue(stat), value);
    }
}
