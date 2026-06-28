package ink.myumoon.epiphany.content;

import com.mojang.serialization.Codec;

/**
 * Defines the initial visibility/selectability of a Module or Epiphany.
 */
public enum InitialState {
    /**
     * Initially hidden. If a condition is present, becomes selectable when condition is met.
     * If no condition is present, can only be unlocked via API/commands (developer-triggered flow).
     */
    LOCKED,
    /**
     * Immediately visible and selectable (typically paired with no condition).
     */
    SELECTABLE;

    public static final Codec<InitialState> CODEC = Codec.STRING.xmap(
            s -> "selectable".equals(s) ? SELECTABLE : LOCKED,
            state -> state == SELECTABLE ? "selectable" : "locked"
    );
}
    