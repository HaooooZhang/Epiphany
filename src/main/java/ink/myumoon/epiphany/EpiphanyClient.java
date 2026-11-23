package ink.myumoon.epiphany;

import com.mojang.blaze3d.platform.InputConstants;
import ink.myumoon.epiphany.client.ui.EpiphanyUIFactory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ink.myumoon.epiphany.client.ui.progression.EpiphanyConfigurationScreen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class EpiphanyClient {
    public static final KeyMapping OPEN_UI_KEY = new KeyMapping(
            "key.epiphany.open_ui", InputConstants.Type.KEYSYM, InputConstants.KEY_K, "key.categories.epiphany");

    private EpiphanyClient() {}

    @Mod.EventBusSubscriber(modid = Epiphany.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerConfigurationScreen(FMLClientSetupEvent event) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, parent) -> new EpiphanyConfigurationScreen(parent)));
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_UI_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = Epiphany.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            boolean open = OPEN_UI_KEY.consumeClick();
            while (OPEN_UI_KEY.consumeClick()) {
                open = true;
            }
            var minecraft = Minecraft.getInstance();
            if (open && minecraft.player != null && minecraft.screen == null) {
                EpiphanyUIFactory.requestOpenFromClient(minecraft.player);
            }
        }
    }
}
