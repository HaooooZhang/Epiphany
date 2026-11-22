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
        CONDITION_TYPES.register(modBus);
        INSIGHT_REWARD_TYPES.register(modBus);
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
