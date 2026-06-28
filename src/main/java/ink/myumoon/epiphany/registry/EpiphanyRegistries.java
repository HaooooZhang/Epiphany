package ink.myumoon.epiphany.registry;

import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.EpiphanyConstants;
import ink.myumoon.epiphany.content.*;
import ink.myumoon.epiphany.content.condition.AlwaysCondition;
import ink.myumoon.epiphany.content.condition.Condition;
import ink.myumoon.epiphany.content.reward.EpiphanyReward;
import ink.myumoon.epiphany.content.reward.InsightReward;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;

/**
 * Central registry definitions for Epiphany.
 * <p>
 * Three builtin registries (storing MapCodecs) enable polymorphic dispatch
 * for Conditions, InsightRewards, and EpiphanyRewards.
 * <p>
 * Four datapack registries (JSON-driven) define Modules, Insights, Epiphanies, and Paths.
 */
@EventBusSubscriber(modid = Epiphany.MODID)
public final class EpiphanyRegistries {

    // ============================================================
    // Builtin Registries — store MapCodec<? extends T> for dispatch
    // ============================================================

    public static final ResourceKey<Registry<MapCodec<? extends Condition>>> CONDITION_SERIALIZERS_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "condition_type"));

    public static final DefaultedRegistry<MapCodec<? extends Condition>> CONDITION_SERIALIZERS =
            new DefaultedMappedRegistry<>(
                    EpiphanyConstants.ALWAYS_KEY,
                    CONDITION_SERIALIZERS_KEY,
                    Lifecycle.stable(),
                    false
            );

    public static final ResourceKey<Registry<MapCodec<? extends InsightReward>>> INSIGHT_REWARD_SERIALIZERS_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "insight_reward_type"));

    public static final DefaultedRegistry<MapCodec<? extends InsightReward>> INSIGHT_REWARD_SERIALIZERS =
            new DefaultedMappedRegistry<>(
                    EpiphanyConstants.NO_OP_KEY,
                    INSIGHT_REWARD_SERIALIZERS_KEY,
                    Lifecycle.stable(),
                    false
            );

    public static final ResourceKey<Registry<MapCodec<? extends EpiphanyReward>>> EPIPHANY_REWARD_SERIALIZERS_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "epiphany_reward_type"));

    public static final DefaultedRegistry<MapCodec<? extends EpiphanyReward>> EPIPHANY_REWARD_SERIALIZERS =
            new DefaultedMappedRegistry<>(
                    EpiphanyConstants.NO_OP_KEY,
                    EPIPHANY_REWARD_SERIALIZERS_KEY,
                    Lifecycle.stable(),
                    false
            );

    // ============================================================
    // Datapack Registry Keys — JSON-driven, loaded from data/epiphany/
    // ============================================================

    public static final ResourceKey<Registry<ModuleData>> MODULE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "module"));

    public static final ResourceKey<Registry<InsightData>> INSIGHT_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "insight"));

    public static final ResourceKey<Registry<EpiphanyData>> EPIPHANY_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "epiphany"));

    public static final ResourceKey<Registry<PathData>> PATH_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "path"));

    // ============================================================
    // Events
    // ============================================================

    @SubscribeEvent
    static void registerBuiltinRegistries(NewRegistryEvent event) {
        event.register(CONDITION_SERIALIZERS);
        event.register(INSIGHT_REWARD_SERIALIZERS);
        event.register(EPIPHANY_REWARD_SERIALIZERS);
    }

    @SubscribeEvent
    static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(MODULE_REGISTRY_KEY, ModuleData.CODEC, ModuleData.CODEC);
        event.dataPackRegistry(INSIGHT_REGISTRY_KEY, InsightData.CODEC, InsightData.CODEC);
        event.dataPackRegistry(EPIPHANY_REGISTRY_KEY, EpiphanyData.CODEC, EpiphanyData.CODEC);
        event.dataPackRegistry(PATH_REGISTRY_KEY, PathData.CODEC, PathData.CODEC);
    }

    private EpiphanyRegistries() {
    }
}
