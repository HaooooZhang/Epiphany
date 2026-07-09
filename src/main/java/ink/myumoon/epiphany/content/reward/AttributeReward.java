package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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

    /** Vanilla 1.20.5+ snake_case aliases — preferred form for datapack authors. */
    private static final java.util.Map<String, AttributeModifier.Operation> ALIASES = java.util.Map.of(
            "add_value", AttributeModifier.Operation.ADD_VALUE,
            "add_multiplied_base", AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            "add_multiplied_total", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private static String snakeCaseName(AttributeModifier.Operation op) {
        return switch (op) {
            case ADD_VALUE -> "add_value";
            case ADD_MULTIPLIED_BASE -> "add_multiplied_base";
            case ADD_MULTIPLIED_TOTAL -> "add_multiplied_total";
        };
    }

    /**
     * Codec for {@link AttributeModifier.Operation} that accepts both the vanilla
     * 1.20.5+ snake_case names used in attribute_modifier JSON
     * ({@code add_value} / {@code add_multiplied_base} / {@code add_multiplied_total})
     * and the legacy CamelCase enum constant names ({@code ADD_VALUE} / etc).
     * <p>
     * Datapack authors should prefer snake_case to match vanilla attribute_modifier
     * syntax. CamelCase is kept for backwards compatibility with pre-existing
     * datapacks.
     */
    private static final Codec<AttributeModifier.Operation> OPERATION_CODEC =
            Codec.STRING.flatXmap(
                    s -> {
                        AttributeModifier.Operation direct = ALIASES.get(s);
                        if (direct != null) return DataResult.success(direct);
                        try {
                            return DataResult.success(AttributeModifier.Operation.valueOf(s));
                        } catch (IllegalArgumentException e) {
                            return DataResult.error(() ->
                                    "Unknown attribute_modifier operation: '" + s
                                            + "'. Expected one of add_value / add_multiplied_base / add_multiplied_total");
                        }
                    },
                    op -> DataResult.success(snakeCaseName(op))
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
