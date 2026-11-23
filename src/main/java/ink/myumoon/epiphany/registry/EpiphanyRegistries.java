package ink.myumoon.epiphany.registry;

import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.EpiphanyConstants;
import ink.myumoon.epiphany.content.AptitudeSourceConfig;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.content.PathData;
import ink.myumoon.epiphany.content.condition.Condition;
import ink.myumoon.epiphany.content.condition.logic.AlwaysCondition;
import ink.myumoon.epiphany.content.reward.EpiphanyReward;
import ink.myumoon.epiphany.content.reward.InsightReward;
import ink.myumoon.epiphany.content.reward.NoOpEpiphanyReward;
import ink.myumoon.epiphany.content.reward.NoOpInsightReward;
import ink.myumoon.epiphany.content.condition.logic.NeverCondition;
import ink.myumoon.epiphany.content.condition.logic.AndCondition;
import ink.myumoon.epiphany.content.condition.logic.OrCondition;
import ink.myumoon.epiphany.content.condition.logic.NotCondition;
import ink.myumoon.epiphany.content.condition.builtin.AdvancementCondition;
import ink.myumoon.epiphany.content.condition.builtin.AttributeCondition;
import ink.myumoon.epiphany.content.condition.builtin.ItemCondition;
import ink.myumoon.epiphany.content.condition.builtin.EffectCondition;
import ink.myumoon.epiphany.content.condition.builtin.DimensionCondition;
import ink.myumoon.epiphany.content.condition.builtin.StructureCondition;
import ink.myumoon.epiphany.content.condition.builtin.BiomeCondition;
import ink.myumoon.epiphany.content.condition.builtin.StatisticCondition;
import ink.myumoon.epiphany.content.condition.builtin.ExperienceLevelCondition;
import ink.myumoon.epiphany.content.condition.builtin.ItemUsedCondition;
import ink.myumoon.epiphany.content.condition.builtin.ItemCraftedCondition;
import ink.myumoon.epiphany.content.condition.builtin.ItemBrokenCondition;
import ink.myumoon.epiphany.content.condition.builtin.BlockBrokenCondition;
import ink.myumoon.epiphany.content.condition.builtin.KillEntityCondition;
import ink.myumoon.epiphany.content.condition.builtin.ModuleSelectedCondition;
import ink.myumoon.epiphany.content.condition.builtin.ModuleCompletedCondition;
import ink.myumoon.epiphany.content.condition.builtin.InsightSelectedCondition;
import ink.myumoon.epiphany.content.condition.builtin.EpiphanySelectedCondition;
import ink.myumoon.epiphany.content.condition.builtin.AptitudeCondition;
import ink.myumoon.epiphany.content.condition.builtin.ftbq.FTBQChapterCompletedCondition;
import ink.myumoon.epiphany.content.condition.builtin.ftbq.FTBQChapterStartedCondition;
import ink.myumoon.epiphany.content.condition.builtin.ftbq.FTBQQuestCondition;
import ink.myumoon.epiphany.content.condition.builtin.ftbq.FTBQTagCondition;
import ink.myumoon.epiphany.content.condition.builtin.kubejs.KubeJSStageCondition;
import ink.myumoon.epiphany.content.reward.KubeJSStageReward;
import ink.myumoon.epiphany.content.condition.builtin.InsightPointsCondition;
import ink.myumoon.epiphany.content.reward.AttributeReward;
import ink.myumoon.epiphany.content.reward.ItemReward;
import ink.myumoon.epiphany.content.reward.CommandReward;
import ink.myumoon.epiphany.content.reward.ExperienceReward;
import ink.myumoon.epiphany.content.reward.ExperienceLevelReward;
import ink.myumoon.epiphany.content.reward.EffectReward;
import ink.myumoon.epiphany.content.reward.ParticleReward;
import ink.myumoon.epiphany.content.reward.UnlockModuleReward;
import ink.myumoon.epiphany.content.reward.UnlockEpiphanyReward;
import ink.myumoon.epiphany.content.reward.LockModuleReward;
import ink.myumoon.epiphany.content.reward.LockEpiphanyReward;
import ink.myumoon.epiphany.content.reward.AptitudeReward;
import ink.myumoon.epiphany.content.reward.InsightPointsReward;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public final class EpiphanyRegistries {
    public static final ResourceKey<Registry<MapCodec<? extends Condition>>> CONDITION_SERIALIZERS_KEY =
            key("condition_type");
    public static final ResourceKey<Registry<MapCodec<? extends InsightReward>>> INSIGHT_REWARD_SERIALIZERS_KEY =
            key("insight_reward_type");
    public static final ResourceKey<Registry<MapCodec<? extends EpiphanyReward>>> EPIPHANY_REWARD_SERIALIZERS_KEY =
            key("epiphany_reward_type");

    private static final DeferredRegister<MapCodec<? extends Condition>> CONDITION_TYPES =
            DeferredRegister.create(CONDITION_SERIALIZERS_KEY, Epiphany.MODID);
    private static final DeferredRegister<MapCodec<? extends InsightReward>> INSIGHT_REWARD_TYPES =
            DeferredRegister.create(INSIGHT_REWARD_SERIALIZERS_KEY, Epiphany.MODID);
    private static final DeferredRegister<MapCodec<? extends EpiphanyReward>> EPIPHANY_REWARD_TYPES =
            DeferredRegister.create(EPIPHANY_REWARD_SERIALIZERS_KEY, Epiphany.MODID);

    public static final Supplier<IForgeRegistry<MapCodec<? extends Condition>>> CONDITION_SERIALIZERS =
            makeTypeRegistry(CONDITION_TYPES, EpiphanyConstants.ALWAYS_KEY);
    public static final Supplier<IForgeRegistry<MapCodec<? extends InsightReward>>> INSIGHT_REWARD_SERIALIZERS =
            makeTypeRegistry(INSIGHT_REWARD_TYPES, EpiphanyConstants.NO_OP_KEY);
    public static final Supplier<IForgeRegistry<MapCodec<? extends EpiphanyReward>>> EPIPHANY_REWARD_SERIALIZERS =
            makeTypeRegistry(EPIPHANY_REWARD_TYPES, EpiphanyConstants.NO_OP_KEY);

    public static final ResourceKey<Registry<ModuleData>> MODULE_REGISTRY_KEY = key("module");
    public static final ResourceKey<Registry<InsightData>> INSIGHT_REGISTRY_KEY = key("insight");
    public static final ResourceKey<Registry<EpiphanyData>> EPIPHANY_REGISTRY_KEY = key("epiphany");
    public static final ResourceKey<Registry<PathData>> PATH_REGISTRY_KEY = key("path");
    public static final ResourceKey<Registry<AptitudeSourceConfig>> APTITUDE_SOURCE_REGISTRY_KEY = key("aptitude");

    private EpiphanyRegistries() {
    }

    public static void register(IEventBus modBus) {
        CONDITION_TYPES.register(EpiphanyConstants.ALWAYS_KEY, () -> AlwaysCondition.CODEC);
        INSIGHT_REWARD_TYPES.register(EpiphanyConstants.NO_OP_KEY, () -> NoOpInsightReward.CODEC);
        EPIPHANY_REWARD_TYPES.register(EpiphanyConstants.NO_OP_KEY, () -> NoOpEpiphanyReward.CODEC);
        CONDITION_TYPES.register("never", () -> NeverCondition.CODEC);
        CONDITION_TYPES.register("and", () -> AndCondition.CODEC);
        CONDITION_TYPES.register("or", () -> OrCondition.CODEC);
        CONDITION_TYPES.register("not", () -> NotCondition.CODEC);
        CONDITION_TYPES.register("advancement", () -> AdvancementCondition.CODEC);
        CONDITION_TYPES.register("attribute", () -> AttributeCondition.CODEC);
        CONDITION_TYPES.register("item", () -> ItemCondition.CODEC);
        CONDITION_TYPES.register("effect", () -> EffectCondition.CODEC);
        CONDITION_TYPES.register("dimension", () -> DimensionCondition.CODEC);
        CONDITION_TYPES.register("structure", () -> StructureCondition.CODEC);
        CONDITION_TYPES.register("biome", () -> BiomeCondition.CODEC);
        CONDITION_TYPES.register("statistic", () -> StatisticCondition.CODEC);
        CONDITION_TYPES.register("experience_level", () -> ExperienceLevelCondition.CODEC);
        CONDITION_TYPES.register("item_used", () -> ItemUsedCondition.CODEC);
        CONDITION_TYPES.register("item_crafted", () -> ItemCraftedCondition.CODEC);
        CONDITION_TYPES.register("item_broken", () -> ItemBrokenCondition.CODEC);
        CONDITION_TYPES.register("block_broken", () -> BlockBrokenCondition.CODEC);
        CONDITION_TYPES.register("kill_entity", () -> KillEntityCondition.CODEC);
        CONDITION_TYPES.register("module_selected", () -> ModuleSelectedCondition.CODEC);
        CONDITION_TYPES.register("module_completed", () -> ModuleCompletedCondition.CODEC);
        CONDITION_TYPES.register("insight_selected", () -> InsightSelectedCondition.CODEC);
        CONDITION_TYPES.register("epiphany_selected", () -> EpiphanySelectedCondition.CODEC);
        CONDITION_TYPES.register("aptitude", () -> AptitudeCondition.CODEC);
        CONDITION_TYPES.register("insight_points", () -> InsightPointsCondition.CODEC);
        CONDITION_TYPES.register("ftbq_quest", () -> FTBQQuestCondition.CODEC);
        CONDITION_TYPES.register("ftbq_chapter_started", () -> FTBQChapterStartedCondition.CODEC);
        CONDITION_TYPES.register("ftbq_chapter_completed", () -> FTBQChapterCompletedCondition.CODEC);
        CONDITION_TYPES.register("ftbq_tag", () -> FTBQTagCondition.CODEC);
        CONDITION_TYPES.register("kubejs_stage", () -> KubeJSStageCondition.CODEC);
        CONDITION_TYPES.register(modBus);
        INSIGHT_REWARD_TYPES.register("attribute", () -> AttributeReward.CODEC);
        INSIGHT_REWARD_TYPES.register("item", () -> ItemReward.CODEC);
        INSIGHT_REWARD_TYPES.register("command", () -> CommandReward.CODEC);
        INSIGHT_REWARD_TYPES.register("experience", () -> ExperienceReward.CODEC);
        INSIGHT_REWARD_TYPES.register("experience_level", () -> ExperienceLevelReward.CODEC);
        INSIGHT_REWARD_TYPES.register("effect", () -> EffectReward.CODEC);
        INSIGHT_REWARD_TYPES.register("particle", () -> ParticleReward.CODEC);
        INSIGHT_REWARD_TYPES.register("unlock_module", () -> UnlockModuleReward.CODEC);
        INSIGHT_REWARD_TYPES.register("unlock_epiphany", () -> UnlockEpiphanyReward.CODEC);
        INSIGHT_REWARD_TYPES.register("lock_module", () -> LockModuleReward.CODEC);
        INSIGHT_REWARD_TYPES.register("lock_epiphany", () -> LockEpiphanyReward.CODEC);
        INSIGHT_REWARD_TYPES.register("aptitude", () -> AptitudeReward.CODEC);
        INSIGHT_REWARD_TYPES.register("insight_points", () -> InsightPointsReward.CODEC);
        INSIGHT_REWARD_TYPES.register("kubejs_stage", () -> KubeJSStageReward.CODEC);
        INSIGHT_REWARD_TYPES.register(modBus);
        EPIPHANY_REWARD_TYPES.register("attribute", () -> AttributeReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("item", () -> ItemReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("command", () -> CommandReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("experience", () -> ExperienceReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("experience_level", () -> ExperienceLevelReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("effect", () -> EffectReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("particle", () -> ParticleReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("unlock_module", () -> UnlockModuleReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("unlock_epiphany", () -> UnlockEpiphanyReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("lock_module", () -> LockModuleReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("lock_epiphany", () -> LockEpiphanyReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("aptitude", () -> AptitudeReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("insight_points", () -> InsightPointsReward.CODEC);
        EPIPHANY_REWARD_TYPES.register("kubejs_stage", () -> KubeJSStageReward.CODEC);
        EPIPHANY_REWARD_TYPES.register(modBus);
        modBus.addListener(EpiphanyRegistries::registerDatapackRegistries);
    }

    private static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(MODULE_REGISTRY_KEY, ModuleData.CODEC, ModuleData.CODEC);
        event.dataPackRegistry(INSIGHT_REGISTRY_KEY, InsightData.CODEC, InsightData.CODEC);
        event.dataPackRegistry(EPIPHANY_REGISTRY_KEY, EpiphanyData.CODEC, EpiphanyData.CODEC);
        event.dataPackRegistry(PATH_REGISTRY_KEY, PathData.CODEC, PathData.CODEC);
        event.dataPackRegistry(APTITUDE_SOURCE_REGISTRY_KEY, AptitudeSourceConfig.CODEC, AptitudeSourceConfig.CODEC);
    }

    private static <T> ResourceKey<Registry<T>> key(String path) {
        return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, path));
    }

    private static <T> Supplier<IForgeRegistry<T>> makeTypeRegistry(DeferredRegister<T> entries, String defaultKey) {
        return entries.makeRegistry(() -> new RegistryBuilder<T>()
                .setDefaultKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, defaultKey))
                .disableSaving()
                .disableSync());
    }
}
