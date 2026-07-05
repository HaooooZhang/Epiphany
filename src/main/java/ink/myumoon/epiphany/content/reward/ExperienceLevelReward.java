package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants experience levels.
 * <p>
 * JSON: {@code {"type": "epiphany:experience_level", "levels": 5}}
 */
public record ExperienceLevelReward(int levels) implements InsightReward, EpiphanyReward {

    public static final MapCodec<ExperienceLevelReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.INT.fieldOf("levels").forGetter(ExperienceLevelReward::levels)
    ).apply(instance, ExperienceLevelReward::new));

    @Override
    public MapCodec<ExperienceLevelReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        player.giveExperienceLevels(levels);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        player.giveExperienceLevels(-levels);
    }
}
