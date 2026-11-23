package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

final class ItemStatisticConditionSupport {

    private ItemStatisticConditionSupport() {
    }

    static Codec<Either<Item, TagKey<Item>>> itemOrTagCodec() {
        return Codec.STRING.xmap(
                value -> {
                    if (value.startsWith("#")) {
                        return Either.right(TagKey.create(Registries.ITEM, ResourceLocation.parse(value.substring(1))));
                    }
                    return Either.left(BuiltInRegistries.ITEM.get(ResourceLocation.parse(value)));
                },
                itemOrTag -> itemOrTag.map(
                        item -> BuiltInRegistries.ITEM.getKey(item).toString(),
                        tag -> "#" + tag.location()
                )
        );
    }

    static boolean hasAtLeast(ServerPlayer player,
                              Either<Item, TagKey<Item>> itemOrTag,
                              int count,
                              StatType<Item> statType) {
        return itemOrTag.map(
                item -> player.getStats().getValue(statType.get(item)) >= count,
                tag -> {
                    int total = 0;
                    for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                        total += player.getStats().getValue(statType.get(holder.value()));
                        if (total >= count) return true;
                    }
                    return false;
                }
        );
    }
}
