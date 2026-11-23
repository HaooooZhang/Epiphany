package ink.myumoon.epiphany.content.condition.builtin;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether a specific Module has been completed by the player.
 * Event-driven: {@code ModuleCompletedEvent} → {@code checkAutoUnlock()}.
 * <p>
 * JSON: {@code {"type": "epiphany:module_completed", "module": "epiphany:combat"}}
 */
public record ModuleCompletedCondition(ResourceLocation module) implements Condition {

    public static final MapCodec<ModuleCompletedCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("module").forGetter(ModuleCompletedCondition::module)
    ).apply(instance, ModuleCompletedCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return ModuleManager.isCompleted(player, module);
    }
}
