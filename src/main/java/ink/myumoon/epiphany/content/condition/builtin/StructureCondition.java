package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether the player is inside a specific structure.
 * <p>
 * JSON: {@code {"type": "epiphany:structure", "structure": "minecraft:fortress"}}
 */
public record StructureCondition(ResourceLocation structure) implements Condition {

    public static final MapCodec<StructureCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("structure").forGetter(StructureCondition::structure)
    ).apply(instance, StructureCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        var level = player.serverLevel();
        var pos = player.blockPosition();
        return level.structureManager().getStructureWithPieceAt(
                pos, level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(structure)
        ).isValid();
    }
}
