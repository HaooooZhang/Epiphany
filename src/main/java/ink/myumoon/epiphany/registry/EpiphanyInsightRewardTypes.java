package ink.myumoon.epiphany.registry;

import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.reward.AttributeReward;
import ink.myumoon.epiphany.content.reward.InsightReward;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Epiphany's built-in InsightReward types into the
 * {@link EpiphanyRegistries#INSIGHT_REWARD_SERIALIZERS} registry.
 */
@SuppressWarnings("unused")
public final class EpiphanyInsightRewardTypes {

    public static final DeferredRegister<MapCodec<? extends InsightReward>> REGISTRY =
            DeferredRegister.create(EpiphanyRegistries.INSIGHT_REWARD_SERIALIZERS, Epiphany.MODID);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<AttributeReward>> ATTRIBUTE =
            REGISTRY.register("attribute", () -> AttributeReward.CODEC);

    private EpiphanyInsightRewardTypes() {
    }
}
