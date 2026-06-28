package ink.myumoon.epiphany.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Full player data for the Epiphany skill-tree system, persisted via
 * NeoForge {@link net.neoforged.neoforge.attachment.AttachmentType Attachment}.
 * <p>
 * Contains aptitude, insight points, per-module/insight/epiphany state,
 * and epiphany slot counts.
 */
public record PlayerEpiphanyData(
        long aptitude,
        int insightPoints,
        int totalInsightPointsSpent,
        Map<ResourceLocation, ModulePlayerState> modules,
        Map<ResourceLocation, InsightPlayerState> insights,
        Map<ResourceLocation, EpiphanyPlayerState> epiphanies,
        int epiphanySlots,
        int usedEpiphanySlots
) {

    // ============================================================
    // Default factory
    // ============================================================

    /** Creates a fresh default instance with all values at zero and empty maps. */
    public static PlayerEpiphanyData createDefault() {
        return new PlayerEpiphanyData(
                0, 0, 0,
                new HashMap<>(), new HashMap<>(), new HashMap<>(),
                0, 0
        );
    }

    // ============================================================
    // Codec — NBT serialization via AttachmentType.Builder#serialize
    // ============================================================

    public static final Codec<PlayerEpiphanyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("aptitude").forGetter(PlayerEpiphanyData::aptitude),
            Codec.INT.fieldOf("insightPoints").forGetter(PlayerEpiphanyData::insightPoints),
            Codec.INT.fieldOf("totalInsightPointsSpent").forGetter(PlayerEpiphanyData::totalInsightPointsSpent),
            Codec.unboundedMap(ResourceLocation.CODEC, ModulePlayerState.CODEC)
                    .fieldOf("modules").forGetter(PlayerEpiphanyData::modules),
            Codec.unboundedMap(ResourceLocation.CODEC, InsightPlayerState.CODEC)
                    .fieldOf("insights").forGetter(PlayerEpiphanyData::insights),
            Codec.unboundedMap(ResourceLocation.CODEC, EpiphanyPlayerState.CODEC)
                    .fieldOf("epiphanies").forGetter(PlayerEpiphanyData::epiphanies),
            Codec.INT.fieldOf("epiphanySlots").forGetter(PlayerEpiphanyData::epiphanySlots),
            Codec.INT.fieldOf("usedEpiphanySlots").forGetter(PlayerEpiphanyData::usedEpiphanySlots)
    ).apply(instance, PlayerEpiphanyData::new));

    // ============================================================
    // StreamCodec — network sync via AttachmentType.Builder#sync
    // ============================================================

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEpiphanyData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
