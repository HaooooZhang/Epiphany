package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.EpiphanyManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Force-locks an Epiphany. The mirror of {@link UnlockEpiphanyReward}: useful
 * as a "curse-style" reward that takes options off the table.
 * <p>
 * JSON: {@code {"type": "epiphany:lock_epiphany", "epiphany": "epiphany:phoenix"}}
 * <p>
 * Semantics:
 * <ul>
 *   <li>{@code apply} → {@code EpiphanyManager.setUnlocked(player, id, false)}</li>
 *   <li>{@code remove} → {@code EpiphanyManager.setUnlocked(player, id, true)}
 *       (restores unlock so the reward can be re-issued / reset cleanly)</li>
 * </ul>
 * <p>
 * Note: This reward is unusual — typically you'd use the {@code epiphany:condition}
 * on the target Epiphany to gate visibility. Use this only when one player's choice
 * must dynamically remove another option from the same player.
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
