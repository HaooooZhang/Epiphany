package ink.myumoon.epiphany.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after a player's available Insight Points balance changes.
 * <p>
 * Triggered once per Manager call (settlement-level), regardless of how many
 * level-ups occurred inside that call — so listeners don't get spammed when a
 * single {@code addAptitude} fills the bar multiple times.
 * <p>
 * Post (notification-only) event. Not cancellable.
 */
public class InsightPointsChangedEvent extends EpiphanyEvent {

    private final int oldValue;
    private final int newValue;

    public InsightPointsChangedEvent(ServerPlayer player, int oldValue, int newValue) {
        super(player);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public int getOldValue() { return oldValue; }

    public int getNewValue() { return newValue; }

    /** Net change of available Insight Points. Positive = gained, negative = spent. */
    public int getDelta() { return newValue - oldValue; }

    public boolean isGain() { return newValue > oldValue; }
}
