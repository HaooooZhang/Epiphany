package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants experience points.
 * <p>
 * JSON: {@code {"type": "epiphany:experience", "amount": 100}}
 */
public record ExperienceReward(int amount) implements InsightReward, EpiphanyReward {

    public static final MapCodec<ExperienceReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.INT.fieldOf("amount").forGetter(ExperienceReward::amount)
    ).apply(instance, ExperienceReward::new));

    @Override
    public MapCodec<ExperienceReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        player.giveExperiencePoints(amount);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        player.giveExperiencePoints(-amount);
    }
}
