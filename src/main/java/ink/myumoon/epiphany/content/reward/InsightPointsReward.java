package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.AptitudeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Modifies the player's insight points.
 * <p>
 * JSON: {@code {"type": "epiphany:insight_points", "amount": 3}}
 */
public record InsightPointsReward(int amount) implements InsightReward, EpiphanyReward {

    public static final MapCodec<InsightPointsReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.INT.fieldOf("amount").forGetter(InsightPointsReward::amount)
    ).apply(instance, InsightPointsReward::new));

    @Override
    public MapCodec<InsightPointsReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        apply(player, new RewardSource("legacy_api", sourceId, "reward", sourceId));
    }

    @Override
    public void apply(ServerPlayer player, RewardSource source) {
        AptitudeManager.applyInsightPointsReward(player, sourceKey(source), amount);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        remove(player, new RewardSource("legacy_api", sourceId, "reward", sourceId));
    }

    @Override
    public void remove(ServerPlayer player, RewardSource source) {
        AptitudeManager.removeInsightPointsReward(player, sourceKey(source));
    }

    private static String sourceKey(RewardSource source) {
        // Single rewards and the first element of a list share index zero.
        String prefix = source.ownerId().getPath() + "/" + source.rewardSlot() + "_";
        String index = "0";
        if (source.ownerId().getNamespace().equals(source.legacyId().getNamespace())
                && source.legacyId().getPath().startsWith(prefix)) {
            index = source.legacyId().getPath().substring(prefix.length());
        }
        return source.ownerKind() + "|" + source.ownerId() + "|" + source.rewardSlot() + "|" + index;
    }
}
