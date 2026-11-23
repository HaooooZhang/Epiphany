package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.ModuleManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Force-locks a Module. The inverse of {@link UnlockModuleReward}.
 * Useful for branching storylines where picking one path locks another.
 * <p>
 * JSON: {@code {"type": "epiphany:lock_module", "module": "epiphany:combat"}}
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
