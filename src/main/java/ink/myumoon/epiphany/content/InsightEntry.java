package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * A reference to an Insight within a Module's definition.
 * <p>
 * Each entry specifies the Insight's datapack ID and its depth in the tree.
 * All Insights at the same depth form an AND-group:
 * all depth-0 Insights must be unlocked before depth-1 Insights become available.
 */
public record InsightEntry(ResourceLocation id, int depth) {
    public static final Codec<InsightEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(InsightEntry::id),
            Codec.INT.optionalFieldOf("depth", 0).forGetter(InsightEntry::depth)
    ).apply(instance, InsightEntry::new));
}
