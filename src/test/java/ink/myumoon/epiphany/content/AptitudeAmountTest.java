package ink.myumoon.epiphany.content;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.RandomSource;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AptitudeAmountTest {
    private static AptitudeAmount parse(String json) {
        return AptitudeAmount.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void acceptsAllThreeFormsAndRoundTripsWithoutLosingLongPrecision() {
        assertEquals(AptitudeAmount.fixed(5), parse("5"));
        assertEquals(AptitudeAmount.fixed(5), parse("[5,5]"));
        assertEquals(parse("[2,5]"), parse("{\"min\":2,\"max\":5}"));
        for (String json : new String[]{"0", "9223372036854775807", "[0,9223372036854775807]",
                "{\"min\":9007199254740993,\"max\":9223372036854775807}"}) {
            AptitudeAmount amount = parse(json);
            var encoded = AptitudeAmount.CODEC.encodeStart(JsonOps.INSTANCE, amount).getOrThrow();
            assertEquals(amount, AptitudeAmount.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        }
        assertEquals(9007199254740993L, parse("[9007199254740993,9223372036854775807]").min());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[1]", "[1,2,3]", "[2,1]", "-1", "[0,-1]", "1.5", "[1.5,2]",
            "9223372036854775808", "{\"min\":1}", "{\"max\":2}", "{\"min\":2,\"max\":1}",
            "{\"min\":0,\"max\":2.5}", "\"5\"", "true", "null"})
    void rejectsMalformedAmounts(String json) {
        assertTrue(AptitudeAmount.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).isError(), json);
    }

    @Test
    void samplesInclusiveEndpointsAndRejectsBiasedTail() {
        RandomSource random = mock(RandomSource.class);
        when(random.nextLong()).thenReturn(0L, 4L, -1L, 2L);
        AptitudeAmount amount = new AptitudeAmount(2, 4);
        assertEquals(2, amount.sample(random));
        assertEquals(4, amount.sample(random));
        assertEquals(3, amount.sample(random)); // -1 supplies a rejected tail value
        verify(random, times(4)).nextLong();
    }

    @Test
    void handlesFullLongDomainAndNearMaximumEndpoints() {
        RandomSource random = mock(RandomSource.class);
        when(random.nextLong()).thenReturn(0L, -1L, 0L, 2L);
        AptitudeAmount full = new AptitudeAmount(0, Long.MAX_VALUE);
        assertEquals(0, full.sample(random));
        assertEquals(Long.MAX_VALUE, full.sample(random));
        AptitudeAmount upper = new AptitudeAmount(Long.MAX_VALUE - 1, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE - 1, upper.sample(random));
        assertEquals(Long.MAX_VALUE, upper.sample(random));
    }

    @Test
    void fixedAmountDoesNotConsumeRandomness() {
        RandomSource random = mock(RandomSource.class);
        assertEquals(5, AptitudeAmount.fixed(5).sample(random));
        verifyNoInteractions(random);
    }

    @Test
    void seededSamplesStayInsideRange() {
        RandomSource random = RandomSource.create(1729L);
        AptitudeAmount range = new AptitudeAmount(9007199254740993L, Long.MAX_VALUE);
        for (int i = 0; i < 10_000; i++) {
            long sample = range.sample(random);
            assertTrue(sample >= range.min() && sample <= range.max());
        }
    }

    @Test
    void rejectsInvalidOptionalFieldsAndCombinedRewardOverflow() {
        for (String json : new String[]{"{\"default\":[5,1]}",
                "{\"specials\":[{\"target\":\"minecraft:zombie\",\"reward\":1.5}]}",
                "{\"default\":9223372036854775807,\"specials\":[{\"target\":\"minecraft:zombie\",\"first_reward\":1}]}"}) {
            assertTrue(AptitudeSourceConfig.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).isError(), json);
        }
    }

    @Test
    void sourceRangesSurviveRegistryNetworkCodecRoundTrip() {
        var config = AptitudeSourceConfig.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"default\":[2,5],\"specials\":[{\"target\":\"minecraft:zombie\","
                        + "\"reward\":7,\"first_reward\":{\"min\":10,\"max\":20}}]}" )).getOrThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            AptitudeSourceConfig.STREAM_CODEC.encode(buffer, config);
            assertEquals(config, AptitudeSourceConfig.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
