package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.ModuleManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Force-unlocks a Module. Used as a cross-reference: completing one Module
 * can unlock another.
 * <p>
 * JSON: {@code {"type": "epiphany:unlock_module", "module": "epiphany:combat"}}
 */
public record UnlockModuleReward(ResourceLocation module) implements InsightReward, EpiphanyReward {

    public static final MapCodec<UnlockModuleReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("module").forGetter(UnlockModuleReward::module)
    ).apply(instance, UnlockModuleReward::new));

    @Override
    public MapCodec<UnlockModuleReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        ModuleManager.setUnlocked(player, module, true);
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        ModuleManager.setUnlocked(player, module, false);
    }
}
