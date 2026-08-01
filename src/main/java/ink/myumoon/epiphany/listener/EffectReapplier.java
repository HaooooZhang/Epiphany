package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.reward.EffectReward;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Epiphany.MODID)
public final class EffectReapplier {
    private EffectReapplier() {}

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            EffectReward.reapplyStoredEffects(player);
        }
    }
}
