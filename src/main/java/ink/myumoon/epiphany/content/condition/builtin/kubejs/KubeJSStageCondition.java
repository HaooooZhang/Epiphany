package ink.myumoon.epiphany.content.condition.builtin.kubejs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.Condition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks whether a player has a specific KubeJS stage.
 * <p>
 * Uses {@code StageEvents.get(player).has(stage)} internally.
 * <p>
 * JSON: {@code {"type": "epiphany:kubejs_stage", "stage": "nether_access"}}
 */
public record KubeJSStageCondition(String stage) implements Condition {

    public static final MapCodec<KubeJSStageCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("stage").forGetter(KubeJSStageCondition::stage)
    ).apply(instance, KubeJSStageCondition::new));

    @Override
    public MapCodec<? extends Condition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return KubeJSHelper.hasStage(player, stage);
    }
}
