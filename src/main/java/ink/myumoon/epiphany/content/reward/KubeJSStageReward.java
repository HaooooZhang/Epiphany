package ink.myumoon.epiphany.content.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ink.myumoon.epiphany.content.condition.builtin.kubejs.KubeJSHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Adds or removes a KubeJS stage when granted/revoked.
 * <p>
 * Supports both {@link InsightReward} and {@link EpiphanyReward}.
 * <p>
 * JSON: {@code {"type": "epiphany:kubejs_stage", "stage": "nether_access", "action": "add"}}
 * <p>
 * {@code action} defaults to {@code "add"}. When the reward is revoked
 * ({@link #remove}), the opposite action is performed.
 */
public record KubeJSStageReward(String stage, Action action) implements InsightReward, EpiphanyReward {

    public enum Action {
        ADD, REMOVE;

        public static final Codec<Action> CODEC = Codec.STRING.xmap(
                s -> "remove".equals(s) ? REMOVE : ADD,
                a -> a == REMOVE ? "remove" : "add"
        );
    }

    public static final MapCodec<KubeJSStageReward> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("stage").forGetter(KubeJSStageReward::stage),
            Action.CODEC.optionalFieldOf("action", Action.ADD).forGetter(KubeJSStageReward::action)
    ).apply(instance, KubeJSStageReward::new));

    @Override
    public MapCodec<KubeJSStageReward> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, ResourceLocation sourceId) {
        if (action == Action.ADD) {
            KubeJSHelper.addStage(player, stage);
        } else {
            KubeJSHelper.removeStage(player, stage);
        }
    }

    @Override
    public void remove(ServerPlayer player, ResourceLocation sourceId) {
        // Reverse: if we added it, remove it; if we removed it, add it back
        if (action == Action.ADD) {
            KubeJSHelper.removeStage(player, stage);
        } else {
            KubeJSHelper.addStage(player, stage);
        }
    }
}
