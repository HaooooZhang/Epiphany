package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.InsightManager;
import ink.myumoon.epiphany.api.ModuleManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Entry point for the Epiphany main UI. The server creates an empty menu UI;
 * the client loads {@code assets/epiphany/ui/main.xml} and wires its controllers.
 * <p>
 * LDLib2 invokes the registered factory once per side. The client reads the
 * player data attachment after it has been synchronized from the server.
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
     * player instance. The server returns an empty UI because display and data
     * binding work is client-side. If the client XML is missing, it also
     * returns a fallback empty UI.
     */
    public static void register() {
        PlayerUIMenuType.register(MAIN_UI_ID, outerPlayer -> createUiPlayer -> {
            if (!createUiPlayer.level().isClientSide) {
                return ModularUI.of(UI.empty(), createUiPlayer);
            }

            var xml = XmlUtils.loadXml(MAIN_UI_XML);
            if (xml == null) {
                Epiphany.LOGGER.error("Failed to load Epiphany main UI XML: {}", MAIN_UI_XML);
                return ModularUI.of(UI.empty(), createUiPlayer);
            }
            var ui = UI.of(xml);
            EpiphanyUIClientHooks.attach(ui);
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
