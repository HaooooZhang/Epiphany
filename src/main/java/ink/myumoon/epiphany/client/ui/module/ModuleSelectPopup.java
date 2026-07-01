package ink.myumoon.epiphany.client.ui.module;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.client.ui.ClientData;
import ink.myumoon.epiphany.client.ui.overlay.Overlay;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Popup for selecting a new Module into an empty slot.
 * <p>
 * Phase 5 MVP layout: title + show-locked Switch + scrollable module list.
 * Each module row: name + "Select" button. Selection calls
 * {@code ModuleManager.select(player, moduleId)} via Button.setOnServerClick.
 * <p>
 * Locked modules (initialState=LOCKED) are hidden by default; the Switch
 * reveals them (greyed). Client-side we don't evaluate conditions —
 * server-side validation remains the safety net for selection success.
 */
public final class ModuleSelectPopup {

    /** Filter state for the popup. */
    public static final class Filter {
        public boolean showLocked;
        public static Filter initial() {
            Filter f = new Filter();
            f.showLocked = false;
            return f;
        }
    }

    /** A candidate module paired with its registry id. */
    private record Candidate(ResourceLocation id, ModuleData data) {}

    private ModuleSelectPopup() {
    }

    /**
     * Build the popup. Returns the Overlay — caller adds {@code overlay.wrapper()}
     * to its root and calls {@code overlay.show()} when opening.
     */
    public static Overlay create() {
        // Build the panel first.
        var panel = new UIElement();
        panel.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(380).height(280)
                .paddingAll(8).gapAll(4));
        panel.style(s -> s.background(
                com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites.BORDER));

        Filter filter = Filter.initial();

        // 2. Scrollable body container — declared BEFORE the switch lambda,
        //    so the lambda (setOnSwitchChanged) can capture it as effectively-final.
        var scroller = new ScrollerView();
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL));
        scroller.layout(l -> l.flexGrow(1).widthPercent(100));

        var body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP)
                .gapAll(4));
        scroller.addScrollViewChild(body);

        // 1. Title + filter switch
        var header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW)
                .widthPercent(100).height(20)
                .marginBottom(4));
        header.addChild(new Label().setText(
                Component.translatable("epiphany.ui.module.select_title")));

        var showLockedLabel = new Label().setText(
                Component.translatable("epiphany.ui.module.show_locked"));
        header.addChild(showLockedLabel);

        var showLockedSwitch = new Switch();
        showLockedSwitch.setOnSwitchChanged(b -> {
            filter.showLocked = b;
            populateBody(body, filter);
        });
        header.addChild(showLockedSwitch);
        panel.addChild(header);
        panel.addChild(scroller);

        // Initial populate (client dist guard inside).
        populateBody(body, filter);

        return Overlay.of(panel);
    }

    /** Rebuild the body's children list, applying the given filter. */
    private static void populateBody(UIElement body, Filter filter) {
        removeAllChildren(body);
        if (net.neoforged.fml.loading.FMLLoader.getDist() != net.neoforged.api.distmarker.Dist.CLIENT) {
            return;
        }

        HolderLookup.RegistryLookup<ModuleData> lookup = ClientData.moduleLookup();
        PlayerEpiphanyData d = ClientData.clientData();
        if (lookup == null || d == null) return;

        // Gather candidates (id + data) so the Select button can call
        // ModuleManager.select with the right ResourceLocation.
        List<Candidate> candidates = new ArrayList<>();
        lookup.listElements().forEach(holder -> {
            ModuleData m = holder.value();
            boolean isSelectable = m.initialState() == InitialState.SELECTABLE;
            if (isSelectable || filter.showLocked) {
                candidates.add(new Candidate(holder.key().location(), m));
            }
        });
        candidates.sort(Comparator
                .<Candidate>comparingInt(c -> c.data().weight())
                .thenComparing(c -> c.id().toString()));

        for (Candidate c : candidates) {
            body.addChild(buildModuleRow(c.id(), c.data(), filter.showLocked));
        }
    }

    /** Remove all children of an element. Helper since UIElement lacks clearChildren(). */
    private static void removeAllChildren(UIElement element) {
        // Use a defensive copy of the children list, then remove each.
        // Note: actual remove API on UIElement may be removeChild(UIElement)
        // or removeAllListeners — we use the simplest approach.
        for (var child : new ArrayList<>(element.getChildren())) {
            element.removeChild(child);
        }
    }

    private static UIElement buildModuleRow(ResourceLocation moduleId, ModuleData m, boolean filterLocked) {
        var row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW)
                .widthPercent(100).height(40)
                .gapAll(6).paddingAll(4));

        Component name = m.name().isPresent()
                ? m.name().get()
                : Component.translatable("epiphany.ui.module.unnamed");
        row.addChild(new Label().setText(name));

        // Spacer that grows to push the button right.
        var spacer = new UIElement();
        spacer.layout(l -> l.flex(1));
        row.addChild(spacer);

        boolean isLocked = m.initialState() == InitialState.LOCKED;
        var selectBtn = new Button();
        selectBtn.setText(Component.translatable(
                isLocked ? "epiphany.ui.module.locked_button" : "epiphany.ui.module.select_button"));
        if (!filterLocked && isLocked) {
            // Don't attach server click handler if locked and not showing locked visually.
        } else {
            selectBtn.setOnServerClick(event -> {
                Player p = event.currentElement.getModularUI().player;
                if (p instanceof ServerPlayer sp) {
                    ink.myumoon.epiphany.api.ModuleManager.select(sp, moduleId);
                }
            });
        }
        row.addChild(selectBtn);
        return row;
    }
}
