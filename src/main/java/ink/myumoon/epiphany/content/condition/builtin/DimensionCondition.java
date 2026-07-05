package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether the player is in a specific dimension.
 * <p>
 * JSON: {@code {"type": "epiphany:dimension", "dimension": "minecraft:the_nether"}}
 */
public record DimensionCondition(ResourceLocation dimension) implements Condition {

    public static final MapCodec<DimensionCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(DimensionCondition::dimension)
    ).apply(instance, DimensionCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return player.level().dimension().location().equals(dimension);
    }
}
