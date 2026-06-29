package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import ink.myumoon.epiphany.util.DefaultedCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

/**
 * A polymorphic reward type for Epiphanies (qualitative / gameplay-altering effects).
 * <p>
 * Each Epiphany can grant one EpiphanyReward when selected.
 * <p>
 * Extensible via the {@link EpiphanyRegistries#EPIPHANY_REWARD_SERIALIZERS} registry.
 */
public interface EpiphanyReward {

    Codec<EpiphanyReward> CODEC = DefaultedCodec.registryDispatch(
            EpiphanyRegistries.EPIPHANY_REWARD_SERIALIZERS,
            EpiphanyReward::codec,
            Function.identity(),
            () -> NoOpEpiphanyReward.INSTANCE
    );

    MapCodec<? extends EpiphanyReward> codec();

    /**
     * Applies this reward to the given player.
     *
     * @param player    the target player
     * @param epiphany  the Epiphany definition that this reward belongs to
     */
    void apply(ServerPlayer player, EpiphanyData epiphany);
}
