package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.EpiphanyManager;
import ink.myumoon.epiphany.api.InsightManager;
import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** The shared LDLib2 menu entry point used by the server and client. */
public final class EpiphanyUIFactory {
    public static final ResourceLocation MAIN_UI_ID = ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "main_ui");
    private static final String RPC_OPEN_UI = "epiphany.open_main_ui";

    private EpiphanyUIFactory() {}

    public static void register() {
        PlayerUIMenuType.register(MAIN_UI_ID, opener -> player -> player.level().isClientSide
                ? EpiphanyUIClientHooks.create(player) : ModularUI.of(UI.empty(), player));
    }

    public static void openFor(ServerPlayer player) {
        PlayerUIMenuType.openUI(player, MAIN_UI_ID);
    }

    public static void requestOpenFromClient(Player player) {
        RPCPacketDistributor.rpcToServer(RPC_OPEN_UI);
    }

    @RPCPacket(value = RPC_OPEN_UI, modId = Epiphany.MODID)
    public static void onOpenMainUiRpc(RPCSender sender) {
        ServerPlayer player = sender.asPlayer();
        if (player != null) openFor(player);
    }

    @RPCPacket(value = "epiphany.select_module", modId = Epiphany.MODID)
    public static void onSelectModuleRpc(RPCSender sender, String id) {
        ServerPlayer player = sender.asPlayer();
        if (player == null || id == null) return;
        ResourceLocation module = ResourceLocation.tryParse(id);
        if (module != null
                && player.server.registryAccess().registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY).containsKey(module)) {
            ModuleManager.select(player, module);
        }
    }

    @RPCPacket(value = "epiphany.select_insight", modId = Epiphany.MODID)
    public static void onSelectInsightRpc(RPCSender sender, String insightId, String moduleId) {
        ServerPlayer player = sender.asPlayer();
        if (player == null || insightId == null || moduleId == null) return;
        ResourceLocation insight = ResourceLocation.tryParse(insightId);
        ResourceLocation module = ResourceLocation.tryParse(moduleId);
        if (insight != null && module != null) InsightManager.select(player, insight, module);
    }

    @RPCPacket(value = "epiphany.select_epiphany", modId = Epiphany.MODID)
    public static void onSelectEpiphanyRpc(RPCSender sender, String id) {
        ServerPlayer player = sender.asPlayer();
        if (player == null || id == null) return;
        ResourceLocation epiphany = ResourceLocation.tryParse(id);
        if (epiphany != null) EpiphanyManager.select(player, epiphany);
    }
}
