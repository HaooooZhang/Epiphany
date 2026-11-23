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
import net.minecraft.world.level.block.Block;

/**
 * Checks how many blocks of a type the player has broken.
 * <p>
 * Supports single blocks or tags (prefix with {@code #}).
 * When a tag is specified, the counts of all blocks in the tag are summed.
 * <p>
 * JSON examples:
 * <ul>
 *   <li>{@code {"type": "epiphany:block_broken", "block": "minecraft:oak_log", "count": 10}}</li>
 *   <li>{@code {"type": "epiphany:block_broken", "block": "#minecraft:logs", "count": 10}}</li>
 * </ul>
 */
public record BlockBrokenCondition(
        Either<Block, TagKey<Block>> block,
        int count
) implements Condition {

    public static final MapCodec<BlockBrokenCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            blockOrTagCodec().fieldOf("block").forGetter(BlockBrokenCondition::block),
            Codec.INT.optionalFieldOf("count", 1).forGetter(BlockBrokenCondition::count)
    ).apply(instance, BlockBrokenCondition::new));

    private static Codec<Either<Block, TagKey<Block>>> blockOrTagCodec() {
        return Codec.STRING.xmap(
                str -> {
                    if (str.startsWith("#")) {
                        ResourceLocation rl = ResourceLocation.parse(str.substring(1));
                        return Either.right(TagKey.create(Registries.BLOCK, rl));
                    } else {
                        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(str));
                        return Either.left(block);
                    }
                },
                either -> either.map(
                        block -> BuiltInRegistries.BLOCK.getKey(block).toString(),
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
        return block.map(
                // Single block — direct stat lookup
                b -> player.getStats().getValue(Stats.BLOCK_MINED.get(b)) >= count,
                // Tag — sum up stats for all blocks in the tag
                tag -> {
                    int total = 0;
                    for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                        total += player.getStats().getValue(Stats.BLOCK_MINED.get(holder.value()));
                        if (total >= count) return true;
                    }
                    return false;
                }
        );
    }
}
