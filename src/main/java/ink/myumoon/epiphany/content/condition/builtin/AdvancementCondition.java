package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether the player has completed a specific advancement.
 * <p>
 * JSON: {@code {"type": "epiphany:advancement", "advancement": "minecraft:story/mine_stone"}}
 */
public record AdvancementCondition(ResourceLocation advancement) implements Condition {
    public static final MapCodec<AdvancementCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("advancement").forGetter(AdvancementCondition::advancement)
    ).apply(instance, AdvancementCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        Advancement holder = player.server.getAdvancements().getAdvancement(this.advancement);
        if (holder == null) {
            Epiphany.LOGGER.warn("Advancement \"{}\" does not exist, referenced in epiphany:advancement condition",
                    this.advancement);
            return false;
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
