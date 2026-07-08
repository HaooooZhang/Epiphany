package ink.myumoon.epiphany.event.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * KubeJS event for {@link ink.myumoon.epiphany.event.AptitudeChangedEvent} (Post, notification).
 */
public class AptitudeChangedKubeEvent implements KubeEvent {

    private final ServerPlayer player;
    private final long oldAptitude;
    private final long newAptitude;

    public AptitudeChangedKubeEvent(ServerPlayer player, long oldAptitude, long newAptitude) {
        this.player = player;
        this.oldAptitude = oldAptitude;
        this.newAptitude = newAptitude;
    }

    public ServerPlayer getPlayer() { return player; }
    public long getOldAptitude() { return oldAptitude; }
    public long getNewAptitude() { return newAptitude; }
}
