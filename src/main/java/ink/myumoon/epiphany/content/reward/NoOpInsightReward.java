package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;

/**
 * A no-op Insight reward. Does nothing.
 * Used as the default when JSON parsing fails or no reward is specified.
 */
public enum NoOpInsightReward implements InsightReward {
    INSTANCE;

    public static final MapCodec<NoOpInsightReward> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<? extends InsightReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player) {
        // No-op
    }
}
