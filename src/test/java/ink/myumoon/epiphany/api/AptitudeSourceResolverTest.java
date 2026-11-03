package ink.myumoon.epiphany.api;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import ink.myumoon.epiphany.content.AptitudeAmount;
import ink.myumoon.epiphany.content.AptitudeSourceConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AptitudeSourceResolverTest {
    private static final ResourceLocation BEHAVIOR = ResourceLocation.parse("epiphany:kill_entity");
    private static final ResourceLocation ZOMBIE = ResourceLocation.parse("minecraft:zombie");

    private static AptitudeSourceConfig parse(String json) {
        return AptitudeSourceConfig.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void preservesOldFixedRewardAndClaimSemantics() {
        var config = parse("{\"default\":3,\"specials\":[{\"target\":\"minecraft:zombie\",\"first_reward\":50}]}");
        var first = AptitudeSourceResolver.resolve(config, BEHAVIOR, ZOMBIE, null, key -> false);
        assertTrue(first.isFixed());
        assertEquals(53, first.reward());
        assertEquals(AptitudeSourceResolver.claimKey(BEHAVIOR, ZOMBIE), first.claimKey());
        var repeat = AptitudeSourceResolver.resolve(config, BEHAVIOR, ZOMBIE, null, key -> true);
        assertEquals(3, repeat.reward());
        assertNull(repeat.claimKey());
        assertEquals(7, new AptitudeSourceResolver.Resolution(true, 7, null).reward());
    }

    @Test
    void describesIndependentRangesAndQueriesNeverClaimOrSample() {
        var config = parse("{\"default\":[2,5],\"specials\":[{\"target\":\"minecraft:zombie\",\"first_reward\":[10,20]}]}");
        Set<ResourceLocation> claims = new HashSet<>();
        var first = AptitudeSourceResolver.resolve(config, BEHAVIOR, ZOMBIE, null, claims::contains);
        for (int i = 0; i < 10; i++) {
            assertEquals(first, AptitudeSourceResolver.resolve(config, BEHAVIOR, ZOMBIE, null, claims::contains));
        }
        assertTrue(claims.isEmpty());
        assertEquals(new AptitudeAmount(2, 5), first.baseReward());
        assertEquals(new AptitudeAmount(10, 20), first.firstReward());
        assertEquals(12, first.minReward());
        assertEquals(25, first.maxReward());
        assertFalse(first.isFixed());
        assertThrows(IllegalStateException.class, first::reward);
    }

    @Test
    void keepsExcludeFirstMatchAndFallbackRules() {
        var config = parse("{\"default\":[2,5],\"specials\":["
                + "{\"target\":\"minecraft:zombie\",\"reward\":[3,7]},"
                + "{\"target\":\"minecraft:zombie\",\"reward\":99}]}");
        assertEquals(new AptitudeAmount(3, 7), AptitudeSourceResolver.resolve(config, BEHAVIOR, ZOMBIE, null, k -> false).baseReward());
        assertEquals(new AptitudeAmount(2, 5), AptitudeSourceResolver.resolve(config, BEHAVIOR,
                ResourceLocation.parse("minecraft:skeleton"), null, k -> false).baseReward());
        var excluded = new AptitudeSourceConfig(config.defaultReward(), config.specials(), List.of("minecraft:zombie"));
        assertFalse(AptitudeSourceResolver.resolve(excluded, BEHAVIOR, ZOMBIE, null, k -> false).applies());
        assertFalse(AptitudeSourceResolver.resolve(AptitudeSourceConfig.EMPTY, BEHAVIOR, ZOMBIE, null, k -> false).applies());
    }

    @Test
    void firstOnlyRewardStopsApplyingAfterClaim() {
        var config = parse("{\"specials\":[{\"target\":\"minecraft:zombie\",\"first_reward\":[1,3]}]}");
        assertTrue(AptitudeSourceResolver.resolve(config, BEHAVIOR, ZOMBIE, null, k -> false).applies());
        assertFalse(AptitudeSourceResolver.resolve(config, BEHAVIOR, ZOMBIE, null, k -> true).applies());
    }
}
