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
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Checks whether the player has at least a certain count of an item
 * across inventory and ender chest.
 * <p>
 * Supports single items or tags (prefix with {@code #}).
 * When a tag is specified, the counts of all items in the tag are summed.
 * <p>
 * JSON examples:
 * <ul>
 *   <li>{@code {"type": "epiphany:item", "item": "minecraft:diamond", "count": 5}}</li>
 *   <li>{@code {"type": "epiphany:item", "item": "#minecraft:planks", "count": 64}}</li>
 * </ul>
 */
public record ItemCondition(
        Either<Item, TagKey<Item>> item,
        int count
) implements Condition {

    public static final MapCodec<ItemCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            itemOrTagCodec().fieldOf("item").forGetter(ItemCondition::item),
            com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1)
                    .forGetter(ItemCondition::count)
    ).apply(instance, ItemCondition::new));

    private static Codec<Either<Item, TagKey<Item>>> itemOrTagCodec() {
        return Codec.STRING.xmap(
                str -> {
                    if (str.startsWith("#")) {
                        ResourceLocation rl = ResourceLocation.parse(str.substring(1));
                        return Either.right(TagKey.create(Registries.ITEM, rl));
                    } else {
                        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(str));
                        return Either.left(item);
                    }
                },
                either -> either.map(
                        item -> BuiltInRegistries.ITEM.getKey(item).toString(),
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
        return item.map(
                // Single item — check inventory + ender chest
                single -> {
                    int found = 0;
                    for (var stack : player.getInventory().items) {
                        if (stack.is(single)) {
                            found += stack.getCount();
                            if (found >= count) return true;
                        }
                    }
                    for (var stack : player.getEnderChestInventory().getItems()) {
                        if (stack.is(single)) {
                            found += stack.getCount();
                            if (found >= count) return true;
                        }
                    }
                    return false;
                },
                // Tag — check all items in the tag
                tag -> {
                    int found = 0;
                    for (var stack : player.getInventory().items) {
                        if (stack.is(tag)) {
                            found += stack.getCount();
                            if (found >= count) return true;
                        }
                    }
                    for (var stack : player.getEnderChestInventory().getItems()) {
                        if (stack.is(tag)) {
                            found += stack.getCount();
                            if (found >= count) return true;
                        }
                    }
                    return false;
                }
        );
    }
}
