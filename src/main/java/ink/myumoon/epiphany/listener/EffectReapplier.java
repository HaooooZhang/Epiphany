package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Epiphany.MODID)
public final class EffectReapplier {

    private static final String KEY = "epiphany_permanent_effects";

    private EffectReapplier() {}

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            var list = player.getPersistentData().getList(KEY, Tag.TAG_STRING);
            if (list.isEmpty()) continue;

            for (int i = 0; i < list.size(); i++) {
                String[] parts = list.getString(i).split("\\|", 2);
                if (parts.length != 2) continue;
                var id = ResourceLocation.tryParse(parts[0]);
                if (id == null) continue;
                int amp;
                try { amp = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { continue; }
                var holder = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
                if (holder == null) continue;
                if (!player.hasEffect(holder)) {
                    player.addEffect(new MobEffectInstance(holder, -1, amp, false, false, true), null);
                }
            }
        }
    }
}
