package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.InsightManager;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether a specific Insight has been selected (unlocked) by the player.
 * Event-driven: {@code InsightSelectedEvent} → {@code checkAutoUnlock()}.
 * <p>
 * JSON: {@code {"type": "epiphany:insight_selected", "insight": "epiphany:survival_health"}}
 */
public record InsightSelectedCondition(ResourceLocation insight) implements Condition {

    public static final MapCodec<InsightSelectedCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("insight").forGetter(InsightSelectedCondition::insight)
    ).apply(instance, InsightSelectedCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return InsightManager.isSelected(player, insight);
    }
}
