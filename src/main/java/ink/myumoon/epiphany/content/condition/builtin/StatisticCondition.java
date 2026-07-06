package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Comparison;
import ink.myumoon.epiphany.content.condition.Condition;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import org.slf4j.Logger;

import java.lang.reflect.Field;

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

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Field STATS_FIELD;

    static {
        Field f = null;
        try {
            f = StatsCounter.class.getDeclaredField("stats");
            f.setAccessible(true);
        } catch (Exception e) {
            LOGGER.error("Failed to access StatsCounter.stats field", e);
        }
        STATS_FIELD = f;
    }

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
    @SuppressWarnings("unchecked")
    public boolean test(ServerPlayer player) {
        if (STATS_FIELD == null) return false;
        try {
            Object2IntMap<Stat<?>> stats = (Object2IntMap<Stat<?>>) STATS_FIELD.get(player.getStats());
            for (Object2IntMap.Entry<Stat<?>> entry : stats.object2IntEntrySet()) {
                Stat<?> stat = entry.getKey();
                if (stat.getType() == Stats.CUSTOM && statId.equals(stat.getValue())) {
                    return comparison.test(entry.getIntValue(), value);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[StatisticCondition] Failed to read player stats for '{}'", statId, e);
        }
        return false;
    }
}
