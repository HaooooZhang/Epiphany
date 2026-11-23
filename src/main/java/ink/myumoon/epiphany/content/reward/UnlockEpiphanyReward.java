package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.EpiphanyManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Force-unlocks an Epiphany. Used as a cross-reference: completing one Module
 * can make an Epiphany available.
 * <p>
 * JSON: {@code {"type": "epiphany:unlock_epiphany", "epiphany": "epiphany:phoenix"}}
 */
public record UnlockEpiphanyReward(ResourceLocation epiphany) implements InsightReward, EpiphanyReward {

    public static final MapCodec<UnlockEpiphanyReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("epiphany").forGetter(UnlockEpiphanyReward::epiphany)
    ).apply(instance, UnlockEpiphanyReward::new));

    @Override
    public MapCodec<UnlockEpiphanyReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        EpiphanyManager.setUnlocked(player, epiphany, true);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        EpiphanyManager.setUnlocked(player, epiphany, false);
    }
}
