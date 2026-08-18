package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.Epiphany;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Adds a permanent attribute modifier to the player.
 * <p>
 * Implements both {@link InsightReward}, {@link EpiphanyReward}, and
 * {@link PersistentReward} so it can be used as the reward for both
 * Insight nodes and Epiphany abilities, and survives player death.
 * <p>
 * JSON: {@code {"type": "epiphany:attribute", "attribute": "minecraft:generic.max_health",
 * "amount": 2.0, "operation": "add_value"}}
 */
public record AttributeReward(
        Holder<Attribute> attribute,
        double amount,
        AttributeModifier.Operation operation
) implements InsightReward, EpiphanyReward, PersistentReward {

    private static final String KEY = "epiphany_attribute_modifiers";

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

    private static final Codec<Double> AMOUNT_CODEC = Codec.DOUBLE.validate(value ->
            Double.isFinite(value)
                    ? DataResult.success(value)
                    : DataResult.error(() -> "Attribute reward amount must be finite"));

    public static final MapCodec<AttributeReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ATTRIBUTE.holderByNameCodec()
                    .fieldOf("attribute").forGetter(AttributeReward::attribute),
            AMOUNT_CODEC.optionalFieldOf("amount", 0.0).forGetter(AttributeReward::amount),
            OPERATION_CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                    .forGetter(AttributeReward::operation)
    ).apply(instance, AttributeReward::new));

    @Override
    public MapCodec<AttributeReward> codec() {
        return CODEC;
    }

    @Override
    @Deprecated(forRemoval = false)
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        apply(player, new RewardSource("legacy_api", sourceId, "reward", sourceId));
    }

    @Override
    public void apply(ServerPlayer player, RewardSource source) {
        var target = player.getAttribute(attribute);
        if (target == null) {
            Epiphany.LOGGER.warn("Cannot apply attribute reward {} to {}: player does not have attribute {}",
                    source.legacyId(), player.getGameProfile().getName(), attributeId(attribute));
            return;
        }

        CompoundTag data = player.getPersistentData();
        List<Record> records = readRecords(data);
        ResourceLocation id = modifierId(source);
        Record replacement = new Record(source, attributeId(attribute), id, amount, operation);
        boolean current = false;
        var updated = new ArrayList<Record>(records.size() + 1);

        for (Record record : records) {
            if (!record.matches(source)) {
                updated.add(record);
                continue;
            }
            if (record.equals(replacement)) {
                current = true;
                updated.add(record);
            } else {
                removeModifier(player, record.attributeId(), record.modifierId());
            }
        }

        removeLegacyModifiers(player, source);
        AttributeModifier existing = target.getModifier(id);
        if (existing != null && (Double.compare(existing.amount(), amount) != 0
                || existing.operation() != operation)) {
            target.removeModifier(id);
            existing = null;
        }
        if (existing == null) {
            target.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
        if (!current) updated.add(replacement);
        writeRecords(data, updated);
    }

    @Override
    @Deprecated(forRemoval = false)
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        remove(player, new RewardSource("legacy_api", sourceId, "reward", sourceId));
    }

    @Override
    public void remove(ServerPlayer player, RewardSource source) {
        CompoundTag data = player.getPersistentData();
        List<Record> records = readRecords(data);
        var remaining = new ArrayList<Record>(records.size());
        for (Record record : records) {
            if (record.matches(source)) {
                removeModifier(player, record.attributeId(), record.modifierId());
            } else {
                remaining.add(record);
            }
        }
        writeRecords(data, remaining);

        var target = player.getAttribute(attribute);
        if (target != null) target.removeModifier(modifierId(source));
        removeLegacyModifiers(player, source);
        clampHealth(player);
    }

    static void clearTrackedSources(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        for (Record record : readRecords(data)) {
            removeModifier(player, record.attributeId(), record.modifierId());
        }
        data.remove(KEY);
    }

    static void finishSourceRebuild(ServerPlayer player) {
        clampHealth(player);
    }

    private static ResourceLocation modifierId(RewardSource source) {
        String path = "attribute/" + source.ownerKind() + "/"
                + source.ownerId().getNamespace() + "/" + source.ownerId().getPath() + "/"
                + source.rewardSlot() + "/" + entryIndex(source);
        return ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, path);
    }

    private static int entryIndex(RewardSource source) {
        if (!source.ownerId().getNamespace().equals(source.legacyId().getNamespace())) return 0;
        String prefix = source.ownerId().getPath() + "/" + source.rewardSlot() + "_";
        String path = source.legacyId().getPath();
        if (!path.startsWith(prefix)) return 0;
        try {
            return Integer.parseInt(path.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static ResourceLocation legacyModifierId(ServerPlayer player, ResourceLocation sourceId) {
        String path = "modifier/" + player.getStringUUID() + "/"
                + sourceId.getNamespace() + "/" + sourceId.getPath();
        return ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, path);
    }

    private void removeLegacyModifiers(ServerPlayer player, RewardSource source) {
        var target = player.getAttribute(attribute);
        if (target == null) return;
        target.removeModifier(legacyModifierId(player, source.legacyId()));
        target.removeModifier(legacyModifierId(player, source.ownerId()));
    }

    private static void removeModifier(ServerPlayer player, String attributeId, ResourceLocation modifierId) {
        ResourceLocation id = ResourceLocation.tryParse(attributeId);
        if (id == null) return;
        Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
        if (holder == null) return;
        var instance = player.getAttribute(holder);
        if (instance != null) instance.removeModifier(modifierId);
    }

    private static String attributeId(Holder<Attribute> attribute) {
        return attribute.unwrapKey().orElseThrow().location().toString();
    }

    private static void clampHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static List<Record> readRecords(CompoundTag data) {
        Tag tag = data.get(KEY);
        if (!(tag instanceof ListTag list)) return List.of();
        var records = new ArrayList<Record>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Record record = Record.read(list.get(i));
            if (record != null) records.add(record);
        }
        return records;
    }

    private static void writeRecords(CompoundTag data, List<Record> records) {
        if (records.isEmpty()) {
            data.remove(KEY);
            return;
        }
        ListTag list = new ListTag();
        records.stream().sorted(Comparator.comparing(Record::sortKey))
                .map(Record::write).forEach(list::add);
        data.put(KEY, list);
    }

    private record Record(RewardSource source, String attributeId, ResourceLocation modifierId,
                          double amount, AttributeModifier.Operation operation) {
        private static Record read(Tag tag) {
            if (!(tag instanceof CompoundTag compound)) return null;
            ResourceLocation ownerId = ResourceLocation.tryParse(compound.getString("owner_id"));
            ResourceLocation legacyId = ResourceLocation.tryParse(compound.getString("legacy_id"));
            ResourceLocation modifierId = ResourceLocation.tryParse(compound.getString("modifier_id"));
            String ownerKind = compound.getString("owner_kind");
            String rewardSlot = compound.getString("reward_slot");
            String attributeId = compound.getString("attribute");
            AttributeModifier.Operation operation = ALIASES.get(compound.getString("operation"));
            if (ownerId == null || legacyId == null || modifierId == null || ownerKind.isEmpty()
                    || rewardSlot.isEmpty() || ResourceLocation.tryParse(attributeId) == null
                    || operation == null) {
                return null;
            }
            double amount = compound.getDouble("amount");
            if (!Double.isFinite(amount)) return null;
            return new Record(new RewardSource(ownerKind, ownerId, rewardSlot, legacyId),
                    attributeId, modifierId, amount, operation);
        }

        private boolean matches(RewardSource other) {
            return source.ownerKind().equals(other.ownerKind())
                    && source.ownerId().equals(other.ownerId())
                    && source.rewardSlot().equals(other.rewardSlot())
                    && entryIndex(source) == entryIndex(other);
        }

        private String sortKey() {
            return source.ownerKind() + "|" + source.ownerId() + "|" + source.rewardSlot()
                    + "|" + entryIndex(source);
        }

        private Tag write() {
            CompoundTag compound = new CompoundTag();
            compound.putString("owner_kind", source.ownerKind());
            compound.putString("owner_id", source.ownerId().toString());
            compound.putString("reward_slot", source.rewardSlot());
            compound.putString("legacy_id", source.legacyId().toString());
            compound.putString("attribute", attributeId);
            compound.putString("modifier_id", modifierId.toString());
            compound.putDouble("amount", amount);
            compound.putString("operation", snakeCaseName(operation));
            return compound;
        }
    }
}
