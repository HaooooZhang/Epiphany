package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether a specific Epiphany has been selected by the player.
 * Event-driven: {@code EpiphanySelectedEvent} → {@code checkAutoUnlock()}.
 * <p>
 * JSON: {@code {"type": "epiphany:epiphany_selected", "epiphany": "epiphany:phoenix"}}
 */
public record EpiphanySelectedCondition(ResourceLocation epiphany) implements Condition {

    public static final MapCodec<EpiphanySelectedCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("epiphany").forGetter(EpiphanySelectedCondition::epiphany)
    ).apply(instance, EpiphanySelectedCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return EpiphanyManager.isSelected(player, epiphany);
    }
}
