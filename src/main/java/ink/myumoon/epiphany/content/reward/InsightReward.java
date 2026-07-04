package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import ink.myumoon.epiphany.util.DefaultedCodec;
import net.minecraft.resources.ResourceLocation;
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
     * @param player   the server-side player
     * @param sourceId the registry ID of the Insight or Module that grants this reward
     */
    void apply(ServerPlayer player, ResourceLocation sourceId);

    /**
     * Removes this reward from the given player.
     *
     * @param player   the server-side player
     * @param sourceId the registry ID of the Insight or Module that granted this reward
     */
    default void remove(ServerPlayer player, ResourceLocation sourceId) {}
}
