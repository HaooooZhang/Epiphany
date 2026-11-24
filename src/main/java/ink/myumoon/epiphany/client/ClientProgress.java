package ink.myumoon.epiphany.client;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.config.ProgressionRules;
import ink.myumoon.epiphany.config.SelectionRules;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = Epiphany.MODID, value = Dist.CLIENT)
public final class ClientProgress {
    private static PlayerEpiphanyData progress;
    private static ProgressionRules rules;
    private static SelectionRules selectionRules;

    private ClientProgress() {
    }

    public static Optional<PlayerEpiphanyData> progress() {
        return Optional.ofNullable(progress);
    }

    public static Optional<ProgressionRules> rules() {
        return Optional.ofNullable(rules);
    }

    public static Optional<SelectionRules> selectionRules() {
        return Optional.ofNullable(selectionRules);
    }

    public static void receiveSelectionRules(Connection connection, SelectionRules snapshot) {
        if (isCurrentConnection(connection)) selectionRules = snapshot;
    }

    public static void receiveProgress(Connection connection, PlayerEpiphanyData snapshot) {
        if (isCurrentConnection(connection)) progress = snapshot;
    }

    public static void receiveRules(Connection connection, ProgressionRules snapshot) {
        if (isCurrentConnection(connection)) rules = snapshot;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        progress = null;
        rules = null;
        selectionRules = null;
    }

    private static boolean isCurrentConnection(Connection connection) {
        var listener = Minecraft.getInstance().getConnection();
        return listener != null && listener.getConnection() == connection;
    }
}
