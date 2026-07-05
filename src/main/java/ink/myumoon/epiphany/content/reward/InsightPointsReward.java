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
        AptitudeManager.setInsightPoints(player,
                AptitudeManager.getInsightPoints(player) + amount);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        AptitudeManager.setInsightPoints(player,
                Math.max(0, AptitudeManager.getInsightPoints(player) - amount));
    }
}
