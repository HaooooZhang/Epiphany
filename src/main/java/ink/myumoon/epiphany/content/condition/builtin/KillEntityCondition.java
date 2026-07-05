package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;

/**
 * Checks how many times the player has killed a specific entity type.
 * Event-driven: {@code LivingDeathEvent} (source=player) → {@code checkAutoUnlock()}.
 * <p>
 * JSON: {@code {"type": "epiphany:kill_entity", "entity": "minecraft:zombie", "count": 10}}
 */
public record KillEntityCondition(EntityType<?> entityType, int count) implements Condition {

    public static final MapCodec<KillEntityCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec()
                    .fieldOf("entity").forGetter(KillEntityCondition::entityType),
            com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1)
                    .forGetter(KillEntityCondition::count)
    ).apply(instance, KillEntityCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return player.getStats().getValue(Stats.ENTITY_KILLED.get(entityType)) >= count;
    }
}
