package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Grants a potion effect. Survives death via {@link PersistentReward}.
 * <p>
 * Permanent effects (duration = -1) are stored in the player's persistent NBT
 * and re-applied every tick via {@code EffectReapplier}, so they survive milk.
 * <p>
 * JSON: {@code {"type": "epiphany:effect", "effect": "minecraft:regeneration",
 * "duration": 600, "amplifier": 0}}<br>
 * Omit duration (or set -1) for permanent.
 */
public record EffectReward(
        Holder<MobEffect> effect,
        int duration,
        int amplifier
) implements InsightReward, EpiphanyReward, PersistentReward {

    private static final String KEY = "epiphany_permanent_effects";

    public static final MapCodec<EffectReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec()
                    .fieldOf("effect").forGetter(EffectReward::effect),
            com.mojang.serialization.Codec.INT.optionalFieldOf("duration", -1)
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
        var instance = new MobEffectInstance(effect, duration, amplifier, false, false, true);
        player.addEffect(instance);
        if (duration == -1) {
            storePermanent(player, effect, amplifier);
        }
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        player.removeEffect(effect);
        removePermanent(player, effect);
    }

    static void storePermanent(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        var data = player.getPersistentData();
        var list = data.getList(KEY, Tag.TAG_STRING);
        if (list.isEmpty()) {
            list = new ListTag();
        }
        String key = effectKey(effect, amplifier);
        // avoid duplicates
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(key)) return;
        }
        list.add(StringTag.valueOf(key));
        data.put(KEY, list);
    }

    static void removePermanent(ServerPlayer player, Holder<MobEffect> effect) {
        var data = player.getPersistentData();
        var list = data.getList(KEY, Tag.TAG_STRING);
        if (list.isEmpty()) return;
        // remove all entries for this effect (any amplifier)
        list.removeIf(tag -> tag instanceof StringTag s && s.getAsString().startsWith(
                effect.unwrapKey().orElseThrow().location().toString()));
        if (list.isEmpty()) data.remove(KEY);
    }

    static String effectKey(Holder<MobEffect> effect, int amplifier) {
        return effect.unwrapKey().orElseThrow().location() + "|" + amplifier;
    }
}
