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
        InitialState initialState,
        Optional<EpiphanyReward> reward,
        int weight
) {
    public static final Codec<EpiphanyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(EpiphanyData::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(EpiphanyData::description),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(EpiphanyData::icon),
            ResourceLocation.CODEC.optionalFieldOf("path").forGetter(EpiphanyData::path),
            Condition.CODEC.optionalFieldOf("condition").forGetter(EpiphanyData::condition),
            InitialState.CODEC.optionalFieldOf("initial_state", InitialState.LOCKED)
                    .forGetter(EpiphanyData::initialState),
            EpiphanyReward.CODEC.optionalFieldOf("reward").forGetter(EpiphanyData::reward),
            Codec.INT.optionalFieldOf("weight", 100).forGetter(EpiphanyData::weight)
    ).apply(instance, EpiphanyData::new));
}
