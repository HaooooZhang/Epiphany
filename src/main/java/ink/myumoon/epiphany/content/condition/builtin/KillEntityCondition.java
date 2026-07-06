package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Checks how many times the player has killed a specific entity type.
 * Supports single entity types or tags (prefix with {@code #}).
 * When a tag is specified, the counts of all entity types in the tag are summed.
 * <p>
 * JSON examples:
 * <ul>
 *   <li>{@code {"type": "epiphany:kill_entity", "entity": "minecraft:zombie", "count": 10}}</li>
 *   <li>{@code {"type": "epiphany:kill_entity", "entity": "#minecraft:skeletons", "count": 10}}</li>
 * </ul>
 */
public record KillEntityCondition(
        Either<EntityType<?>, TagKey<EntityType<?>>> entity,
        int count
) implements Condition {

    public static final MapCodec<KillEntityCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            entityOrTagCodec().fieldOf("entity").forGetter(KillEntityCondition::entity),
            Codec.INT.optionalFieldOf("count", 1).forGetter(KillEntityCondition::count)
    ).apply(instance, KillEntityCondition::new));

    private static Codec<Either<EntityType<?>, TagKey<EntityType<?>>>> entityOrTagCodec() {
        return Codec.STRING.xmap(
                str -> {
                    if (str.startsWith("#")) {
                        ResourceLocation rl = ResourceLocation.parse(str.substring(1));
                        return Either.right(TagKey.create(Registries.ENTITY_TYPE, rl));
                    } else {
                        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(str));
                        return Either.left(type);
                    }
                },
                either -> either.map(
                        type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                        tag -> "#" + tag.location()
                )
        );
    }

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return entity.map(
                // Single entity type — direct stat lookup
                type -> player.getStats().getValue(Stats.ENTITY_KILLED.get(type)) >= count,
                // Tag — sum up stats for all entity types in the tag
                tag -> {
                    int total = 0;
                    for (var holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
                        total += player.getStats().getValue(Stats.ENTITY_KILLED.get(holder.value()));
                        if (total >= count) return true;
                    }
                    return false;
                }
        );
    }
}
