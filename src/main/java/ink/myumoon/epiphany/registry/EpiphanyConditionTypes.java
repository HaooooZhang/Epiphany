package ink.myumoon.epiphany.registry;

import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.EpiphanyConstants;
import ink.myumoon.epiphany.content.condition.*;
import ink.myumoon.epiphany.content.condition.builtin.*;
import ink.myumoon.epiphany.content.condition.logic.*;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EpiphanyConditionTypes {

    public static final DeferredRegister<MapCodec<? extends Condition>> REGISTRY =
            DeferredRegister.create(EpiphanyRegistries.CONDITION_SERIALIZERS, Epiphany.MODID);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<AlwaysCondition>> ALWAYS =
            REGISTRY.register(EpiphanyConstants.ALWAYS_KEY, () -> AlwaysCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<NeverCondition>> NEVER =
            REGISTRY.register("never", () -> NeverCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<AndCondition>> AND =
            REGISTRY.register("and", () -> AndCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<OrCondition>> OR =
            REGISTRY.register("or", () -> OrCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<NotCondition>> NOT =
            REGISTRY.register("not", () -> NotCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<AdvancementCondition>> ADVANCEMENT =
            REGISTRY.register("advancement", () -> AdvancementCondition.CODEC);

    // --- Phase 7.1: vanilla + Epiphany conditions ---
    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<AttributeCondition>> ATTRIBUTE =
            REGISTRY.register("attribute", () -> AttributeCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<ItemCondition>> ITEM =
            REGISTRY.register("item", () -> ItemCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<EffectCondition>> EFFECT =
            REGISTRY.register("effect", () -> EffectCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<DimensionCondition>> DIMENSION =
            REGISTRY.register("dimension", () -> DimensionCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<StructureCondition>> STRUCTURE =
            REGISTRY.register("structure", () -> StructureCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<BiomeCondition>> BIOME =
            REGISTRY.register("biome", () -> BiomeCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<StatisticCondition>> STATISTIC =
            REGISTRY.register("statistic", () -> StatisticCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<ExperienceLevelCondition>> EXPERIENCE_LEVEL =
            REGISTRY.register("experience_level", () -> ExperienceLevelCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<ItemUsedCondition>> ITEM_USED =
            REGISTRY.register("item_used", () -> ItemUsedCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<BlockBrokenCondition>> BLOCK_BROKEN =
            REGISTRY.register("block_broken", () -> BlockBrokenCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<KillEntityCondition>> KILL_ENTITY =
            REGISTRY.register("kill_entity", () -> KillEntityCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<ModuleSelectedCondition>> MODULE_SELECTED =
            REGISTRY.register("module_selected", () -> ModuleSelectedCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<ModuleCompletedCondition>> MODULE_COMPLETED =
            REGISTRY.register("module_completed", () -> ModuleCompletedCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<InsightSelectedCondition>> INSIGHT_SELECTED =
            REGISTRY.register("insight_selected", () -> InsightSelectedCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<EpiphanySelectedCondition>> EPIPHANY_SELECTED =
            REGISTRY.register("epiphany_selected", () -> EpiphanySelectedCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<AptitudeCondition>> APTITUDE =
            REGISTRY.register("aptitude", () -> AptitudeCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends Condition>, MapCodec<InsightPointsCondition>> INSIGHT_POINTS =
            REGISTRY.register("insight_points", () -> InsightPointsCondition.CODEC);

    private EpiphanyConditionTypes() {
    }
}
