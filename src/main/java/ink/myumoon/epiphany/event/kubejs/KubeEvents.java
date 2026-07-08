package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * All Epiphany KubeJS event definitions.
 * <p>
 * EventGroup name "Epiphany" is auto-exposed as {@code EpiphanyEvents} in JS scripts.
 * <p>
 * Naming convention:
 * <ul>
 *   <li>Past-tense (e.g. "moduleUnlocked") — Post notification, not cancellable</li>
 *   <li>Present-tense (e.g. "moduleUnlock") — Pre event, {@code .hasResult()} → cancellable via {@code event.cancel()}</li>
 * </ul>
 */
public interface KubeEvents {

    EventGroup GROUP = EventGroup.of("Epiphany");

    // ─── Module events ──────────────────────────────────────────────

    /** Fired before a Module transitions from locked to unlocked. Cancellable. */
    EventHandler MODULE_UNLOCK = GROUP.server("moduleUnlock", () -> ModuleUnlockKubeEvent.class).hasResult();

    /** Fired after a Module has been unlocked. */
    EventHandler MODULE_UNLOCKED = GROUP.server("moduleUnlocked", () -> ModuleUnlockedKubeEvent.class);

    /** Fired before a Module is selected (Insight Points about to be deducted). Cancellable. */
    EventHandler MODULE_SELECT = GROUP.server("moduleSelect", () -> ModuleSelectKubeEvent.class).hasResult();

    /** Fired after a Module has been selected. */
    EventHandler MODULE_SELECTED = GROUP.server("moduleSelected", () -> ModuleSelectedKubeEvent.class);

    /** Fired before a Module is completed (all Insights unlocked). Cancellable. */
    EventHandler MODULE_COMPLETE = GROUP.server("moduleComplete", () -> ModuleCompleteKubeEvent.class).hasResult();

    /** Fired after a Module has been completed. */
    EventHandler MODULE_COMPLETED = GROUP.server("moduleCompleted", () -> ModuleCompletedKubeEvent.class);

    // ─── Insight events ─────────────────────────────────────────────

    /** Fired before an Insight is unlocked. Cancellable. */
    EventHandler INSIGHT_SELECT = GROUP.server("insightSelect", () -> InsightSelectKubeEvent.class).hasResult();

    /** Fired after an Insight has been unlocked. */
    EventHandler INSIGHT_SELECTED = GROUP.server("insightSelected", () -> InsightSelectedKubeEvent.class);

    // ─── Epiphany events ────────────────────────────────────────────

    /** Fired before an Epiphany transitions from locked to unlocked. Cancellable. */
    EventHandler EPIPHANY_UNLOCK = GROUP.server("epiphanyUnlock", () -> EpiphanyUnlockKubeEvent.class).hasResult();

    /** Fired after an Epiphany has been unlocked. */
    EventHandler EPIPHANY_UNLOCKED = GROUP.server("epiphanyUnlocked", () -> EpiphanyUnlockedKubeEvent.class);

    /** Fired before an Epiphany is selected (slotted). Cancellable. */
    EventHandler EPIPHANY_SELECT = GROUP.server("epiphanySelect", () -> EpiphanySelectKubeEvent.class).hasResult();

    /** Fired after an Epiphany has been selected. */
    EventHandler EPIPHANY_SELECTED = GROUP.server("epiphanySelected", () -> EpiphanySelectedKubeEvent.class);

    // ─── Aptitude / Points events ───────────────────────────────────

    /** Fired after a player's aptitude value changes. */
    EventHandler APTITUDE_CHANGED = GROUP.server("aptitudeChanged", () -> AptitudeChangedKubeEvent.class);

    /** Fired after a player's available Insight Points balance changes. */
    EventHandler INSIGHT_POINTS_CHANGED = GROUP.server("insightPointsChanged", () -> InsightPointsChangedKubeEvent.class);
}
