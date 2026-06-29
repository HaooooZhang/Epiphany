package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import ink.myumoon.epiphany.util.DefaultedCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

/**
 * A polymorphic reward type for Insights (quantitative / incremental effects).
 * <p>
 * Each Insight can grant one InsightReward when unlocked.
 * Module {@code on_select_reward} and {@code on_complete_reward} also use this type.
 * <p>
 * Extensible via the {@link EpiphanyRegistries#INSIGHT_REWARD_SERIALIZERS} registry.
 */
public interface InsightReward {

    Codec<InsightReward> CODEC = DefaultedCodec.registryDispatch(
            EpiphanyRegistries.INSIGHT_REWARD_SERIALIZERS,
            InsightReward::codec,
            Function.identity(),
            () -> NoOpInsightReward.INSTANCE
    );

    MapCodec<? extends InsightReward> codec();

    /**
     * Applies this reward to the given player.
     *
     * @param player  the target player
     * @param insight the Insight definition that this reward belongs to
     */
    void apply(ServerPlayer player, InsightData insight);
}
