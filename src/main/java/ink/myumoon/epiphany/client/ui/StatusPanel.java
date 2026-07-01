package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import ink.myumoon.epiphany.api.AptitudeFormula;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Status tab content: a read-only summary panel of the player's Epiphany
 * progress (aptitude, insight points, spent, slots). All values are read
 * from the synced Attachment mirror on the client.
 */
public final class StatusPanel {

    private StatusPanel() {
    }

    public static UIElement create() {
        var root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .widthPercent(100)
                .gapAll(4)
                .paddingAll(8));

        root.addChild(statLabel(StatusPanel::aptitudeLine));
        root.addChild(statLabel(StatusPanel::totalSpentLine));
        root.addChild(statLabel(StatusPanel::insightPointsLine));
        root.addChild(statLabel(StatusPanel::nextLevelLine));
        root.addChild(statLabel(StatusPanel::epiphanySlotsLine));

        return root;
    }

    private static Label statLabel(java.util.function.Supplier<Component> supplier) {
        var label = new Label();
        label.bindDataSource(SupplierDataSource.of(supplier));
        return label;
    }

    // ============================================================
    // Line builders — read client mirror only
    // ============================================================

    private static Component aptitudeLine() {
        PlayerEpiphanyData d = clientData();
        long cap = AptitudeFormula.calcRequiredAptitude(
                d != null ? d.totalInsightPointsSpent() : 0,
                d != null ? d.insightPoints() : 0);
        return key("epiphany.ui.status.aptitude")
                .append(Component.literal(": " + (d != null ? d.aptitude() : 0) + " / " + cap));
    }

    private static Component totalSpentLine() {
        PlayerEpiphanyData d = clientData();
        return key("epiphany.ui.status.spent")
                .append(Component.literal(": " + (d != null ? d.totalInsightPointsSpent() : 0)));
    }

    private static Component insightPointsLine() {
        PlayerEpiphanyData d = clientData();
        return key("epiphany.ui.status.points")
                .append(Component.literal(": " + (d != null ? d.insightPoints() : 0)));
    }

    private static Component nextLevelLine() {
        PlayerEpiphanyData d = clientData();
        if (d == null) return Component.empty();
        long cap = AptitudeFormula.calcRequiredAptitude(
                d.totalInsightPointsSpent(), d.insightPoints());
        long remaining = Math.max(0, cap - d.aptitude());
        return key("epiphany.ui.status.next_level")
                .append(Component.literal(": " + remaining));
    }

    private static Component epiphanySlotsLine() {
        PlayerEpiphanyData d = clientData();
        int used = d != null ? d.usedEpiphanySlots() : 0;
        int total = d != null ? d.epiphanySlots() : 0;
        return key("epiphany.ui.status.slots")
                .append(Component.literal(": " + used + " / " + total));
    }

    /** Shorthand for a translated label segment, mutable so callers can append. */
    private static MutableComponent key(String translationKey) {
        return Component.translatable(translationKey);
    }

    private static PlayerEpiphanyData clientData() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
    }
}
