package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.DisplayDataManager;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.content.reward.PersistentReward;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.OnDatapackSyncEvent;

@EventBusSubscriber(modid = Epiphany.MODID)
public final class EffectDatapackSyncListener {
    private EffectDatapackSyncListener() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getPlayers().forEach(player -> {
            ModuleManager.cleanupOrphanedData(player);
            EpiphanyManager.cleanupOrphanedData(player);
            DisplayDataManager.reconcile(player);
            PersistentReward.reapplyAttributes(player);
            PersistentReward.reapplyEffects(player);
        });
    }
}