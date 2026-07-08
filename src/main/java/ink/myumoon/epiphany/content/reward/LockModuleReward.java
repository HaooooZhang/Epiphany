package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.ModuleManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Force-locks a Module. The mirror of {@link UnlockModuleReward}: useful as a
 * "curse-style" reward that removes module availability from the player.
 * <p>
 * JSON: {@code {"type": "epiphany:lock_module", "module": "epiphany:combat"}}
 * <p>
 * Semantics:
 * <ul>
 *   <li>{@code apply} → {@code ModuleManager.setUnlocked(player, id, false)}</li>
 *   <li>{@code remove} → {@code ModuleManager.setUnlocked(player, id, true)}
 *       (restores unlock so the reward can be re-issued / reset cleanly)</li>
 * </ul>
 * <p>
 * Note: this only clears the player-side {@code unlocked} flag. It does <b>not</b>
 * unselect the module if the player had already chosen it, nor reset insights.
 * Combine with a crafted condition chain (or admin {@code /epiphany reset}) for
 * full rollback.
 */
public record LockModuleReward(ResourceLocation module) implements InsightReward, EpiphanyReward {

    public static final MapCodec<LockModuleReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("module").forGetter(LockModuleReward::module)
    ).apply(instance, LockModuleReward::new));

    @Override
    public MapCodec<LockModuleReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        ModuleManager.setUnlocked(player, module, false);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        ModuleManager.setUnlocked(player, module, true);
    }
}
