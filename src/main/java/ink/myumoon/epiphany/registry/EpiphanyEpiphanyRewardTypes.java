package ink.myumoon.epiphany.registry;

import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.reward.AttributeReward;
import ink.myumoon.epiphany.content.reward.EpiphanyReward;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EpiphanyEpiphanyRewardTypes {

    public static final DeferredRegister<MapCodec<? extends EpiphanyReward>> REGISTRY =
            DeferredRegister.create(EpiphanyRegistries.EPIPHANY_REWARD_SERIALIZERS, Epiphany.MODID);

    public static final DeferredHolder<MapCodec<? extends EpiphanyReward>, MapCodec<AttributeReward>> ATTRIBUTE =
            REGISTRY.register("attribute", () -> AttributeReward.CODEC);

    private EpiphanyEpiphanyRewardTypes() {
    }
}
