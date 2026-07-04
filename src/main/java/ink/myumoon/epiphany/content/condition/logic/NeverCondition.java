package ink.myumoon.epiphany.content.condition.logic;

import com.mojang.serialization.MapCodec;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

/**
 * A condition that always evaluates to {@code false}.
 * Useful for pack makers to disable content or as a placeholder.
 */
public enum NeverCondition implements Condition {
    INSTANCE;

    public static final MapCodec<NeverCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return false;
    }
}
