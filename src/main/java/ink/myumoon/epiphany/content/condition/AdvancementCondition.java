package ink.myumoon.epiphany.content.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.Epiphany;
import net.minecraft.advancements.AdvancementHolder;
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
        AdvancementHolder holder = player.server.getAdvancements().get(this.advancement);
        if (holder == null) {
            Epiphany.LOGGER.warn("Advancement \"{}\" does not exist, referenced in epiphany:advancement condition",
                    this.advancement);
            return false;
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
