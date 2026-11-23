package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Checks whether the player is inside a specific structure.
 * <p>
 * Tag support: prefix the id with {@code #} to check against any structure in a tag.
 * <ul>
 *   <li>{@code {"type":"epiphany:structure", "structure":"minecraft:fortress"}}  — single structure id</li>
 *   <li>{@code {"type":"epiphany:structure", "structure":"#minecraft:village"}}  — any element of a tag</li>
 * </ul>
 * <p>
 * The codec only parses the reference string (id + optional {@code #} prefix); the
 * actual {@link Structure} resolution happens in {@link #test(ServerPlayer)} using the
 * live server registry (since on 1.21.1 STRUCTURE is not in
 * {@link net.minecraft.core.registries.BuiltInRegistries}).
 */
public record StructureCondition(Either<ResourceLocation, TagKey<Structure>> reference) implements Condition {

    public static final MapCodec<StructureCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("structure").forGetter(StructureCondition::rawString)
    ).apply(instance, StructureCondition::parseRef));

    /** Parses {@code "name:path"} → Left(id), {@code "#name:path"} → Right(tag). */
    public static StructureCondition parseRef(String ref) {
        if (ref.startsWith("#")) {
            ResourceLocation loc = ResourceLocation.parse(ref.substring(1));
            return new StructureCondition(Either.right(TagKey.create(Registries.STRUCTURE, loc)));
        }
        return new StructureCondition(Either.left(ResourceLocation.parse(ref)));
    }

    /** Serializes back to a single string ({@code "minecraft:fortress"} or {@code "#minecraft:village"}). */
    public String rawString() {
        return reference.map(
                id -> id.toString(),
                tag -> "#" + tag.location()
        );
    }

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        var level = player.serverLevel();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

        return reference.map(
                // Single structure id — direct check at the player's position.
                id -> {
                    if (!registry.containsKey(id)) {
                        Epiphany.LOGGER.warn("Structure \"{}\" does not exist, referenced in epiphany:structure condition",
                                id);
                        return false;
                    }
                    return level.structureManager()
                            .getStructureWithPieceAt(player.blockPosition(), registry.get(id))
                            .isValid();
                },
                // Tag — any structure in the tag whose bounding box covers the player.
                tag -> {
                    for (var holder : registry.getTagOrEmpty(tag)) {
                        if (level.structureManager()
                                .getStructureWithPieceAt(player.blockPosition(), holder.value())
                                .isValid()) {
                            return true;
                        }
                    }
                    return false;
                }
        );
    }
}


