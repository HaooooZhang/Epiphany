package ink.myumoon.epiphany.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-insight player state stored in the Epiphany attachment.
 * <p>
 * {@code moduleSelected} is a redundant copy of the parent module's select state,
 * kept here for convenient UI lookups without traversing the module map.
 */
public record InsightPlayerState(
        boolean selected,
        boolean moduleSelected
) {
    public static final Codec<InsightPlayerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("selected").forGetter(InsightPlayerState::selected),
            Codec.BOOL.fieldOf("moduleSelected").forGetter(InsightPlayerState::moduleSelected)
    ).apply(instance, InsightPlayerState::new));

    // create a default state
    public static InsightPlayerState createDefault() {
        return new InsightPlayerState(false, false);
    }
}
