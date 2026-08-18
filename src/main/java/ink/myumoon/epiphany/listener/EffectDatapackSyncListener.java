package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.DisplayDataManager;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.content.reward.PersistentReward;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@EventBusSubscriber(modid = Epiphany.MODID)
public final class EffectDatapackSyncListener {
    private EffectDatapackSyncListener() {
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(player -> {
            ModuleManager.cleanupOrphanedData(player);
            EpiphanyManager.cleanupOrphanedData(player);
            DisplayDataManager.reconcile(player);
            PersistentReward.reapplyAttributes(player);
            PersistentReward.reapplyEffects(player);
        });
    }
}