package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.Epiphany;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        private static final Codec<Integer> DURATION_CODEC = Codec.INT.validate(value ->
            value == -1 || value > 0
                ? DataResult.success(value)
                : DataResult.error(() -> "Effect duration must be -1 or greater than 0"));
        private static final Codec<Integer> AMPLIFIER_CODEC = Codec.intRange(0, Integer.MAX_VALUE);

    public static final MapCodec<EffectReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec()
                    .fieldOf("effect").forGetter(EffectReward::effect),
                DURATION_CODEC.optionalFieldOf("duration", -1)
                    .forGetter(EffectReward::duration),
                AMPLIFIER_CODEC.optionalFieldOf("amplifier", 0)
                    .forGetter(EffectReward::amplifier)
    ).apply(instance, EffectReward::new));

    @Override
    public MapCodec<EffectReward> codec() {
        return CODEC;
    }

    @Override
    /**
     * Compatibility entry point for callers that do not provide a stable reward source.
     * Permanent effects applied this way are removable in the same session, but are not
     * retained by the player-state rebuild performed after login or datapack reload.
     */
    @Deprecated(forRemoval = false)
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        if (duration != -1) {
            applyEffect(player);
            return;
        }
        upsertPermanent(player, new RewardSource("legacy_api", sourceId, "reward", sourceId),
                effect, amplifier);
        reapplyStoredEffects(player);
    }

    @Override
    public void apply(ServerPlayer player, RewardSource source) {
        if (duration != -1) {
            applyEffect(player);
            return;
        }
        upsertPermanent(player, source, effect, amplifier);
        reapplyStoredEffects(player);
    }

    @Override
    /** Compatibility counterpart to {@link #apply(ServerPlayer, ResourceLocation)}. */
    @Deprecated(forRemoval = false)
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        if (duration != -1) return;
        int removedAmplifier = removeLegacyApiSource(player, sourceId, effect);
        if (removedAmplifier != Integer.MIN_VALUE) {
            reconcileEffect(player, effect, removedAmplifier);
        }
    }

    @Override
    public void remove(ServerPlayer player, RewardSource source) {
        if (duration != -1) return;
        int removedAmplifier = removeSource(player, source, effect);
        if (removedAmplifier != Integer.MIN_VALUE) {
            reconcileEffect(player, effect, removedAmplifier);
        }
    }

    private void applyEffect(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }

    private static void upsertPermanent(ServerPlayer player, RewardSource source,
                                         Holder<MobEffect> effect, int amplifier) {
        var data = player.getPersistentData();
        var list = readList(data);
        var records = new ArrayList<Record>(list.size() + 1);
        String effectId = effectId(effect);
        boolean updated = false;
        boolean migrateLegacy = !source.ownerKind().equals("legacy_api");
        for (int i = 0; i < list.size(); i++) {
            Tag tag = list.get(i);
            Record record = Record.read(tag);
            if (record == null) continue;
            // A precise active source supersedes source-less data from old saves.
            if (migrateLegacy && record.source() == null && record.effectId().equals(effectId)) {
                continue;
            }
            if (record.matches(source, effectId)) {
                records.add(new Record(source, effectId, amplifier));
                updated = true;
            } else {
                records.add(record);
            }
        }
        if (!updated) records.add(new Record(source, effectId, amplifier));
        writeRecords(data, records);
    }

    private static int removeSource(ServerPlayer player, RewardSource source, Holder<MobEffect> effect) {
        var data = player.getPersistentData();
        var list = readList(data);
        String effectId = effectId(effect);
        var records = new ArrayList<Record>(list.size());
        int removedAmplifier = Integer.MIN_VALUE;
        for (int i = 0; i < list.size(); i++) {
            Tag tag = list.get(i);
            Record record = Record.read(tag);
            if (record != null && record.matches(source, effectId)) {
                removedAmplifier = Math.max(removedAmplifier, record.amplifier());
                continue;
            }
            if (record != null) records.add(record);
        }
        writeRecords(data, records);
        return removedAmplifier;
    }

    private static int removeLegacyApiSource(ServerPlayer player, ResourceLocation sourceId,
                                             Holder<MobEffect> effect) {
        var data = player.getPersistentData();
        var list = readList(data);
        String effectId = effectId(effect);
        var records = new ArrayList<Record>(list.size());
        int removedAmplifier = Integer.MIN_VALUE;
        for (int i = 0; i < list.size(); i++) {
            Record record = Record.read(list.get(i));
            if (record == null) continue;
            boolean matchingSource = record.source() != null
                    && record.source().ownerKind().equals("legacy_api")
                    && record.source().ownerId().equals(sourceId);
            if (matchingSource && record.effectId().equals(effectId)) {
                removedAmplifier = Math.max(removedAmplifier, record.amplifier());
                continue;
            }
            records.add(record);
        }
        writeRecords(data, records);
        return removedAmplifier;
    }

    public static void reapplyStoredEffects(ServerPlayer player) {
        var list = readList(player.getPersistentData());
        var best = new java.util.HashMap<String, Integer>();
        for (int i = 0; i < list.size(); i++) {
            Record record = Record.read(list.get(i));
            if (record != null) best.merge(record.effectId(), record.amplifier(), Math::max);
        }
        for (var entry : best.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) continue;
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (holder == null) continue;
            MobEffectInstance current = player.getEffect(holder);
            if (current == null || current.getAmplifier() < entry.getValue()) {
                player.addEffect(new MobEffectInstance(holder, -1, entry.getValue(), false, false, true), null);
            }
        }
    }

    static Map<String, Integer> clearTrackedSources(ServerPlayer player) {
        var data = player.getPersistentData();
        var list = readList(data);
        var legacy = new ArrayList<Record>();
        var previous = new HashMap<String, Integer>();
        for (int i = 0; i < list.size(); i++) {
            Record record = Record.read(list.get(i));
            if (record == null) continue;
            previous.merge(record.effectId(), record.amplifier(), Math::max);
            if (record.source() == null) {
                legacy.add(record);
            }
        }
        writeRecords(data, legacy);
        return previous;
    }

    static void finishSourceRebuild(ServerPlayer player, Map<String, Integer> previous) {
        var list = readList(player.getPersistentData());
        var current = new HashMap<String, Integer>();
        for (int i = 0; i < list.size(); i++) {
            Record record = Record.read(list.get(i));
            if (record != null) current.merge(record.effectId(), record.amplifier(), Math::max);
        }
        for (var entry : previous.entrySet()) {
            Integer currentAmplifier = current.get(entry.getKey());
            if (currentAmplifier != null && currentAmplifier.equals(entry.getValue())) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) continue;
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (holder == null) continue;
            reconcileEffect(player, holder, entry.getValue());
        }
        reapplyStoredEffects(player);
    }

    private static void reconcileEffect(ServerPlayer player, Holder<MobEffect> effect,
                                        int removedAmplifier) {
        var list = readList(player.getPersistentData());
        String effectId = effectId(effect);
        Integer managedAmplifier = null;
        for (int i = 0; i < list.size(); i++) {
            Record record = Record.read(list.get(i));
            if (record != null && record.effectId().equals(effectId)) {
                managedAmplifier = managedAmplifier == null
                        ? record.amplifier() : Math.max(managedAmplifier, record.amplifier());
            }
        }
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() > removedAmplifier) {
            if (current.getDuration() > 0) {
                MobEffectInstance visibleEffect = new MobEffectInstance(
                        effect, current.getDuration(), current.getAmplifier(),
                        current.isAmbient(), current.isVisible(), current.showIcon());
                player.removeEffect(effect);
                player.addEffect(visibleEffect, null);
            } else {
                Epiphany.LOGGER.debug(
                        "Keeping higher permanent effect {} while removing Epiphany amplifier {} from {}",
                        effectId, removedAmplifier, player.getGameProfile().getName());
            }
            return;
        }
        if (managedAmplifier == null) {
            if (current != null) player.removeEffect(effect);
            return;
        }
        if (current == null || current.getAmplifier() != managedAmplifier) {
            player.removeEffect(effect);
            player.addEffect(new MobEffectInstance(
                    effect, -1, managedAmplifier, false, false, true), null);
        }
    }

    private static void writeRecords(CompoundTag data, List<Record> records) {
        if (records.isEmpty()) {
            data.remove(KEY);
            return;
        }
        ListTag list = new ListTag();
        records.stream().sorted(Comparator.comparing(Record::effectId))
            .map(Record::write).forEach(list::add);
        data.put(KEY, list);
    }

    private static String effectId(Holder<MobEffect> effect) {
        return effect.unwrapKey().orElseThrow().location().toString();
    }

    private static ListTag readList(net.minecraft.nbt.CompoundTag data) {
        Tag tag = data.get(KEY);
        return tag instanceof ListTag list ? list : new ListTag();
    }

    private record Record(RewardSource source, String effectId, int amplifier) {
        private static Record read(Tag tag) {
            if (tag instanceof StringTag string) {
                String[] parts = string.getAsString().split("\\|", 2);
                if (parts.length != 2) return null;
                try {
                    return new Record(null, parts[0], Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            if (!(tag instanceof net.minecraft.nbt.CompoundTag compound)) return null;
            if (compound.getBoolean("legacy")) {
                String effectId = compound.getString("effect");
                return effectId.isEmpty() ? null
                        : new Record(null, effectId, compound.getInt("amplifier"));
            }
            ResourceLocation ownerId = ResourceLocation.tryParse(compound.getString("owner_id"));
            String ownerKind = compound.getString("owner_kind");
            String rewardSlot = compound.getString("reward_slot");
            if (ownerId == null || ownerKind.isEmpty() || rewardSlot.isEmpty()) return null;
            ResourceLocation legacyId = ResourceLocation.tryParse(compound.getString("legacy_id"));
            RewardSource source = legacyId == null
                    ? new RewardSource(ownerKind, ownerId, rewardSlot)
                    : new RewardSource(ownerKind, ownerId, rewardSlot, legacyId);
            return new Record(source, compound.getString("effect"), compound.getInt("amplifier"));
        }

        private boolean matches(RewardSource source, String effectId) {
            return this.source != null && this.source.ownerKind().equals(source.ownerKind())
                    && this.source.ownerId().equals(source.ownerId())
                    && this.source.rewardSlot().equals(source.rewardSlot())
                    && this.effectId.equals(effectId);
        }

        private Tag write() {
            var compound = new CompoundTag();
            if (source == null) {
                compound.putBoolean("legacy", true);
            } else {
                compound.putString("owner_kind", source.ownerKind());
                compound.putString("owner_id", source.ownerId().toString());
                compound.putString("reward_slot", source.rewardSlot());
                compound.putString("legacy_id", source.legacyId().toString());
            }
            compound.putString("effect", effectId);
            compound.putInt("amplifier", amplifier);
            return compound;
        }
    }
}
