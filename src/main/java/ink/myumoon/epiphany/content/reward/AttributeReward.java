package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Adds a permanent attribute modifier to the player.
 * <p>
 * Implements both {@link InsightReward} and {@link EpiphanyReward}
 * so it can be used as the reward for both Insight nodes and Epiphany abilities.
 * <p>
 * JSON: {@code {"type": "epiphany:attribute", "attribute": "minecraft:generic.max_health",
 * "amount": 2.0, "operation": "addition"}}
 */
public record AttributeReward(
        Holder<Attribute> attribute,
        double amount,
        AttributeModifier.Operation operation
) implements InsightReward, EpiphanyReward {

    private static final Codec<AttributeModifier.Operation> OPERATION_CODEC =
            Codec.STRING.xmap(
                    AttributeModifier.Operation::valueOf,
                    AttributeModifier.Operation::name
            );

    public static final MapCodec<AttributeReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ATTRIBUTE.holderByNameCodec()
                    .fieldOf("attribute").forGetter(AttributeReward::attribute),
            Codec.DOUBLE.optionalFieldOf("amount", 0.0).forGetter(AttributeReward::amount),
            OPERATION_CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                    .forGetter(AttributeReward::operation)
    ).apply(instance, AttributeReward::new));

    @Override
    public MapCodec<AttributeReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player) {
        var attr = player.getAttribute(attribute);
        if (attr == null) return;
        attr.addPermanentModifier(
                new AttributeModifier(modifierId(), amount, operation));
    }

    @Override
    public void remove(ServerPlayer player) {
        var attr = player.getAttribute(attribute);
        if (attr != null) {
            attr.removeModifier(modifierId());
        }
    }

    /** Deterministic UUID from (attribute, amount, operation) so apply/remove match. */
    private ResourceLocation modifierId() {
        String idStr = attribute.value().getDescriptionId() + amount + operation.name();
        UUID uuid = UUID.nameUUIDFromBytes(idStr.getBytes(StandardCharsets.UTF_8));
        return ResourceLocation.fromNamespaceAndPath("epiphany", "reward_" + uuid);
    }
}
