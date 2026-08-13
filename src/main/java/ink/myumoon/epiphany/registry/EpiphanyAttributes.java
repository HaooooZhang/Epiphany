package ink.myumoon.epiphany.registry;

import ink.myumoon.epiphany.Epiphany;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EpiphanyAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, Epiphany.MODID);

    public static final DeferredHolder<Attribute, Attribute> APTITUDE_GAIN_MULTIPLIER = ATTRIBUTES.register("aptitude_gain_multiplier",
            () -> new RangedAttribute(
                    "attribute.epiphany.aptitude_gain_multiplier",
                    1.0,
                    0.0,
                    100.0
            ).setSyncable(true)
    );
}
