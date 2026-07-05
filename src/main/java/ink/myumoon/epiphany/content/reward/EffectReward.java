package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Grants a potion effect. Survives death via {@link PersistentReward}.
 * <p>
 * JSON: {@code {"type": "epiphany:effect", "effect": "minecraft:regeneration",
 * "duration": 600, "amplifier": 0}}
 */
public record EffectReward(
        Holder<MobEffect> effect,
        int duration,
        int amplifier
) implements InsightReward, EpiphanyReward, PersistentReward {

    public static final MapCodec<EffectReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec()
                    .fieldOf("effect").forGetter(EffectReward::effect),
            com.mojang.serialization.Codec.INT.optionalFieldOf("duration", Integer.MAX_VALUE)
                    .forGetter(EffectReward::duration),
            com.mojang.serialization.Codec.INT.optionalFieldOf("amplifier", 0)
                    .forGetter(EffectReward::amplifier)
    ).apply(instance, EffectReward::new));

    @Override
    public MapCodec<EffectReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier,
                false, false, true));
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        player.removeEffect(effect);
    }
}
