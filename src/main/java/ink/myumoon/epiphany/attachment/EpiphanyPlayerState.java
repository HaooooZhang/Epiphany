package ink.myumoon.epiphany.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-epiphany player state stored in the Epiphany attachment.
 */
public record EpiphanyPlayerState(
        boolean selected,
        boolean unlocked
) {
    public static final Codec<EpiphanyPlayerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("selected").forGetter(EpiphanyPlayerState::selected),
            Codec.BOOL.fieldOf("unlocked").forGetter(EpiphanyPlayerState::unlocked)
    ).apply(instance, EpiphanyPlayerState::new));

    // create a default state
    public static EpiphanyPlayerState createDefault() {
        return new EpiphanyPlayerState(false, false);
    }
}
