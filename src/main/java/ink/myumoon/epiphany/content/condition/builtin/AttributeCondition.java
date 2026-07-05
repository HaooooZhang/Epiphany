package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Comparison;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Checks the player's attribute value against a threshold.
 * <p>
 * JSON: {@code {"type": "epiphany:attribute", "attribute": "minecraft:generic.max_health",
 * "comparison": ">=", "value": 30.0}}
 */
public record AttributeCondition(
        Holder<Attribute> attribute,
        Comparison comparison,
        double value
) implements Condition {

    public static final MapCodec<AttributeCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ATTRIBUTE.holderByNameCodec()
                    .fieldOf("attribute").forGetter(AttributeCondition::attribute),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_OR_EQUAL)
                    .forGetter(AttributeCondition::comparison),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("value").forGetter(AttributeCondition::value)
    ).apply(instance, AttributeCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        var attr = player.getAttribute(attribute);
        if (attr == null) return false;
        return comparison.test(attr.getValue(), value);
    }
}
