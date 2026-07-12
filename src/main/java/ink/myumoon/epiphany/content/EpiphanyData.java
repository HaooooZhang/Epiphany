package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import ink.myumoon.epiphany.content.reward.EpiphanyReward;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Data-pack defined epiphany: a qualitative ability chosen after completing a Module.
 * <p>
 * Epiphanies exist in a global pool independent of any specific Module.
 * They are grouped in the UI by their optional {@code path} reference.
 * <p>
 * The epiphany's registry id is derived from the JSON file path.
 */
public record EpiphanyData(
        Optional<Component> name,
        Optional<Component> description,
        Optional<ResourceLocation> icon,
        Optional<ResourceLocation> path,
        Optional<Condition> condition,
        Optional<Component> conditionDescription,
        InitialState initialState,
        Optional<EpiphanyReward> reward,
        Optional<Component> rewardDescription,
        int weight
) {
    public static final Codec<EpiphanyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(EpiphanyData::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(EpiphanyData::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(EpiphanyData::icon),
            ResourceLocation.CODEC.optionalFieldOf("path").forGetter(EpiphanyData::path),
            Condition.CODEC.optionalFieldOf("condition").forGetter(EpiphanyData::condition),
            ComponentSerialization.CODEC.optionalFieldOf("condition_description")
                    .forGetter(EpiphanyData::conditionDescription),
            InitialState.CODEC.optionalFieldOf("initial_state", InitialState.SELECTABLE)
                    .forGetter(EpiphanyData::initialState),
            EpiphanyReward.CODEC.optionalFieldOf("reward").forGetter(EpiphanyData::reward),
            ComponentSerialization.CODEC.optionalFieldOf("reward_description").forGetter(EpiphanyData::rewardDescription),
            Codec.INT.optionalFieldOf("weight", 100).forGetter(EpiphanyData::weight)
    ).apply(instance, EpiphanyData::new));

    /**
     * Returns the name, falling back to a translatable key {@code epiphany.<ns>.<path>.name}.
     */
    public Component effectiveName(ResourceLocation id) {
        return name.orElseGet(() ->
                Component.translatable("epiphany." + id.getNamespace() + "." + id.getPath() + ".name"));
    }

    /**
     * Returns the description, falling back to a translatable key {@code epiphany.<ns>.<path>.description}.
     */
    public Component effectiveDescription(ResourceLocation id) {
        return description.orElseGet(() ->
                Component.translatable("epiphany." + id.getNamespace() + "." + id.getPath() + ".description"));
    }

    /**
     * Returns the condition description, falling back to a translatable key {@code epiphany.<ns>.<path>.condition_description}.
     */
    public Component effectiveConditionDescription(ResourceLocation id) {
        return conditionDescription.orElseGet(() ->
                Component.translatable("epiphany." + id.getNamespace() + "." + id.getPath() + ".condition_description"));
    }

    /**
     * Returns the reward description, falling back to a translatable key {@code epiphany.<ns>.<path>.reward_description}.
     */
    public Component effectiveRewardDescription(ResourceLocation id) {
        return rewardDescription.orElseGet(() ->
                Component.translatable("epiphany." + id.getNamespace() + "." + id.getPath() + ".reward_description"));
    }
}
