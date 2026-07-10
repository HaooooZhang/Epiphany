package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.AptitudeLevelUpEvent} (Standalone).
 * <p>
 * Fired when the aptitude bar fills and the player earns a new Insight Point.
 */
public class AptitudeLevelUpKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final int newInsightPoints;

    public AptitudeLevelUpKubeEvent(ServerPlayer player, int newInsightPoints) {
        this.player = player;
        this.newInsightPoints = newInsightPoints;
    }

    public ServerPlayer getPlayer() { return player; }

    /** Total available Insight Points (after the level-up). */
    public int getNewInsightPoints() { return newInsightPoints; }
}
