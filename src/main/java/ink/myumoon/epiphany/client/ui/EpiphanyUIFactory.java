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
import ink.myumoon.epiphany.client.ui.epiphany.EpiphanySelectController;
import ink.myumoon.epiphany.client.ui.epiphany.EpiphanySlotColumnController;
import ink.myumoon.epiphany.client.ui.module.ModuleGridController;
import ink.myumoon.epiphany.client.ui.module.ModuleSelectController;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Entry point for the Epiphany main UI. Registers a {@link PlayerUIMenuType}
 * that builds the screen from {@code assets/epiphany/ui/main.xml} and wires
 * up its controllers.
 * <p>
 * This is a Menu‑based UI: LDLib2 invokes the registered factory once per side
 * (server with ServerPlayer, client with LocalPlayer). All S→C data bindings
 * (e.g. in {@link TopBarController}) are safe because they run only on the
 * server side.
 */
public final class EpiphanyUIFactory {

    public static final ResourceLocation MAIN_UI_ID =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "main_ui");

    private static final String RPC_OPEN_UI = "epiphany.open_main_ui";

    private static final ResourceLocation MAIN_UI_XML =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "ui/main.xml");

    private EpiphanyUIFactory() {
    }

    /**
     * Registers the UI factory. Called during common setup on both sides.
     * <p>
     * The factory lambda takes an outer {@code Player} (the menu opener) and
     * returns a {@code PlayerUIHolder} whose {@code createUI} receives the same
     * player instance. We use the inner parameter for binding so that data
     * binding getters see the side‑specific player (ServerPlayer on server,
     * LocalPlayer on client). The UI XML is loaded from assets; if missing,
     * returns a fallback empty UI.
     */
    public static void register() {
        PlayerUIMenuType.register(MAIN_UI_ID, outerPlayer -> createUiPlayer -> {
            var xml = XmlUtils.loadXml(MAIN_UI_XML);
            if (xml == null) {
                Epiphany.LOGGER.error("Failed to load Epiphany main UI XML: {}", MAIN_UI_XML);
                return ModularUI.of(UI.of(new UIElement()), createUiPlayer);
            }
            var ui = UI.of(xml);
            TopBarController.bind(ui);
            ModuleGridController.attach(ui);
            EpiphanySlotColumnController.attach(ui);
            ModuleSelectController.attach(ui);
            EpiphanySelectController.attach(ui);
            return ModularUI.of(ui, createUiPlayer);
        });
    }

    /** Opens the UI for a server‑side player. Must be called on the server thread. */
    public static void openFor(ServerPlayer player) {
        PlayerUIMenuType.openUI(player, MAIN_UI_ID);
    }

    /** Opens the UI for a server‑side player. Must be called on the server thread. */
    public static void requestOpenFromClient(Player player) {
        RPCPacketDistributor.rpcToServer(RPC_OPEN_UI);
    }

    @RPCPacket(value = RPC_OPEN_UI, modId = Epiphany.MODID)
    public static void onOpenMainUiRpc(RPCSender sender) {
        Player target = sender.asPlayer();
        if (target instanceof ServerPlayer serverPlayer) {
            openFor(serverPlayer);
        }
    }

    @RPCPacket(value = "epiphany.select_insight", modId = Epiphany.MODID)
    public static void onSelectInsightRpc(RPCSender sender, String insightIdStr, String moduleIdStr) {
        Player target = sender.asPlayer();
        if (target instanceof ServerPlayer sp) {
            InsightManager.select(sp,
                    ResourceLocation.parse(insightIdStr),
                    ResourceLocation.parse(moduleIdStr));
        }
    }

    @RPCPacket(value = "epiphany.select_epiphany", modId = Epiphany.MODID)
    public static void onSelectEpiphanyRpc(RPCSender sender, String epiphanyIdStr) {
        Player target = sender.asPlayer();
        if (target instanceof ServerPlayer sp) {
            EpiphanyManager.select(sp, ResourceLocation.parse(epiphanyIdStr));
        }
    }

    @RPCPacket(value = "epiphany.select_module", modId = Epiphany.MODID)
    public static void onSelectModuleRpc(RPCSender sender, String moduleIdStr) {
        Player target = sender.asPlayer();
        if (target instanceof ServerPlayer sp) {
            ModuleManager.select(sp, ResourceLocation.parse(moduleIdStr));
        }
    }
}
