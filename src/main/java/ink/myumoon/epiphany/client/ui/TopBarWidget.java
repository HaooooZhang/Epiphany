package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle;
import dev.vfyjxf.taffy.style.FlexDirection;
import ink.myumoon.epiphany.api.AptitudeFormula;
import ink.myumoon.epiphany.api.AptitudeManager;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Top bar of the Epiphany UI: title + Aptitude progress bar + Insight Point badge.
 * <p>
 * All values are read from the client-side {@link PlayerEpiphanyData} mirror,
 * which is kept up to date automatically by the Attachment sync channel
 * (NeoForge {@code AttachmentType.sync(STREAM_CODEC)}). No LDLib2
 * {@code DataBindingBuilder} is used — the SupplierDataSource just re-reads
 * the mirrored attachment every tick.
 */
public final class TopBarWidget {

    private TopBarWidget() {
    }

    public static UIElement create() {
        var bar = new UIElement().layout(TopBarWidget::rowLayout);

        // 1. Title (left-aligned, no flex grow)
        bar.addChild(new Label().setText(Component.translatable("epiphany.ui.title")));

        // 2. Aptitude progress bar (fills available horizontal space)
        bar.addChild(buildAptitudeBar());

        // 3. Insight Points badge (right-aligned, fixed width)
        bar.addChild(buildInsightPointsBadge());

        return bar;
    }

    /** Aptitude progress bar: bound to aptitude / cap. Cap is dynamic and
     * equals {@code baseAptitudeCap + totalInsightPointsSpent * aptitudeCapGrowth}. */
    private static ProgressBar buildAptitudeBar() {
        var progressBar = new ProgressBar();
        progressBar.layout(l -> l.flexGrow(1).height(20).minWidth(120));

        // Phase 6: hover tooltip shows current formula + breakdown.
        ink.myumoon.epiphany.client.ui.tooltips.TooltipBuilder.attach(progressBar, () -> {
            var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            lines.add(net.minecraft.network.chat.Component.translatable("epiphany.ui.aptitude")
                    .copy().withStyle(net.minecraft.ChatFormatting.GOLD));
            PlayerEpiphanyData d = clientData();
            if (d == null) {
                lines.add(net.minecraft.network.chat.Component.translatable("epiphany.ui.no_data"));
                return lines;
            }
            long cap = ink.myumoon.epiphany.api.AptitudeFormula.calcRequiredAptitude(
                    d.totalInsightPointsSpent(), d.insightPoints());
            long remaining = Math.max(0, cap - d.aptitude());
            // Breakdown: current / cap (left to next point)
            lines.add(net.minecraft.network.chat.Component.literal(d.aptitude() + " / " + cap)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(net.minecraft.network.chat.Component.translatable("epiphany.ui.aptitude.remaining",
                    remaining).withStyle(net.minecraft.ChatFormatting.GRAY));
            // Formula line: baseAptitudeCap + totalSpent × aptitudeCapGrowth (only when Shift held).
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                lines.add(net.minecraft.network.chat.Component.empty());
                lines.add(net.minecraft.network.chat.Component.literal(
                        ink.myumoon.epiphany.Config.BASE_APTITUDE_CAP.get() + " + "
                                + d.totalInsightPointsSpent() + " × "
                                + ink.myumoon.epiphany.Config.APTITUDE_CAP_GROWTH.get()
                                + " = " + cap)
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
            } else {
                lines.add(net.minecraft.network.chat.Component.translatable("epiphany.ui.shift_hint")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                        .withStyle(net.minecraft.ChatFormatting.ITALIC));
            }
            return lines;
        });
        // Range is dynamic, so we use normalized 0..1 via SupplierDataSource<Float>.
        progressBar.bindDataSource(SupplierDataSource.of(TopBarWidget::aptitudeRatio));
        // Reflect current/cap as the bar's overlay label.
        progressBar.label(label -> label
                .bindDataSource(SupplierDataSource.of(TopBarWidget::aptitudeLabel)));
        return progressBar;
    }

    /** Insight Points badge: shows the current number as "N <<points>>". */
    private static UIElement buildInsightPointsBadge() {
        // Construct first, so the Label-typed methods are not lost by the
        // UIElement-typed return of layout(...).
        var badge = new Label();
        badge.bindDataSource(SupplierDataSource.of(TopBarWidget::insightPointsLabel));
        badge.layout(l -> l.width(80).height(20));
        return badge;
    }

    // ============================================================
    // Layout helpers
    // ============================================================

    private static void rowLayout(LayoutStyle l) {
        l.flexDirection(FlexDirection.ROW)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER)
                .widthPercent(100)
                .gapAll(8)
                .paddingAll(6);
    }

    // ============================================================
    // Data suppliers — pure client reads of the synced Attachment
    // ============================================================

    /** Returns aptitude/cap in [0, 1] for ProgressBar fill. */
    private static float aptitudeRatio() {
        PlayerEpiphanyData data = clientData();
        if (data == null) return 0f;
        long cap = AptitudeFormula.calcRequiredAptitude(
                data.totalInsightPointsSpent(), data.insightPoints());
        return cap <= 0 ? 0f : (float) data.aptitude() / cap;
    }

    /** Returns the textual "current / cap" label for the bar overlay. */
    private static Component aptitudeLabel() {
        PlayerEpiphanyData data = clientData();
        if (data == null) return Component.empty();
        long cap = AptitudeFormula.calcRequiredAptitude(
                data.totalInsightPointsSpent(), data.insightPoints());
        return Component.literal(data.aptitude() + " / " + cap)
                .append(" ").append(Component.translatable("epiphany.ui.aptitude"));
    }

    /** Returns "N points" form for the badge. */
    private static Component insightPointsLabel() {
        PlayerEpiphanyData data = clientData();
        if (data == null) return Component.empty();
        return Component.literal(String.valueOf(data.insightPoints()))
                .append(" ").append(Component.translatable("epiphany.ui.point"));
    }

    /** Safely fetch the client-side Player data, or null if not available. */
    private static PlayerEpiphanyData clientData() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        // AptitudeManager.getInsightPoints/... are server-side; on the client we
        // read the mirror directly via the AttachmentType holder.
        // Note: `getData` on the client returns the synced snapshot.
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
    }
}
