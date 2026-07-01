package ink.myumoon.epiphany.client.ui.tooltips;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Tooltip builders for Epiphany UI elements.
 * <p>
 * Consistent tooltip model:
 * <pre>
 *   Header (switching color: gold)
 *   ──────────────────────────────
 *   description line(s) (gray)
 *
 *   [Shift + hold]
 *     reward description (aqua)
 * </pre>
 * Shift-aware: the reward line only shows when the player holds Shift.
 * When Shift is not held, a hint line "Hold Shift for details" is shown.
 */
public final class TooltipBuilder {

    private TooltipBuilder() {
    }

    /** Attach a hover tooltip to the given element. Reads the supplied lines on every hover. */
    public static void attach(UIElement element, Supplier<List<Component>> linesSupplier) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            List<Component> lines = linesSupplier.get();
            if (lines != null && !lines.isEmpty()) {
                var tooltips = com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips.empty();
                // empty().append takes Component... — varargs work with a 0-arg array too
                tooltips = tooltips.append(lines.toArray(new Component[0]));
                e.hoverTooltips = tooltips;
            }
        });
    }

    /**
     * Build a 2-section tooltip: header + description, plus optional Shift-gated reward.
     */
    public static List<Component> forContent(
            Component header,
            Optional<Component> description,
            Optional<Component> rewardDescription,
            Optional<Component> completionRewardDescription) {
        var lines = new java.util.ArrayList<Component>();

        // Header (gold)
        lines.add(copy(header).withStyle(ChatFormatting.GOLD));

        // Description (gray) — show "(no description)" fallback
        Component desc = description.orElse(Component.translatable("epiphany.ui.no_description"));
        lines.add(copy(desc).withStyle(ChatFormatting.GRAY));

        // Reward section only shown when Shift is held; otherwise hint.
        boolean shift = Screen.hasShiftDown();
        if (shift) {
            boolean anyReward = rewardDescription.isPresent()
                    || completionRewardDescription.isPresent();
            if (anyReward) {
                lines.add(Component.empty());  // blank separator
            }
            rewardDescription.ifPresent(rd -> lines.add(Component.translatable("epiphany.tooltip.reward")
                    .append(": ").append(copy(rd).withStyle(ChatFormatting.AQUA))));
            completionRewardDescription.ifPresent(crd -> lines.add(
                    Component.translatable("epiphany.tooltip.completion_reward")
                            .append(": ").append(copy(crd).withStyle(ChatFormatting.AQUA))));
        } else if (rewardDescription.isPresent() || completionRewardDescription.isPresent()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("epiphany.ui.shift_hint")
                    .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
        }
        return lines;
    }

    /** MutableComponent copy so we can apply ChatFormatting without mutating the source. */
    private static MutableComponent copy(Component c) {
        return c.copy();
    }
}
