package ink.myumoon.epiphany.content.reward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared execution rules for data-defined reward lists. */
public final class RewardListHelper {
    private RewardListHelper() {
    }

    public static void applyInsight(List<? extends InsightReward> rewards,
                                    ServerPlayer player, ResourceLocation sourceId) {
        applyInsight(rewards, player, sourceId, "reward");
    }

    public static void applyInsight(List<? extends InsightReward> rewards,
                                    ServerPlayer player, ResourceLocation sourceId, String sourceKey) {
        apply(rewards, player, sourceId, sourceKey, insightKind(sourceKey), InsightReward::apply);
    }

    public static void removeInsight(List<? extends InsightReward> rewards,
                                     ServerPlayer player, ResourceLocation sourceId) {
        removeInsight(rewards, player, sourceId, "reward");
    }

    public static void removeInsight(List<? extends InsightReward> rewards,
                                     ServerPlayer player, ResourceLocation sourceId, String sourceKey) {
        remove(rewards, player, sourceId, sourceKey, insightKind(sourceKey), InsightReward::remove);
    }

    public static void applyEpiphany(List<? extends EpiphanyReward> rewards,
                                     ServerPlayer player, ResourceLocation sourceId) {
        apply(rewards, player, sourceId, "reward", "epiphany", EpiphanyReward::apply);
    }

    public static void removeEpiphany(List<? extends EpiphanyReward> rewards,
                                      ServerPlayer player, ResourceLocation sourceId) {
        remove(rewards, player, sourceId, "reward", "epiphany", EpiphanyReward::remove);
    }

    public static void reapplyPersistentInsight(List<? extends InsightReward> rewards,
                                                ServerPlayer player, ResourceLocation sourceId) {
        reapplyPersistentInsight(rewards, player, sourceId, "reward");
    }

    public static void reapplyPersistentEffects(List<? extends InsightReward> rewards,
                                                ServerPlayer player, ResourceLocation sourceId,
                                                String sourceKey) {
        applyPersistentEffects(rewards, player, sourceId, sourceKey,
                insightKind(sourceKey), InsightReward::apply);
    }

    public static void reapplyPersistentAttributes(List<? extends InsightReward> rewards,
                                                   ServerPlayer player, ResourceLocation sourceId,
                                                   String sourceKey) {
        applyPersistentAttributes(rewards, player, sourceId, sourceKey,
                insightKind(sourceKey), InsightReward::apply);
    }

    public static void reapplyPersistentInsight(List<? extends InsightReward> rewards,
                                                ServerPlayer player, ResourceLocation sourceId, String sourceKey) {
        applyPersistent(rewards, player, sourceId, sourceKey, insightKind(sourceKey), InsightReward::apply);
    }

    public static void reapplyPersistentEpiphany(List<? extends EpiphanyReward> rewards,
                                                 ServerPlayer player, ResourceLocation sourceId) {
        applyPersistent(rewards, player, sourceId, "reward", "epiphany", EpiphanyReward::apply);
    }

    public static void reapplyPersistentEffects(List<? extends EpiphanyReward> rewards,
                                                 ServerPlayer player, ResourceLocation sourceId) {
        applyPersistentEffects(rewards, player, sourceId, "reward", "epiphany", EpiphanyReward::apply);
    }

    public static void reapplyPersistentAttributes(List<? extends EpiphanyReward> rewards,
                                                    ServerPlayer player, ResourceLocation sourceId) {
        applyPersistentAttributes(rewards, player, sourceId, "reward", "epiphany", EpiphanyReward::apply);
    }

    private static <T> void apply(List<T> rewards, ServerPlayer player,
                                  ResourceLocation sourceId, String sourceKey,
                                  String ownerKind,
                                  TriConsumer<T, ServerPlayer, RewardSource> action) {
        Map<ResourceLocation, Integer> permanentEffects = permanentEffectAmplifiers(rewards);
        for (int index = 0; index < rewards.size(); index++) {
            T reward = rewards.get(index);
            if (skipDuplicatePermanentEffect(reward, permanentEffects)) continue;
            action.accept(reward, player, source(sourceId, sourceKey, ownerKind, index, rewards.size()));
        }
    }

    private static <T> void remove(List<T> rewards, ServerPlayer player,
                                   ResourceLocation sourceId, String sourceKey,
                                   String ownerKind,
                                   TriConsumer<T, ServerPlayer, RewardSource> action) {
        for (int index = rewards.size() - 1; index >= 0; index--) {
            action.accept(rewards.get(index), player, source(sourceId, sourceKey, ownerKind, index, rewards.size()));
        }
    }

    private static <T> void applyPersistent(List<T> rewards, ServerPlayer player,
                                            ResourceLocation sourceId, String sourceKey,
                                            String ownerKind,
                                            TriConsumer<T, ServerPlayer, RewardSource> action) {
        Map<ResourceLocation, Integer> permanentEffects = permanentEffectAmplifiers(rewards);
        for (int index = 0; index < rewards.size(); index++) {
            T reward = rewards.get(index);
            if (isReapplicablePersistent(reward)
                    && !skipDuplicatePermanentEffect(reward, permanentEffects)) {
                action.accept(reward, player, source(sourceId, sourceKey, ownerKind, index, rewards.size()));
            }
        }
    }

    private static <T> void applyPersistentEffects(List<T> rewards, ServerPlayer player,
                                                   ResourceLocation sourceId, String sourceKey,
                                                   String ownerKind,
                                                   TriConsumer<T, ServerPlayer, RewardSource> action) {
        Map<ResourceLocation, Integer> permanentEffects = permanentEffectAmplifiers(rewards);
        for (int index = 0; index < rewards.size(); index++) {
            T reward = rewards.get(index);
            if (reward instanceof EffectReward effect && effect.duration() == -1
                    && !skipDuplicatePermanentEffect(reward, permanentEffects)) {
                action.accept(reward, player, source(sourceId, sourceKey, ownerKind, index, rewards.size()));
            }
        }
    }

    private static <T> void applyPersistentAttributes(List<T> rewards, ServerPlayer player,
                                                      ResourceLocation sourceId, String sourceKey,
                                                      String ownerKind,
                                                      TriConsumer<T, ServerPlayer, RewardSource> action) {
        for (int index = 0; index < rewards.size(); index++) {
            T reward = rewards.get(index);
            if (reward instanceof AttributeReward) {
                action.accept(reward, player, source(sourceId, sourceKey, ownerKind, index, rewards.size()));
            }
        }
    }

    static boolean isReapplicablePersistent(Object reward) {
        return reward instanceof PersistentReward
                && (!(reward instanceof EffectReward effect) || effect.duration() == -1);
    }

    @FunctionalInterface
    private interface TriConsumer<T, U, V> {
        void accept(T first, U second, V third);
    }

    private static String insightKind(String sourceKey) {
        return sourceKey.startsWith("on_") ? "module" : "insight";
    }

    private static RewardSource source(ResourceLocation sourceId, String sourceKey,
                                       String ownerKind, int index, int size) {
        return new RewardSource(ownerKind, sourceId, sourceKey,
                elementSourceId(sourceId, sourceKey, index, size));
    }

    private static <T> Map<ResourceLocation, Integer> permanentEffectAmplifiers(List<T> rewards) {
        Map<ResourceLocation, Integer> result = new HashMap<>();
        for (T reward : rewards) {
            if (reward instanceof EffectReward effect && effect.duration() == -1) {
                ResourceLocation id = effect.effect().unwrapKey().orElseThrow().location();
                result.merge(id, effect.amplifier(), Math::max);
            }
        }
        return result;
    }

    private static boolean skipDuplicatePermanentEffect(
            Object reward, Map<ResourceLocation, Integer> permanentEffects) {
        if (!(reward instanceof EffectReward effect) || effect.duration() != -1) return false;
        ResourceLocation id = effect.effect().unwrapKey().orElseThrow().location();
        Integer maximum = permanentEffects.remove(id);
        if (maximum == null) return true;
        if (effect.amplifier() == maximum) return false;
        permanentEffects.put(id, maximum);
        return true;
    }

    private static ResourceLocation elementSourceId(ResourceLocation sourceId, String sourceKey,
                                                    int index, int size) {
        if (size <= 1) return sourceId;
        return ResourceLocation.fromNamespaceAndPath(
                sourceId.getNamespace(), sourceId.getPath() + "/" + sourceKey + "_" + index);
    }
}