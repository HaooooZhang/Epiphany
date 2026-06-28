package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import ink.myumoon.epiphany.content.reward.InsightReward;
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
        InitialState initialState,
        List<InsightEntry> insights,
        Optional<InsightReward> onSelectReward,
        Optional<InsightReward> onCompleteReward,
        int weight
) {
    public static final Codec<ModuleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(ModuleData::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(ModuleData::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(ModuleData::icon),
            Condition.CODEC.optionalFieldOf("condition").forGetter(ModuleData::condition),
            InitialState.CODEC.optionalFieldOf("initial_state", InitialState.LOCKED)
                    .forGetter(ModuleData::initialState),
            InsightEntry.CODEC.listOf().optionalFieldOf("insights", List.of())
                    .forGetter(ModuleData::insights),
            InsightReward.CODEC.optionalFieldOf("on_select_reward")
                    .forGetter(ModuleData::onSelectReward),
            InsightReward.CODEC.optionalFieldOf("on_complete_reward")
                    .forGetter(ModuleData::onCompleteReward),
            Codec.INT.optionalFieldOf("weight", 100).forGetter(ModuleData::weight)
    ).apply(instance, ModuleData::new));
}
