package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * One-time particle effect at the player's position. Only simple (data-less)
 * particles are supported.
 * <p>
 * JSON: {@code {"type": "epiphany:particle", "particle": "minecraft:flame", "count": 10}}
 */
public record ParticleReward(ResourceLocation particle, int count) implements InsightReward, EpiphanyReward {

    public static final MapCodec<ParticleReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("particle").forGetter(ParticleReward::particle),
            com.mojang.serialization.Codec.INT.optionalFieldOf("count", 10)
                    .forGetter(ParticleReward::count)
    ).apply(instance, ParticleReward::new));

    @Override
    public MapCodec<ParticleReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        var type = BuiltInRegistries.PARTICLE_TYPE.get(particle);
        if (!(type instanceof SimpleParticleType spt)) return;
        var level = player.serverLevel();
        var pos = player.position();
        level.sendParticles(spt, pos.x, pos.y + 1, pos.z, count, 0.5, 0.5, 0.5, 0.1);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        // Particles are transient.
    }
}
