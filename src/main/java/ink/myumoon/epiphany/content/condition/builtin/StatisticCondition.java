package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Comparison;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks a Minecraft statistic value against a threshold.
 * <p>
 * Custom statistic JSON: {@code {"type": "epiphany:statistic",
 * "stat": "minecraft:walk_one_cm", "comparison": ">=", "value": 100000}}
 * <p>Typed statistic JSON: {@code {"type": "epiphany:statistic",
 * "stat_type": "minecraft:crafted", "stat": "minecraft:iron_pickaxe",
 * "comparison": ">=", "value": 2}}
 */
public record StatisticCondition(
        Optional<ResourceLocation> statTypeId,
        ResourceLocation statId,
        Comparison comparison,
        int value
) implements Condition {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> LOGGED_INVALID_STATS = ConcurrentHashMap.newKeySet();

    public static final MapCodec<StatisticCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("stat_type").forGetter(StatisticCondition::statTypeId),
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
        return statTypeId
                .map(typeId -> testTypedStatistic(player, typeId))
                .orElseGet(() -> testCustomStatistic(player));
    }

    private boolean testCustomStatistic(ServerPlayer player) {
        ResourceLocation statValue = Stats.CUSTOM.getRegistry().get(statId);
        if (statValue == null) {
            logInvalidStatistic(ResourceLocation.withDefaultNamespace("custom"), "unknown custom statistic");
            return false;
        }
        return comparison.test(player.getStats().getValue(Stats.CUSTOM.get(statValue)), value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean testTypedStatistic(ServerPlayer player, ResourceLocation typeId) {
        StatType statType = BuiltInRegistries.STAT_TYPE.get(typeId);
        if (statType == null) {
            logInvalidStatistic(typeId, "unknown statistic type");
            return false;
        }

        Registry valueRegistry = statType.getRegistry();
        Object statValue = valueRegistry.get(statId);
        if (statValue == null) {
            logInvalidStatistic(typeId, "unknown statistic target");
            return false;
        }

        Stat<?> stat = statType.get(statValue);
        return comparison.test(player.getStats().getValue(stat), value);
    }

    private void logInvalidStatistic(ResourceLocation typeId, String reason) {
        String key = typeId + "/" + statId;
        if (LOGGED_INVALID_STATS.add(key)) {
            LOGGER.warn("[StatisticCondition] {}: type='{}', stat='{}'", reason, typeId, statId);
        }
    }
}
