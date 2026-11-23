package ink.myumoon.epiphany.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when the aptitude bar fills and the player earns a new Insight Point.
 */
public class AptitudeLevelUpEvent extends EpiphanyEvent {

    private final int newInsightPoints;

    public AptitudeLevelUpEvent(ServerPlayer player, int newInsightPoints) {
        super(player);
        this.newInsightPoints = newInsightPoints;
    }

    /** Total available Insight Points (after the level-up). */
    public int getNewInsightPoints() { return newInsightPoints; }
}
