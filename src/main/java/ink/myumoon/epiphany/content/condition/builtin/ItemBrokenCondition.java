package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Checks how many items of a type the player has broken through durability loss.
 * Supports a single item or an item tag (prefixed with {@code #}).
 * <p>
 * JSON: {@code {"type": "epiphany:item_broken", "item": "minecraft:iron_pickaxe", "count": 1}}
 */
public record ItemBrokenCondition(
        Either<Item, TagKey<Item>> item,
        int count
) implements Condition {

    public static final MapCodec<ItemBrokenCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStatisticConditionSupport.itemOrTagCodec().fieldOf("item").forGetter(ItemBrokenCondition::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemBrokenCondition::count)
    ).apply(instance, ItemBrokenCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return ItemStatisticConditionSupport.hasAtLeast(player, item, count, Stats.ITEM_BROKEN);
    }
}
