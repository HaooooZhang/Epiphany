package ink.myumoon.epiphany.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Per-module player state stored in the Epiphany attachment.
 */
public record ModulePlayerState(
        boolean unlocked,
        boolean selected,
        boolean completed,
        HashSet<ResourceLocation> unlockedInsights
) {
    public static final Codec<ModulePlayerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("unlocked").forGetter(ModulePlayerState::unlocked),
            Codec.BOOL.fieldOf("selected").forGetter(ModulePlayerState::selected),
            Codec.BOOL.fieldOf("completed").forGetter(ModulePlayerState::completed),
            ResourceLocation.CODEC.listOf()
                    .xmap(HashSet::new, ArrayList::new)
                    .fieldOf("unlockedInsights")
                    .forGetter(ModulePlayerState::unlockedInsights)
    ).apply(instance, ModulePlayerState::new));

    /** Creates a default state — nothing unlocked, nothing selected. */
    public static ModulePlayerState createDefault() {
        return new ModulePlayerState(false, false, false, new HashSet<>());
    }
}
