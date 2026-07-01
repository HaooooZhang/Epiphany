package ink.myumoon.epiphany;

import com.mojang.blaze3d.platform.InputConstants;
import ink.myumoon.epiphany.client.ui.EpiphanyUIFactory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Epiphany.MODID, dist = Dist.CLIENT)
public class EpiphanyClient {
    /** The key that opens the main Epiphany UI. Defaults to K. */
    public static final KeyMapping OPEN_UI_KEY = new KeyMapping(
            "key.epiphany.open_ui",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.epiphany");

    public EpiphanyClient(ModContainer container, net.neoforged.bus.api.IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // Register keybinding on the mod event bus (where Register* events fire).
        modEventBus.addListener(this::onRegisterKeyMappings);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_UI_KEY);
    }

    @EventBusSubscriber(modid = Epiphany.MODID, value = Dist.CLIENT)
    public static class ClientGameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            // Consume the keypress on the client and request the UI from the server.
            // The actual UI open must happen on the server (Menu-based UI security).
            while (OPEN_UI_KEY.consumeClick()) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    EpiphanyUIFactory.requestOpenFromClient(player);
                }
            }
        }
    }
}
