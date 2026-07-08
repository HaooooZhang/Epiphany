package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.InsightPointsChangedEvent} (Post, notification).
 */
public class InsightPointsChangedKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final int oldValue;
    private final int newValue;

    public InsightPointsChangedKubeEvent(ServerPlayer player, int oldValue, int newValue) {
        this.player = player;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public ServerPlayer getPlayer() { return player; }
    public int getOldValue() { return oldValue; }
    public int getNewValue() { return newValue; }
    public int getDelta() { return newValue - oldValue; }
    public boolean isGain() { return newValue > oldValue; }
}
