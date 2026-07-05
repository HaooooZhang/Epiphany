package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

/**
 * Checks whether the player is in a specific biome.
 * <p>
 * JSON: {@code {"type": "epiphany:biome", "biome": "minecraft:desert"}}
 */
public record BiomeCondition(ResourceKey<Biome> biome) implements Condition {

    public static final MapCodec<BiomeCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.BIOME)
                    .fieldOf("biome").forGetter(BiomeCondition::biome)
    ).apply(instance, BiomeCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return player.level().getBiome(player.blockPosition()).is(biome);
    }
}
