package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.reward.InsightReward;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Data-pack defined insight: an incremental upgrade node inside a Module.
 * <p>
 * Insights are arranged by {@code depth} (defined via {@link InsightEntry} in the parent Module).
 * Same-depth Insights form an AND-group. Rewards are quantitative.
 * <p>
 * The insight's registry id is derived from the JSON file path.
 */
public record InsightData(
        Optional<Component> name,
        Optional<Component> description,
        Optional<ResourceLocation> icon,
        int cost,
        Optional<InsightReward> reward,
        int weight
) {
    public static final Codec<InsightData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(InsightData::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(InsightData::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(InsightData::icon),
            Codec.INT.optionalFieldOf("cost", 1).forGetter(InsightData::cost),
            InsightReward.CODEC.optionalFieldOf("reward").forGetter(InsightData::reward),
            Codec.INT.optionalFieldOf("weight", 100).forGetter(InsightData::weight)
    ).apply(instance, InsightData::new));
}
