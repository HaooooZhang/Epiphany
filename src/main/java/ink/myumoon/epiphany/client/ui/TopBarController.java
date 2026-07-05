package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import ink.myumoon.epiphany.api.AptitudeFormula;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Binds the Epiphany main UI top bar to server-side player data.
 * <p>
 * All S→C bindings read {@link ServerPlayer} via {@code element.getModularUI().player}
 * — this is safe because DataBindingBuilder getters are evaluated only on the server
 * thread (LDLib2 syncs the result to the client).
 */
public final class TopBarController {

    private TopBarController() {
    }

    public static void bind(UI ui) {
        ProgressBar aptBar = selectOne(ui, "#apt-bar", ProgressBar.class);
        Label pointValue = selectOne(ui, "#point-value", Label.class);

        // Force small, white text on the progress bar's built‑in label
        aptBar.label(l -> l.textStyle(t -> t.fontSize(9).textColor(0xFFFFFFFF).textShadow(true)));

        // LDLib2's CSS doesn't cascade font‑size reliably, so set it directly
        pointValue.textStyle(t -> t.fontSize(16).textColor(0xFFFFFFFF).textShadow(true));

        // Fill ratio
        aptBar.bind(DataBindingBuilder
                .<Float>floatValS2C(() -> aptRatio(serverPlayerOf(aptBar)))
                .build());

        // Built‑in label text "current / cap"
        Label barLabel = aptBar.label;
        barLabel.bind(DataBindingBuilder
                .<Component>componentS2C(() -> aptText(serverPlayerOf(aptBar)))
                .build());

        // Insight point count
        pointValue.bind(DataBindingBuilder
                .<Component>componentS2C(() -> Component.literal(
                        String.valueOf(playerData(serverPlayerOf(pointValue)).insightPoints())))
                .build());
    }

    private static float aptRatio(ServerPlayer player) {
        PlayerEpiphanyData d = playerData(player);
        long cap = AptitudeFormula.calcRequiredAptitude(d.totalInsightPointsSpent(), d.insightPoints());
        if (cap <= 0) return 0f;
        double v = d.aptitude() / (double) cap;
        return (float) Math.max(0, Math.min(1, v));
    }

    private static Component aptText(ServerPlayer player) {
        PlayerEpiphanyData d = playerData(player);
        long cap = AptitudeFormula.calcRequiredAptitude(d.totalInsightPointsSpent(), d.insightPoints());
        return Component.translatable("epiphany.ui.apt_ratio", d.aptitude(), cap);
    }

    /**
     * Only call inside DataBindingBuilder S→C getters, where LDLib2 runs on the server
     * and {@code ModularUI#player} is guaranteed to be a ServerPlayer.
     */
    private static ServerPlayer serverPlayerOf(UIElement element) {
        return (ServerPlayer) element.getModularUI().player;
    }

    private static PlayerEpiphanyData playerData(ServerPlayer player) {
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
    }

    private static <T extends UIElement> T selectOne(UI ui, String selector, Class<T> type) {
        Optional<T> first = ui.select(selector)
                .findFirst()
                .filter(type::isInstance)
                .map(type::cast);
        return first.orElseThrow(() -> new IllegalStateException(
                "Epiphany main UI XML is missing selector '" + selector
                        + "' of type " + type.getSimpleName()));
    }
}
