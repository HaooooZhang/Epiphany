package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Data-pack defined path: an optional grouping label for Epiphanies.
 * <p>
 * Paths are purely for UI categorization. An Epiphany references a Path
 * via its {@code "path"} field one-way; Paths do not hold lists of Epiphanies.
 * <p>
 * The path's registry id is derived from the JSON file path.
 */
public record PathData(
        Optional<Component> name,
        Optional<Component> description,
        Optional<ResourceLocation> icon,
        int weight
) {
    public static final Codec<PathData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(PathData::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(PathData::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(PathData::icon),
            Codec.INT.optionalFieldOf("weight", 100).forGetter(PathData::weight)
    ).apply(instance, PathData::new));
}
