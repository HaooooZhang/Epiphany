package ink.myumoon.epiphany.content.condition.builtin.ftbq;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether a specific FTB Quests chapter has been started.
 * <p>
 * Chapters are identified by their hex ID string, as shown in FTBQ chapter files.
 * <p>
 * JSON: {@code {"type": "epiphany:ftbq_chapter_started", "chapter": "51AE1B0A0CF7A142"}}
 */
public record FTBQChapterStartedCondition(String chapter) implements Condition {

    public static final MapCodec<FTBQChapterStartedCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("chapter").forGetter(FTBQChapterStartedCondition::chapter)
    ).apply(instance, FTBQChapterStartedCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return FTBQHelper.isChapterStarted(player, chapter);
    }

    @Override
    public boolean isEventDriven() {
        return true;
    }
}
