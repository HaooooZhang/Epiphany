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
 * Full player data for the Epiphany build system, persisted via attachment.
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

    // default factory
    public static PlayerEpiphanyData createDefault() {
        return new PlayerEpiphanyData(
                0, 0, 0,
                new HashMap<>(), new HashMap<>(), new HashMap<>(),
                0, 0
        );
    }


    // codec
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

    // network sync
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEpiphanyData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    // Immutable handle
    public PlayerEpiphanyData withAptitude(long v) {
        return new PlayerEpiphanyData(v, insightPoints, totalInsightPointsSpent,
                modules, insights, epiphanies, epiphanySlots, usedEpiphanySlots);
    }

    public PlayerEpiphanyData withInsightPoints(int v) {
        return new PlayerEpiphanyData(aptitude, v, totalInsightPointsSpent,
                modules, insights, epiphanies, epiphanySlots, usedEpiphanySlots);
    }

    public PlayerEpiphanyData withTotalInsightPointsSpent(int v) {
        return new PlayerEpiphanyData(aptitude, insightPoints, v,
                modules, insights, epiphanies, epiphanySlots, usedEpiphanySlots);
    }

    public PlayerEpiphanyData withEpiphanySlots(int v) {
        return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                modules, insights, epiphanies, v, usedEpiphanySlots);
    }

    public PlayerEpiphanyData withUsedEpiphanySlots(int v) {
        return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                modules, insights, epiphanies, epiphanySlots, v);
    }

    public PlayerEpiphanyData withModuleState(ResourceLocation id, ModulePlayerState state) {
        var copy = new HashMap<>(modules);
        copy.put(id, state);
        return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                copy, insights, epiphanies, epiphanySlots, usedEpiphanySlots);
    }

    public PlayerEpiphanyData withInsightState(ResourceLocation id, InsightPlayerState state) {
        var copy = new HashMap<>(insights);
        copy.put(id, state);
        return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                modules, copy, epiphanies, epiphanySlots, usedEpiphanySlots);
    }

    public PlayerEpiphanyData withEpiphanyState(ResourceLocation id, EpiphanyPlayerState state) {
        var copy = new HashMap<>(epiphanies);
        copy.put(id, state);
        return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                modules, insights, copy, epiphanySlots, usedEpiphanySlots);
    }
}
