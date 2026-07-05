package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Gives items to the player. If the inventory is full, drops on the ground.
 * <p>
 * JSON: {@code {"type": "epiphany:item", "item": "minecraft:diamond", "count": 1}}
 */
public record ItemReward(Item item, int count) implements InsightReward, EpiphanyReward {

    public static final MapCodec<ItemReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item").forGetter(ItemReward::item),
            com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1)
                    .forGetter(ItemReward::count)
    ).apply(instance, ItemReward::new));

    @Override
    public MapCodec<ItemReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        var stack = new ItemStack(item, count);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        int remaining = count;
        for (var stack : player.getInventory().items) {
            if (!stack.is(item)) continue;
            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
            if (remaining <= 0) return;
        }
        for (var stack : player.getEnderChestInventory().getItems()) {
            if (!stack.is(item)) continue;
            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
            if (remaining <= 0) return;
        }
    }
}
