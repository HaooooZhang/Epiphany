package ink.myumoon.epiphany.content.reward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Shared source-aware bridge for rewards usable by both reward registries. */
public interface RewardSourceAware {

    void apply(ServerPlayer player, ResourceLocation sourceId);

    default void apply(ServerPlayer player, RewardSource source) {
        apply(player, source.legacyId());
    }

    default void remove(ServerPlayer player, ResourceLocation sourceId) {
    }

    default void remove(ServerPlayer player, RewardSource source) {
        remove(player, source.legacyId());
    }
}