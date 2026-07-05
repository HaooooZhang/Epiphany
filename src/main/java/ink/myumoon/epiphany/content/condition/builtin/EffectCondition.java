package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

/**
 * Checks whether the player has a specific potion effect, optionally at a minimum amplifier.
 * <p>
 * JSON: {@code {"type": "epiphany:effect", "effect": "minecraft:regeneration", "min_amplifier": 0}}
 */
public record EffectCondition(
        Holder<MobEffect> effect,
        int minAmplifier
) implements Condition {

    public static final MapCodec<EffectCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec()
                    .fieldOf("effect").forGetter(EffectCondition::effect),
            com.mojang.serialization.Codec.INT.optionalFieldOf("min_amplifier", 0)
                    .forGetter(EffectCondition::minAmplifier)
    ).apply(instance, EffectCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        var active = player.getEffect(effect);
        return active != null && active.getAmplifier() >= minAmplifier;
    }
}
