package ink.myumoon.epiphany.content.condition.builtin.ftbq;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether at least {@code count} FTB Quests quests with the given
 * tag have been completed by the player's team.
 * <p>
 * JSON: {@code {"type": "epiphany:ftbq_tag", "tag": "boss_kill", "count": 3}}
 */
public record FTBQTagCondition(String tag, int count) implements Condition {

    public static final MapCodec<FTBQTagCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("tag").forGetter(FTBQTagCondition::tag),
            Codec.INT.optionalFieldOf("count", 1).forGetter(FTBQTagCondition::count)
    ).apply(instance, FTBQTagCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return FTBQHelper.countCompletedQuestsWithTag(player, tag) >= count;
    }

    @Override
    public boolean isEventDriven() {
        return true;
    }
}
