package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Checks whether the player is in a specific biome.
 * <p>
 * Tag support: prefix the id with {@code #} to check against any biome in a tag.
 * <ul>
 *   <li>{@code {"type":"epiphany:biome", "biome":"minecraft:desert"}}            — single biome id</li>
 *   <li>{@code {"type":"epiphany:biome", "biome":"#minecraft:is_desert"}}        — any element of a tag</li>
 *   <li>{@code {"type":"epiphany:biome", "biome":"#minecraft:is_ocean"}}         — same, useful for grouping</li>
 * </ul>
 */
public record BiomeCondition(Either<ResourceKey<Biome>, TagKey<Biome>> biome) implements Condition {

    public static final MapCodec<BiomeCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("biome").forGetter(BiomeCondition::rawString)
    ).apply(instance, BiomeCondition::parseRef));

    /** Parses {@code "name:path"} → Left(biome key), {@code "#name:path"} → Right(tag). */
    public static BiomeCondition parseRef(String ref) {
        if (ref.startsWith("#")) {
            ResourceLocation loc = ResourceLocation.parse(ref.substring(1));
            return new BiomeCondition(Either.right(TagKey.create(Registries.BIOME, loc)));
        }
        ResourceLocation loc = ResourceLocation.parse(ref);
        return new BiomeCondition(Either.left(ResourceKey.create(Registries.BIOME, loc)));
    }

    /** Serializes back to a single string ({@code "minecraft:desert"} or {@code "#minecraft:is_ocean"}). */
    public String rawString() {
        return biome.map(
                key -> key.location().toString(),
                tag -> "#" + tag.location()
        );
    }

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        Holder<Biome> current = player.level().getBiome(player.blockPosition());
        return biome.map(
                // Single biome key — direct holder match
                key -> current.is(key),
                // Tag — true if the current biome holder is in the tag
                tag -> current.is(tag)
        );
    }
}

