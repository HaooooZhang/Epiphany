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

/**
 * Adds a permanent attribute modifier to the player.
 * <p>
 * Implements both {@link InsightReward}, {@link EpiphanyReward}, and
 * {@link PersistentReward} so it can be used as the reward for both
 * Insight nodes and Epiphany abilities, and survives player death.
 * <p>
 * JSON: {@code {"type": "epiphany:attribute", "attribute": "minecraft:generic.max_health",
 * "amount": 2.0, "operation": "addition"}}
 */
public record AttributeReward(
        Holder<Attribute> attribute,
        double amount,
        AttributeModifier.Operation operation
) implements InsightReward, EpiphanyReward, PersistentReward {

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
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        var attr = player.getAttribute(attribute);
        if (attr == null) return;
        var id = modifierId(player, sourceId);
        if (attr.getModifier(id) != null) return;
        attr.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        var attr = player.getAttribute(attribute);
        if (attr != null) {
            attr.removeModifier(modifierId(player, sourceId));
        }
    }

    /**
     * Deterministic ResourceLocation from (player, sourceId).
     * Each (player, insight/module/epiphany) pair gets a unique modifier,
     * so two different Insights with the same attribute parameters stack correctly.
     */
    private ResourceLocation modifierId(ServerPlayer player, ResourceLocation sourceId) {
        String path = "modifier/" + player.getStringUUID() + "/"
                + sourceId.getNamespace() + "/" + sourceId.getPath();
        return ResourceLocation.fromNamespaceAndPath("epiphany", path);
    }
}
