package ink.myumoon.epiphany.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when the aptitude bar fills and the player earns a new Insight Point.
 */
public class AptitudeLevelUpEvent extends EpiphanyEvent {

    private final long newTotalSpent;

    public AptitudeLevelUpEvent(ServerPlayer player, long newTotalSpent) {
        super(player);
        this.newTotalSpent = newTotalSpent;
    }

    /** Total Insight Points spent (after the level-up). */
    public long getNewTotalSpent() { return newTotalSpent; }
}
