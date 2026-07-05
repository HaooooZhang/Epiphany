package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.AptitudeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Modifies the player's aptitude.
 * <p>
 * JSON: {@code {"type": "epiphany:aptitude", "amount": 100}}
 */
public record AptitudeReward(long amount) implements InsightReward, EpiphanyReward {

    public static final MapCodec<AptitudeReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.LONG.fieldOf("amount").forGetter(AptitudeReward::amount)
    ).apply(instance, AptitudeReward::new));

    @Override
    public MapCodec<AptitudeReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        if (amount > 0) {
            AptitudeManager.addAptitude(player, amount);
        } else {
            AptitudeManager.setAptitude(player,
                    Math.max(0, AptitudeManager.getAptitude(player) + amount));
        }
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        AptitudeManager.setAptitude(player,
                Math.max(0, AptitudeManager.getAptitude(player) - amount));
    }
}
