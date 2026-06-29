package ink.myumoon.epiphany.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

import java.util.Objects;

/**
 * Base class for all Epiphany events.
 * Every event carries a non-null {@link ServerPlayer} reference.
 */
public abstract class EpiphanyEvent extends Event {

    private final ServerPlayer player;

    protected EpiphanyEvent(ServerPlayer player) {
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
