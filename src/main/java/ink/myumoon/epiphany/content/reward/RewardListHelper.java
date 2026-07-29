package ink.myumoon.epiphany.content.reward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

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
        apply(rewards, player, sourceId, sourceKey, InsightReward::apply);
    }

    public static void removeInsight(List<? extends InsightReward> rewards,
                                     ServerPlayer player, ResourceLocation sourceId) {
        removeInsight(rewards, player, sourceId, "reward");
    }

    public static void removeInsight(List<? extends InsightReward> rewards,
                                     ServerPlayer player, ResourceLocation sourceId, String sourceKey) {
        remove(rewards, player, sourceId, sourceKey, InsightReward::remove);
    }

    public static void applyEpiphany(List<? extends EpiphanyReward> rewards,
                                     ServerPlayer player, ResourceLocation sourceId) {
        apply(rewards, player, sourceId, "reward", EpiphanyReward::apply);
    }

    public static void removeEpiphany(List<? extends EpiphanyReward> rewards,
                                      ServerPlayer player, ResourceLocation sourceId) {
        remove(rewards, player, sourceId, "reward", EpiphanyReward::remove);
    }

    public static void reapplyPersistentInsight(List<? extends InsightReward> rewards,
                                                ServerPlayer player, ResourceLocation sourceId) {
        reapplyPersistentInsight(rewards, player, sourceId, "reward");
    }

    public static void reapplyPersistentInsight(List<? extends InsightReward> rewards,
                                                ServerPlayer player, ResourceLocation sourceId, String sourceKey) {
        applyPersistent(rewards, player, sourceId, sourceKey, InsightReward::apply);
    }

    public static void reapplyPersistentEpiphany(List<? extends EpiphanyReward> rewards,
                                                 ServerPlayer player, ResourceLocation sourceId) {
        applyPersistent(rewards, player, sourceId, "reward", EpiphanyReward::apply);
    }

    private static <T> void apply(List<T> rewards, ServerPlayer player,
                                  ResourceLocation sourceId, String sourceKey,
                                  TriConsumer<T, ServerPlayer, ResourceLocation> action) {
        for (int index = 0; index < rewards.size(); index++) {
            action.accept(rewards.get(index), player,
                    elementSourceId(sourceId, sourceKey, index, rewards.size()));
        }
    }

    private static <T> void remove(List<T> rewards, ServerPlayer player,
                                   ResourceLocation sourceId, String sourceKey,
                                   TriConsumer<T, ServerPlayer, ResourceLocation> action) {
        for (int index = rewards.size() - 1; index >= 0; index--) {
            action.accept(rewards.get(index), player,
                    elementSourceId(sourceId, sourceKey, index, rewards.size()));
        }
    }

    private static <T> void applyPersistent(List<T> rewards, ServerPlayer player,
                                            ResourceLocation sourceId, String sourceKey,
                                            TriConsumer<T, ServerPlayer, ResourceLocation> action) {
        for (int index = 0; index < rewards.size(); index++) {
            T reward = rewards.get(index);
            if (reward instanceof PersistentReward) {
                action.accept(reward, player,
                    elementSourceId(sourceId, sourceKey, index, rewards.size()));
            }
        }
    }

    @FunctionalInterface
    private interface TriConsumer<T, U, V> {
        void accept(T first, U second, V third);
    }

    private static ResourceLocation elementSourceId(ResourceLocation sourceId, String sourceKey,
                                                    int index, int size) {
        if (size <= 1) return sourceId;
        return ResourceLocation.fromNamespaceAndPath(
                sourceId.getNamespace(), sourceId.getPath() + "/" + sourceKey + "_" + index);
    }
}