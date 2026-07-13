package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.reward.InsightReward;
import net.minecraft.locale.Language;
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
        Optional<Component> rewardDescription,
        int weight
) {
    public static final Codec<InsightData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(InsightData::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(InsightData::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(InsightData::icon),
            Codec.INT.optionalFieldOf("cost", 1).forGetter(InsightData::cost),
            InsightReward.CODEC.optionalFieldOf("reward").forGetter(InsightData::reward),
            ComponentSerialization.CODEC.optionalFieldOf("reward_description").forGetter(InsightData::rewardDescription),
            Codec.INT.optionalFieldOf("weight", 100).forGetter(InsightData::weight)
    ).apply(instance, InsightData::new));

    /**
     * Returns the name, falling back to a translatable key {@code insight.<ns>.<path>.name}.
     */
    public Component effectiveName(ResourceLocation id) {
        return name.orElseGet(() ->
                Component.translatable("insight." + id.getNamespace() + "." + id.getPath() + ".name"));
    }

    /**
     * Returns the description, or a translatable fallback if the key exists in the language file.
     */
    public Optional<Component> effectiveDescription(ResourceLocation id) {
        if (description.isPresent()) return description;
        String key = "insight." + id.getNamespace() + "." + id.getPath() + ".description";
        if (Language.getInstance().has(key)) return Optional.of(Component.translatable(key));
        return Optional.empty();
    }

    /**
     * Returns the reward description, or a translatable fallback if the key exists in the language file.
     */
    public Optional<Component> effectiveRewardDescription(ResourceLocation id) {
        if (rewardDescription.isPresent()) return rewardDescription;
        String key = "insight." + id.getNamespace() + "." + id.getPath() + ".reward_description";
        if (Language.getInstance().has(key)) return Optional.of(Component.translatable(key));
        return Optional.empty();
    }
}
