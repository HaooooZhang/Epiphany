package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.content.EpiphanyData;
import net.minecraft.server.level.ServerPlayer;

/**
 * A no-op Epiphany reward. Does nothing.
 * Used as the default when JSON parsing fails or no reward is specified.
 */
public enum NoOpEpiphanyReward implements EpiphanyReward {
    INSTANCE;

    public static final MapCodec<NoOpEpiphanyReward> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<? extends EpiphanyReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, EpiphanyData epiphany) {
        // No-op
    }
}
