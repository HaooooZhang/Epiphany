package ink.myumoon.epiphany.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * Per-module player state stored in the Epiphany attachment.
 * <p>
 * The {@code unlockedInsights} set is <b>immutable</b> via {@link Set#copyOf}
 * in the compact constructor — any mutation attempt throws
 * {@link UnsupportedOperationException}.
 * Managers must create a new {@code ModulePlayerState} record to change state.
 */
public record ModulePlayerState(
        boolean unlocked,
        boolean selected,
        boolean completed,
        Set<ResourceLocation> unlockedInsights
) {
    // Compact constructor: defensive immutable copy.
    public ModulePlayerState {
        unlockedInsights = Set.copyOf(unlockedInsights);
    }

    public static final Codec<ModulePlayerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("unlocked").forGetter(ModulePlayerState::unlocked),
            Codec.BOOL.fieldOf("selected").forGetter(ModulePlayerState::selected),
            Codec.BOOL.fieldOf("completed").forGetter(ModulePlayerState::completed),
            ResourceLocation.CODEC.listOf()
                    .xmap(Set::copyOf, List::copyOf)
                    .fieldOf("unlockedInsights")
                    .forGetter(ModulePlayerState::unlockedInsights)
    ).apply(instance, ModulePlayerState::new));

    // create a default state
    public static ModulePlayerState createDefault() {
        return new ModulePlayerState(false, false, false, Set.of());
    }
}
