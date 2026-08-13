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
                int usedEpiphanySlots,
                Map<ResourceLocation, Long> claimedFirsts,
                PlayerDisplayData displayData) {

        public PlayerEpiphanyData(
                        long aptitude,
                        int insightPoints,
                        int totalInsightPointsSpent,
                        Map<ResourceLocation, ModulePlayerState> modules,
                        Map<ResourceLocation, InsightPlayerState> insights,
                        Map<ResourceLocation, EpiphanyPlayerState> epiphanies,
                        int epiphanySlots,
                        int usedEpiphanySlots,
                        Map<ResourceLocation, Long> claimedFirsts) {
                this(aptitude, insightPoints, totalInsightPointsSpent, modules, insights, epiphanies,
                                epiphanySlots, usedEpiphanySlots, claimedFirsts, PlayerDisplayData.createDefault());
        }

        // default factory
        public static PlayerEpiphanyData createDefault() {
                return new PlayerEpiphanyData(
                                0, 0, 0,
                                new HashMap<>(), new HashMap<>(), new HashMap<>(),
                                0, 0,
                                new HashMap<>(),
                                PlayerDisplayData.createDefault());
        }

        // codec
        public static final Codec<PlayerEpiphanyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("aptitude").forGetter(PlayerEpiphanyData::aptitude),
                        Codec.INT.fieldOf("insightPoints").forGetter(PlayerEpiphanyData::insightPoints),
                        Codec.INT.fieldOf("totalInsightPointsSpent")
                                        .forGetter(PlayerEpiphanyData::totalInsightPointsSpent),
                        Codec.unboundedMap(ResourceLocation.CODEC, ModulePlayerState.CODEC)
                                        .fieldOf("modules").forGetter(PlayerEpiphanyData::modules),
                        Codec.unboundedMap(ResourceLocation.CODEC, InsightPlayerState.CODEC)
                                        .fieldOf("insights").forGetter(PlayerEpiphanyData::insights),
                        Codec.unboundedMap(ResourceLocation.CODEC, EpiphanyPlayerState.CODEC)
                                        .fieldOf("epiphanies").forGetter(PlayerEpiphanyData::epiphanies),
                        Codec.INT.fieldOf("epiphanySlots").forGetter(PlayerEpiphanyData::epiphanySlots),
                        Codec.INT.fieldOf("usedEpiphanySlots").forGetter(PlayerEpiphanyData::usedEpiphanySlots),
                        Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                                        .optionalFieldOf("claimedFirsts", new HashMap<>())
                                        .forGetter(PlayerEpiphanyData::claimedFirsts),
                        PlayerDisplayData.CODEC
                                        .optionalFieldOf("displayData", PlayerDisplayData.createDefault())
                                        .forGetter(PlayerEpiphanyData::displayData))
                        .apply(instance, PlayerEpiphanyData::new));

        // network sync
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEpiphanyData> STREAM_CODEC = ByteBufCodecs
                        .fromCodecWithRegistries(CODEC);

        // Immutable handle
        public PlayerEpiphanyData withAptitude(long v) {
                return new PlayerEpiphanyData(v, insightPoints, totalInsightPointsSpent,
                                modules, insights, epiphanies, epiphanySlots, usedEpiphanySlots, claimedFirsts,
                                displayData);
        }

        public PlayerEpiphanyData withInsightPoints(int v) {
                return new PlayerEpiphanyData(aptitude, v, totalInsightPointsSpent,
                                modules, insights, epiphanies, epiphanySlots, usedEpiphanySlots, claimedFirsts,
                                displayData);
        }

        public PlayerEpiphanyData withTotalInsightPointsSpent(int v) {
                return new PlayerEpiphanyData(aptitude, insightPoints, v,
                                modules, insights, epiphanies, epiphanySlots, usedEpiphanySlots, claimedFirsts,
                                displayData);
        }

        public PlayerEpiphanyData withEpiphanySlots(int v) {
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                modules, insights, epiphanies, v, usedEpiphanySlots, claimedFirsts, displayData);
        }

        public PlayerEpiphanyData withUsedEpiphanySlots(int v) {
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                modules, insights, epiphanies, epiphanySlots, v, claimedFirsts, displayData);
        }

        public PlayerEpiphanyData withDisplayData(PlayerDisplayData v) {
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                modules, insights, epiphanies, epiphanySlots, usedEpiphanySlots, claimedFirsts, v);
        }

        public PlayerEpiphanyData withModuleState(ResourceLocation id, ModulePlayerState state) {
                var copy = new HashMap<>(modules);
                copy.put(id, state);
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                copy, insights, epiphanies, epiphanySlots, usedEpiphanySlots, claimedFirsts,
                                displayData);
        }

        public PlayerEpiphanyData withInsightState(ResourceLocation id, InsightPlayerState state) {
                var copy = new HashMap<>(insights);
                copy.put(id, state);
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                modules, copy, epiphanies, epiphanySlots, usedEpiphanySlots, claimedFirsts,
                                displayData);
        }

        public PlayerEpiphanyData withEpiphanyState(ResourceLocation id, EpiphanyPlayerState state) {
                var copy = new HashMap<>(epiphanies);
                copy.put(id, state);
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                modules, insights, copy, epiphanySlots, usedEpiphanySlots, claimedFirsts, displayData);
        }

        /**
         * Fully removes a module entry (used by orphan cleanup; not the same as
         * resetting to default).
         */
        public PlayerEpiphanyData withoutModule(ResourceLocation id) {
                var copy = new HashMap<>(modules);
                copy.remove(id);
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                copy, insights, epiphanies, epiphanySlots, usedEpiphanySlots, claimedFirsts,
                                displayData);
        }

        /** Fully removes an insight entry. */
        public PlayerEpiphanyData withoutInsight(ResourceLocation id) {
                var copy = new HashMap<>(insights);
                copy.remove(id);
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                modules, copy, epiphanies, epiphanySlots, usedEpiphanySlots, claimedFirsts,
                                displayData);
        }

        /** Fully removes an epiphany entry. */
        public PlayerEpiphanyData withoutEpiphany(ResourceLocation id) {
                var copy = new HashMap<>(epiphanies);
                copy.remove(id);
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                modules, insights, copy, epiphanySlots, usedEpiphanySlots, claimedFirsts, displayData);
        }

        /** Mark a behavior/target pair as having claimed its {@code first_reward}. */
        public PlayerEpiphanyData withClaimedFirst(ResourceLocation claimKey) {
                var copy = new HashMap<>(claimedFirsts);
                copy.put(claimKey, System.currentTimeMillis());
                return new PlayerEpiphanyData(aptitude, insightPoints, totalInsightPointsSpent,
                                modules, insights, epiphanies, epiphanySlots, usedEpiphanySlots, copy, displayData);
        }
}
