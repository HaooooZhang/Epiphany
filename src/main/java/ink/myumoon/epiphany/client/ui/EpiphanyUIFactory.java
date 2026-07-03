package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.InsightManager;
import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.client.ui.epiphany.EpiphanySlotColumnController;
import ink.myumoon.epiphany.client.ui.module.ModuleGridController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Entry point for the Epiphany main UI.
 * <p>
 * Registers a {@link PlayerUIMenuType} that builds the main screen from
 * {@code assets/epiphany/ui/main.xml} and wires up its controllers.
 * <p>
 * LDLib2 invokes the registered {@code Function<Player, PlayerUIHolder>}
 * <b>once per side</b> when the menu opens (server → ServerPlayer, client →
 * LocalPlayer). The {@code player} parameter caught in the outer lambda is the
 * same instance handed to the inner {@code PlayerUIHolder.createUI(p)}.
 * <p>
 * Because this UI is Menu-based, all S→C reads inside
 * {@link com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder}
 * getters (used by {@link TopBarController} etc.) will run with the correct
 * side-specific player (server getter executes only on the server side; client
 * binding ignores the getter).
 */
public final class EpiphanyUIFactory {

    /** Unique id of the main Epiphany UI. Must match on both sides. */
    public static final ResourceLocation MAIN_UI_ID =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "main_ui");

    /** Bare RPC id used for client → server open requests (no namespace prefix). */
    private static final String RPC_OPEN_UI = "epiphany.open_main_ui";

    /** XML template path inside our assets directory. */
    private static final ResourceLocation MAIN_UI_XML =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "ui/main.xml");

    private EpiphanyUIFactory() {
    }

    /**
     * Register the UI holder. Called from {@link ink.myumoon.epiphany.Epiphany}
     * during {@code FMLCommonSetupEvent} on both sides.
     * <p>
     * LDLib2's {@code PlayerUIMenuType.register} takes a
     * {@code Function<Player, PlayerUIHolder>}. {@code PlayerUIHolder} is a
     * {@code @FunctionalInterface} with one abstract method
     * {@code createUI(Player)} — so the holder lambda is two layers:
     * {@code outerPlayer -> createUiPlayer -> ModularUI.of(ui, createUiPlayer)}.
     * <p>
     * In practice the container menu invokes the outer function once with the
     * player opening the menu, then immediately calls {@code createUI(player)}
     * with the same player instance — so the two parameters carry the same
     * object. We use the createUI-side parameter for binding so the
     * DataBindingBuilder getters see the right side-specific player.
     */
    public static void register() {
        PlayerUIMenuType.register(MAIN_UI_ID, outerPlayer -> createUiPlayer -> {
            var xml = XmlUtils.loadXml(MAIN_UI_XML);
            if (xml == null) {
                Epiphany.LOGGER.error("Failed to load Epiphany main UI XML: {}", MAIN_UI_XML);
                return ModularUI.of(UI.of(new UIElement()), createUiPlayer);
            }
            var ui = UI.of(xml);
            // Bind all controllers — each one selects its own elements via ui.select("#id").
            // Runs once per side (server+client) each time the menu opens.
            TopBarController.bind(ui);
            ModuleGridController.attach(ui);
            EpiphanySlotColumnController.attach(ui);
            // Phase B: popup controllers.
            ink.myumoon.epiphany.client.ui.module.ModuleSelectController.attach(ui);
            ink.myumoon.epiphany.client.ui.epiphany.EpiphanySelectController.attach(ui);
            return ModularUI.of(ui, createUiPlayer);
        });
    }

    /**
     * Open the main Epiphany UI for a server-side player. Must be invoked
     * on the server thread (command handler, RPC handler, key binding handler...).
     */
    public static void openFor(ServerPlayer player) {
        PlayerUIMenuType.openUI(player, MAIN_UI_ID);
    }

    /**
     * Called by the client when the player presses the open-UI key.
     * Sends an {@code @RPCPacket} to the server, which validates the player
     * and triggers the UI open on the server side (Menu-based UI security).
     */
    public static void requestOpenFromClient(Player player) {
        RPCPacketDistributor.rpcToServer(RPC_OPEN_UI);
    }

    /**
     * Server-side RPC handler. Triggered by the client keypress; opens the
     * main UI for the sending player. {@link RPCSender#asPlayer()} gives us
     * the verified ServerPlayer (LDLib2 wires it from the packet sender, so
     * we never trust arbitrary client-supplied IDs).
     */
    @RPCPacket(value = RPC_OPEN_UI, modId = Epiphany.MODID)
    public static void onOpenMainUiRpc(RPCSender sender) {
        Player target = sender.asPlayer();
        if (target instanceof ServerPlayer serverPlayer) {
            openFor(serverPlayer);
        }
    }

    /** Server-side: client clicked an Insight node. */
    @RPCPacket(value = "epiphany.select_insight", modId = Epiphany.MODID)
    public static void onSelectInsightRpc(RPCSender sender, String insightIdStr, String moduleIdStr) {
        Player target = sender.asPlayer();
        if (target instanceof ServerPlayer sp) {
            InsightManager.select(sp,
                    ResourceLocation.parse(insightIdStr),
                    ResourceLocation.parse(moduleIdStr));
        }
    }

    /** Server-side: client clicked an Epiphany card. */
    @RPCPacket(value = "epiphany.select_epiphany", modId = Epiphany.MODID)
    public static void onSelectEpiphanyRpc(RPCSender sender, String epiphanyIdStr) {
        Player target = sender.asPlayer();
        if (target instanceof ServerPlayer sp) {
            EpiphanyManager.select(sp, ResourceLocation.parse(epiphanyIdStr));
        }
    }

    /** Server-side: client clicked a Module card. */
    @RPCPacket(value = "epiphany.select_module", modId = Epiphany.MODID)
    public static void onSelectModuleRpc(RPCSender sender, String moduleIdStr) {
        Player target = sender.asPlayer();
        if (target instanceof ServerPlayer sp) {
            ModuleManager.select(sp, ResourceLocation.parse(moduleIdStr));
        }
    }
}
