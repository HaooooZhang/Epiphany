package ink.myumoon.epiphany.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Player-specific display state persisted and synchronized with Epiphany data. */
public record PlayerDisplayData(
        List<ResourceLocation> selectedModuleOrder,
        List<ResourceLocation> selectedEpiphanyOrder
) {
    public PlayerDisplayData {
        selectedModuleOrder = List.copyOf(selectedModuleOrder);
        selectedEpiphanyOrder = List.copyOf(selectedEpiphanyOrder);
    }

    public static final Codec<PlayerDisplayData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf()
                    .optionalFieldOf("selectedModuleOrder", List.of())
                    .forGetter(PlayerDisplayData::selectedModuleOrder),
            ResourceLocation.CODEC.listOf()
                    .optionalFieldOf("selectedEpiphanyOrder", List.of())
                    .forGetter(PlayerDisplayData::selectedEpiphanyOrder)
    ).apply(instance, PlayerDisplayData::new));

    public static PlayerDisplayData createDefault() {
        return new PlayerDisplayData(List.of(), List.of());
    }

    public PlayerDisplayData withSelectedModuleOrder(List<ResourceLocation> order) {
        return new PlayerDisplayData(order, selectedEpiphanyOrder);
    }

    public PlayerDisplayData withSelectedEpiphanyOrder(List<ResourceLocation> order) {
        return new PlayerDisplayData(selectedModuleOrder, order);
    }
}