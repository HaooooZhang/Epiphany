package ink.myumoon.epiphany.content.condition;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;

/**
 * A condition that always evaluates to {@code true}.
 * Used as the default condition when JSON parsing fails or no condition is specified.
 */
public enum AlwaysCondition implements Condition {
    INSTANCE;

    public static final MapCodec<AlwaysCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return true;
    }
}
