package ink.myumoon.epiphany.registry;

import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.EpiphanyConstants;
import ink.myumoon.epiphany.content.condition.*;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Epiphany's built-in Condition types into the
 * {@link EpiphanyRegistries#CONDITION_SERIALIZERS} registry.
 */
@SuppressWarnings("unused")
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

    private EpiphanyConditionTypes() {
    }
}
