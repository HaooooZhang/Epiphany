package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import ink.myumoon.epiphany.content.reward.InsightReward;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Data-pack defined module: an independent skill-tree unit containing Insights.
 * <p>
 * The module's registry id is derived from the JSON file path
 * (e.g. {@code data/epiphany/module/combat.json} → {@code epiphany:combat}).
 */
public record ModuleData(
        Optional<Component> name,
        Optional<Component> description,
        Optional<ResourceLocation> icon,
        Optional<Condition> condition,
        Optional<Component> conditionDescription,
        InitialState initialState,
        List<InsightEntry> insights,
        Optional<InsightReward> onSelectReward,
        Optional<InsightReward> onCompleteReward,
        Optional<Component> onSelectRewardDescription,
        Optional<Component> onCompleteRewardDescription,
        int weight
) {
    public static final Codec<ModuleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(ModuleData::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(ModuleData::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(ModuleData::icon),
            Condition.CODEC.optionalFieldOf("condition").forGetter(ModuleData::condition),
            ComponentSerialization.CODEC.optionalFieldOf("condition_description")
                    .forGetter(ModuleData::conditionDescription),
            InitialState.CODEC.optionalFieldOf("initial_state", InitialState.SELECTABLE)
                    .forGetter(ModuleData::initialState),
            InsightEntry.CODEC.listOf().optionalFieldOf("insights", List.of())
                    .forGetter(ModuleData::insights),
            InsightReward.CODEC.optionalFieldOf("on_select_reward")
                    .forGetter(ModuleData::onSelectReward),
            InsightReward.CODEC.optionalFieldOf("on_complete_reward")
                    .forGetter(ModuleData::onCompleteReward),
            ComponentSerialization.CODEC.optionalFieldOf("on_select_reward_description")
                    .forGetter(ModuleData::onSelectRewardDescription),
            ComponentSerialization.CODEC.optionalFieldOf("on_complete_reward_description")
                    .forGetter(ModuleData::onCompleteRewardDescription),
            Codec.INT.optionalFieldOf("weight", 100).forGetter(ModuleData::weight)
    ).apply(instance, ModuleData::new));

    /**
     * Returns the name, falling back to a translatable key {@code module.<ns>.<path>.name}.
     */
    public Component effectiveName(ResourceLocation id) {
        return name.orElseGet(() ->
                Component.translatable("module." + id.getNamespace() + "." + id.getPath() + ".name"));
    }

    /**
     * Returns the description, falling back to a translatable key {@code module.<ns>.<path>.description}.
     */
    public Optional<Component> effectiveDescription(ResourceLocation id) {
        if (description.isPresent()) return description;
        String key = "module." + id.getNamespace() + "." + id.getPath() + ".description";
        if (Language.getInstance().has(key)) return Optional.of(Component.translatable(key));
        return Optional.empty();
    }

    public Optional<Component> effectiveConditionDescription(ResourceLocation id) {
        if (conditionDescription.isPresent()) return conditionDescription;
        String key = "module." + id.getNamespace() + "." + id.getPath() + ".condition_description";
        if (Language.getInstance().has(key)) return Optional.of(Component.translatable(key));
        return Optional.empty();
    }

    public Optional<Component> effectiveOnSelectRewardDescription(ResourceLocation id) {
        if (onSelectRewardDescription.isPresent()) return onSelectRewardDescription;
        String key = "module." + id.getNamespace() + "." + id.getPath() + ".on_select_reward_description";
        if (Language.getInstance().has(key)) return Optional.of(Component.translatable(key));
        return Optional.empty();
    }

    public Optional<Component> effectiveOnCompleteRewardDescription(ResourceLocation id) {
        if (onCompleteRewardDescription.isPresent()) return onCompleteRewardDescription;
        String key = "module." + id.getNamespace() + "." + id.getPath() + ".on_complete_reward_description";
        if (Language.getInstance().has(key)) return Optional.of(Component.translatable(key));
        return Optional.empty();
    }
}
