package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Block;

/**
 * Checks how many times the player has broken a specific block.
 * Event-driven: {@code BlockEvent.BreakEvent} → {@code checkAutoUnlock()}.
 * <p>
 * JSON: {@code {"type": "epiphany:block_broken", "block": "minecraft:stone", "count": 100}}
 */
public record BlockBrokenCondition(Block block, int count) implements Condition {

    public static final MapCodec<BlockBrokenCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec()
                    .fieldOf("block").forGetter(BlockBrokenCondition::block),
            com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1)
                    .forGetter(BlockBrokenCondition::count)
    ).apply(instance, BlockBrokenCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return player.getStats().getValue(Stats.BLOCK_MINED.get(block)) >= count;
    }
}
