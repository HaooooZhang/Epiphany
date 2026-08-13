package ink.myumoon.epiphany.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Player-specific display state persisted and synchronized with Epiphany data. */
public record PlayerDisplayData(
        List<ResourceLocation> selectedModuleOrder
) {
    public PlayerDisplayData {
        selectedModuleOrder = List.copyOf(selectedModuleOrder);
    }

    public static final Codec<PlayerDisplayData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf()
                    .optionalFieldOf("selectedModuleOrder", List.of())
                    .forGetter(PlayerDisplayData::selectedModuleOrder)
    ).apply(instance, PlayerDisplayData::new));

    public static PlayerDisplayData createDefault() {
        return new PlayerDisplayData(List.of());
    }

    public PlayerDisplayData withSelectedModuleOrder(List<ResourceLocation> order) {
        return new PlayerDisplayData(order);
    }
}