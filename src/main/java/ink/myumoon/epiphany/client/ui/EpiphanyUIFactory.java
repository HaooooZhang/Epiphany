package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import ink.myumoon.epiphany.Epiphany;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Entry point for the Epiphany main UI.
 * <p>
 * Phase 1: registers a {@link PlayerUIMenuType} that opens a TabView with
 * three placeholder tabs (Module / Epiphany / Status). Subsequent phases
 * will replace the tab contents with real widgets.
 * <p>
 * This UI is Menu-based (uses {@code ModularUI.of(ui, player)}) so that
 * the player's {@link ink.myumoon.epiphany.attachment.PlayerEpiphanyData}
 * Attachment sync channel works correctly for client-side reads.
 */
public final class EpiphanyUIFactory {

    /** Unique id of the main Epiphany UI. Must match on both sides. */
    public static final ResourceLocation MAIN_UI_ID =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "main_ui");

    private EpiphanyUIFactory() {
    }

    /**
     * Register the UI holder. Must be called on both sides during
     * {@code FMLCommonSetupEvent} so the menu type resolves consistently.
     */
    public static void register() {
        PlayerUIMenuType.register(MAIN_UI_ID, serverPlayer ->
                player -> ModularUI.of(buildUI(player), player));
    }

    /**
     * Open the main Epiphany UI for a server-side player. Must be invoked
     * on the server thread (from a command handler, key binding handler, etc.).
     */
    public static void openFor(net.minecraft.server.level.ServerPlayer player) {
        PlayerUIMenuType.openUI(player, MAIN_UI_ID);
    }

    /**
     * Called by the client when the player presses the open-UI key.
     * Sends a {@code @RPCPacket} to the server, which validates the player
     * and triggers the UI open on the server side (Menu-based UI security).
     */
    public static void requestOpenFromClient(Player player) {
        RPCPacketDistributor.rpcToServer(RPC_OPEN_UI);
    }

    /**
     * Server-side RPC handler. Triggered by the client keypress; opens the
     * main UI for the sending player. The player parameter is supplied
     * via {@link RPCSender} so we never trust arbitrary client-supplied IDs.
     */
    /**
     * Server-side RPC handler. Triggered by the client keypress; opens the
     * main UI for the sending player. {@link RPCSender#asPlayer()} gives us
     * the verified ServerPlayer (LDLib2 wires it from the packet sender, so
     * we never trust arbitrary client-supplied IDs).
     * <p>
     * Note: per LDLib2 source, the RPC id is the bare {@code value()}
     * (no namespace prefix). The {@code modId} attribute is only used to
     * skip registration if the owning mod is not loaded.
     */
    @RPCPacket(value = "epiphany.open_main_ui", modId = Epiphany.MODID)
    public static void onOpenMainUiRpc(RPCSender sender) {
        // Called from client → executing on server. asPlayer() is the verified ServerPlayer.
        PlayerUIMenuType.openUI(sender.asPlayer(), MAIN_UI_ID);
    }

    /** The RPC id used for client → server UI open requests (bare value, not namespaced). */
    private static final String RPC_OPEN_UI = "epiphany.open_main_ui";

    private static UI buildUI(Player player) {
        return UI.of(buildRoot(), StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP));
    }

    /**
     * Build the root element tree.
     * <p>
     * Structure:
     * <pre>
     *   root (column flex)
     *   ├── TopBarWidget   (always visible at top)
     *   └── TabView        (Module / Epiphany / Status tabs)
     * </pre>
     */
    private static UIElement buildRoot() {
        var root = new UIElement();
        root.layout(l -> l.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN)
                .widthPercent(100).heightPercent(100));

        // 1. Top bar with Aptitude progress + Insight Point badge.
        root.addChild(TopBarWidget.create());

        // 2. Tab view below the top bar.
        var tabView = new TabView();
        tabView.layout(l -> l.flexGrow(1).widthPercent(100));
        tabView.addTab(
                new Tab().setText(Component.translatable("epiphany.ui.tab.module")),
                ink.myumoon.epiphany.client.ui.module.ModuleTabContent.create());
        tabView.addTab(
                new Tab().setText(Component.translatable("epiphany.ui.tab.epiphany")),
                ink.myumoon.epiphany.client.ui.epiphany.EpiphanyTabContent.create());
        tabView.addTab(
                new Tab().setText(Component.translatable("epiphany.ui.tab.status")),
                StatusPanel.create());
        root.addChild(tabView);

        return root;
    }

    /**
     * Minimal placeholder content for a tab — a single centered label.
     * Replaced in subsequent phases by real widgets.
     */
    private static UIElement placeholderPanel(String translationKey) {
        return new UIElement().addChild(
                new Label().setText(Component.translatable(translationKey)));
    }
}
