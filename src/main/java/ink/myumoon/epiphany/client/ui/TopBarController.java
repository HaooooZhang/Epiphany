package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import ink.myumoon.epiphany.api.AptitudeFormula;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

/** Binds the top bar to the client's synced player attachment. */
@OnlyIn(Dist.CLIENT)
public final class TopBarController {

    private TopBarController() {
    }

    public static void attach(UI ui) {
        ProgressBar aptBar = selectOne(ui, "#apt-bar", ProgressBar.class);
        Label pointValue = selectOne(ui, "#point-value", Label.class);

        aptBar.label(l -> l.textStyle(t -> t.fontSize(9).textColor(0xFFFFFFFF).textShadow(true)));
        pointValue.textStyle(t -> t.fontSize(16).textColor(0xFFFFFFFF).textShadow(true));

        aptBar.bindDataSource(SupplierDataSource.of(TopBarController::aptitudeRatio));
        aptBar.label.bindDataSource(SupplierDataSource.of(TopBarController::aptitudeText));
        pointValue.bindDataSource(SupplierDataSource.of(TopBarController::insightPointsText));
    }

    private static float aptitudeRatio() {
        PlayerEpiphanyData data = ClientData.clientData();
        if (data == null) return 0f;
        long cap = AptitudeFormula.calcRequiredAptitude(
                data.totalInsightPointsSpent(), data.insightPoints());
        if (cap <= 0) return 0f;
        double ratio = data.aptitude() / (double) cap;
        return (float) Math.max(0, Math.min(1, ratio));
    }

    private static Component aptitudeText() {
        PlayerEpiphanyData data = ClientData.clientData();
        if (data == null) return Component.translatable("epiphany.ui.apt_ratio", 0, 0);
        long cap = AptitudeFormula.calcRequiredAptitude(
                data.totalInsightPointsSpent(), data.insightPoints());
        return Component.translatable("epiphany.ui.apt_ratio", data.aptitude(), cap);
    }

    private static Component insightPointsText() {
        PlayerEpiphanyData data = ClientData.clientData();
        return Component.literal(String.valueOf(data != null ? data.insightPoints() : 0));
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
