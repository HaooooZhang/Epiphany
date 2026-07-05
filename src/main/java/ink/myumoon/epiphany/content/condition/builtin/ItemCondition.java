package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * Checks whether the player has at least a certain count of an item
 * across inventory and ender chest.
 * <p>
 * JSON: {@code {"type": "epiphany:item", "item": "minecraft:diamond", "count": 5}}
 */
public record ItemCondition(
        Item item,
        int count
) implements Condition {

    public static final MapCodec<ItemCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item").forGetter(ItemCondition::item),
            com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1)
                    .forGetter(ItemCondition::count)
    ).apply(instance, ItemCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        int found = 0;
        for (var stack : player.getInventory().items) {
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        for (var stack : player.getEnderChestInventory().getItems()) {
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }
}
