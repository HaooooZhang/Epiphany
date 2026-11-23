package ink.myumoon.epiphany.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after a player's aptitude value changes.
 */
public class AptitudeChangedEvent extends EpiphanyEvent {

    private final long oldAptitude;
    private final long newAptitude;

    public AptitudeChangedEvent(ServerPlayer player, long oldAptitude, long newAptitude) {
        super(player);
        this.oldAptitude = oldAptitude;
        this.newAptitude = newAptitude;
    }

    public long getOldAptitude() { return oldAptitude; }
    public long getNewAptitude() { return newAptitude; }
}
