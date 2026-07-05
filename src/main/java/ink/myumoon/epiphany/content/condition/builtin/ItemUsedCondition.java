package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;

/**
 * Checks how many times the player has used a specific item.
 * Event-driven: {@code PlayerInteractEvent.RightClickItem} → {@code checkAutoUnlock()}.
 * <p>
 * JSON: {@code {"type": "epiphany:item_used", "item": "minecraft:diamond", "count": 1}}
 */
public record ItemUsedCondition(Item item, int count) implements Condition {

    public static final MapCodec<ItemUsedCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item").forGetter(ItemUsedCondition::item),
            com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1)
                    .forGetter(ItemUsedCondition::count)
    ).apply(instance, ItemUsedCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return player.getStats().getValue(Stats.ITEM_USED.get(item)) >= count;
    }
}
