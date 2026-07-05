package ink.myumoon.epiphany.registry;

import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.reward.*;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EpiphanyInsightRewardTypes {

    public static final DeferredRegister<MapCodec<? extends InsightReward>> REGISTRY =
            DeferredRegister.create(EpiphanyRegistries.INSIGHT_REWARD_SERIALIZERS, Epiphany.MODID);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<AttributeReward>> ATTRIBUTE =
            REGISTRY.register("attribute", () -> AttributeReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<ItemReward>> ITEM =
            REGISTRY.register("item", () -> ItemReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<CommandReward>> COMMAND =
            REGISTRY.register("command", () -> CommandReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<ExperienceReward>> EXPERIENCE =
            REGISTRY.register("experience", () -> ExperienceReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<ExperienceLevelReward>> EXPERIENCE_LEVEL =
            REGISTRY.register("experience_level", () -> ExperienceLevelReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<EffectReward>> EFFECT =
            REGISTRY.register("effect", () -> EffectReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<ParticleReward>> PARTICLE =
            REGISTRY.register("particle", () -> ParticleReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<UnlockModuleReward>> UNLOCK_MODULE =
            REGISTRY.register("unlock_module", () -> UnlockModuleReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<UnlockEpiphanyReward>> UNLOCK_EPIPHANY =
            REGISTRY.register("unlock_epiphany", () -> UnlockEpiphanyReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<AptitudeReward>> APTITUDE =
            REGISTRY.register("aptitude", () -> AptitudeReward.CODEC);

    public static final DeferredHolder<MapCodec<? extends InsightReward>, MapCodec<InsightPointsReward>> INSIGHT_POINTS =
            REGISTRY.register("insight_points", () -> InsightPointsReward.CODEC);

    private EpiphanyInsightRewardTypes() {
    }
}
