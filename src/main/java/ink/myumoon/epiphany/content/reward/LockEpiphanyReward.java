package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.EpiphanyManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Force-locks an Epiphany. The inverse of {@link UnlockEpiphanyReward}.
 * Useful for branching storylines where picking one path locks another.
 * <p>
 * JSON: {@code {"type": "epiphany:lock_epiphany", "epiphany": "epiphany:phoenix"}}
 */
public record LockEpiphanyReward(ResourceLocation epiphany) implements InsightReward, EpiphanyReward {

    public static final MapCodec<LockEpiphanyReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("epiphany").forGetter(LockEpiphanyReward::epiphany)
    ).apply(instance, LockEpiphanyReward::new));

    @Override
    public MapCodec<LockEpiphanyReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        EpiphanyManager.setUnlocked(player, epiphany, false);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        EpiphanyManager.setUnlocked(player, epiphany, true);
    }
}
