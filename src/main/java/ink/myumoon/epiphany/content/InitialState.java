package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;

/**
 * Defines the initial visibility/selectability of a Module or Epiphany.
 */
public enum InitialState {
    LOCKED,
    SELECTABLE;

    public static final Codec<InitialState> CODEC = Codec.STRING.xmap(
            s -> "selectable".equals(s) ? SELECTABLE : LOCKED,
            state -> state == SELECTABLE ? "selectable" : "locked"
    );
}
    