package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.reward.EffectReward;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;

@EventBusSubscriber(modid = Epiphany.MODID)
public final class EffectReapplier {
    private EffectReapplier() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            if (player.isAlive()) EffectReward.reapplyStoredEffects(player);
        }
    }
}
