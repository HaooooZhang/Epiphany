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
 * Wires the top bar of the Epiphany main UI to dynamic data.
 * <p>
 * Binds three things via {@link DataBindingBuilder} S→C (server executes the
 * getter lambda, LDLib2 syncs the result to the client):
 * <ul>
 *   <li>{@code #apt-bar} ProgressBar — 0..1 fill ratio</li>
 *   <li>{@code #apt-label} Label — {@code "current / cap"} literal text</li>
 *   <li>{@code #point-value} Label — insight points count</li>
 * </ul>
 * <p>
 *
 * <b>Why we read ServerPlayer via {@code el.getModularUI().player}</b>:
 * The Menu-based UI constructs one ModularUI on each side (server with
 * ServerPlayer, client with LocalPlayer). The S→C getter from
 * DataBindingBuilder is only ever evaluated on the server side — so the cast
 * to ServerPlayer is safe inside the getter lambda.
 */
public final class TopBarController {

    private TopBarController() {
    }

    /**
     * Bind all top-bar elements. Called once per side when the UI tree is built
     * in {@link EpiphanyUIFactory#register()}.
     * <p>
     * Phase A binds three readable values (Float/Component on ProgressBar/Label —
     * both implement {@code IDataConsumer} so the synced value is reflected in
     * the UI). The {@code .__point_available__} highlight on {@code #point-badge}
     * is deferred to Phase B: UIElement's {@code setValue} is a no-op, so we'll
     * instead attach a {@code UIEvents.TICK} listener there and call
     * {@code pointBadge.addClass(...)} from the client side.
     */
    public static void bind(UI ui) {
        ProgressBar aptBar = selectOne(ui, "#apt-bar", ProgressBar.class);
        Label pointValue = selectOne(ui, "#point-value", Label.class);

        // The built-in ProgressBar label needs explicit styling — small font + white.
        aptBar.label(l -> l.textStyle(t -> t.fontSize(9).textColor(0xFFFFFFFF).textShadow(true)));

        // LSS TextStyle props (font-size etc.) don't reliably cascade to Label's
        // internal TextStyle, so set fontSize explicitly here in Java.
        pointValue.textStyle(t -> t.fontSize(16).textColor(0xFFFFFFFF).textShadow(true));

        // 1) Progress bar fill: 0..1. ProgressBar consumes Float natively.
        aptBar.bind(DataBindingBuilder
                .<Float>floatValS2C(() -> aptRatio(serverPlayerOf(aptBar)))
                .build());

        // 2) ProgressBar's built-in label text: "[current] / [cap]" as a translatable.
        // ProgressBar.label is a Label field; we can DataBindingBuilder.bind to it
        // directly because Label implements IBindable<Component>.
        Label barLabel = aptBar.label;
        barLabel.bind(DataBindingBuilder
                .<Component>componentS2C(() -> aptText(serverPlayerOf(aptBar)))
                .build());

        // 3) Insight points number (big, centered in #point-badge).
        pointValue.bind(DataBindingBuilder
                .<Component>componentS2C(() -> Component.literal(
                        String.valueOf(playerData(serverPlayerOf(pointValue)).insightPoints())))
                .build());
    }

    // ============================================================
    // Read helpers
    // ============================================================

    /** Aptitude → cap fill ratio clamped to [0, 1]. */
    private static float aptRatio(ServerPlayer player) {
        PlayerEpiphanyData d = playerData(player);
        long cap = AptitudeFormula.calcRequiredAptitude(d.totalInsightPointsSpent(), d.insightPoints());
        if (cap <= 0) return 0f;
        double v = d.aptitude() / (double) cap;
        return (float) Math.max(0, Math.min(1, v));
    }

    /** "current / cap" translatable string for the apt ratio label (i18n). */
    private static Component aptText(ServerPlayer player) {
        PlayerEpiphanyData d = playerData(player);
        long cap = AptitudeFormula.calcRequiredAptitude(d.totalInsightPointsSpent(), d.insightPoints());
        return Component.translatable("epiphany.ui.apt_ratio", d.aptitude(), cap);
    }

    /**
     * Resolve the {@link ServerPlayer} holding the UI element.
     * <p>
     * Safe because: this method is only ever called inside a DataBindingBuilder
     * {@code S→C} getter, which LDLib2 evaluates exclusively on the server side
     * (the client-side {@link com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding}
     * does not invoke the getter). On the server, {@link
     * com.lowdragmc.lowdraglib2.gui.ui.ModularUI#player} is the ServerPlayer.
     */
    private static ServerPlayer serverPlayerOf(UIElement element) {
        return (ServerPlayer) element.getModularUI().player;
    }

    /** Convenience: read the Epiphany attachment from a ServerPlayer. */
    private static PlayerEpiphanyData playerData(ServerPlayer player) {
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
    }

    /** Convenience: select exactly one element by CSS selector or throw a clear error. */
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
