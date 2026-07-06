package ink.myumoon.epiphany.content.condition.builtin.ftbq;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether a specific FTB Quests quest has been completed.
 * <p>
 * Quests are identified by their hex ID string, as shown in FTBQ quest files.
 * <p>
 * JSON: {@code {"type": "epiphany:ftbq_quest", "quest": "6D031F64B5DEFA7C"}}
 */
public record FTBQQuestCondition(String quest) implements Condition {

    public static final MapCodec<FTBQQuestCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("quest").forGetter(FTBQQuestCondition::quest)
    ).apply(instance, FTBQQuestCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return FTBQHelper.isQuestCompleted(player, quest);
    }

    @Override
    public boolean isEventDriven() {
        return true;
    }
}
