package ink.myumoon.epiphany.client.ui.progression;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CurvePreviewDraftTest {
    @Test
    void persistedDraftRoundTripsAndPricesMatchServerAcrossAllFunctions() {
        var example = new CurvePreviewDraft();
        for (var function : CurvePreviewDraft.Function.values()) {
            example.segments.get(1).function = function;
            var rules = example.toRules();
            var loaded = new CurvePreviewDraft(rules);
            assertEquals(rules, loaded.toRules());
            var preview = loaded.preview();
            for (int point = 1; point <= 100; point++)
                assertEquals((double) rules.requiredAptitude(point - 1, 0), preview.rule(point).cost(point));
        }
        var draft = new CurvePreviewDraft(new ink.myumoon.epiphany.config.ProgressionRules(-1, 9007199254740993L, 1));
        assertEquals(9007199254740993L, draft.toRules().requiredAptitude(0, 0));
        draft.segments.getFirst().function = CurvePreviewDraft.Function.CONVERGENT;
        draft.segments.getFirst().values.put(CurvePreviewDraft.Parameter.TARGET, "9007199254740992.9999");
        assertFalse(draft.validate().valid());
    }

    @Test
    void cumulativePreviewIncludesRoundedCostsAcrossSegmentBoundaries() {
        var preview = new CurvePreviewDraft().preview();
        double sum = 0;
        assertEquals(0, preview.cumulativeCost(0));
        for (int point = 1; point <= preview.end(); point++) {
            sum += preview.rule(point).cost(point);
            assertEquals(sum, preview.cumulativeCost(point), "point " + point);
        }
        assertEquals(sum, preview.cumulativeCost(Integer.MAX_VALUE));
    }

    @Test
    void cumulativePreviewSupportsEveryFunctionAndBoundedLargeRanges() {
        for (var function : CurvePreviewDraft.Function.values()) {
            var draft = new CurvePreviewDraft();
            draft.segments.clear();
            draft.segments.add(new CurvePreviewDraft.Segment(1, function, "10.25", "0.125"));
            draft.limit = "9000";
            draft.segments.getFirst().values.put(CurvePreviewDraft.Parameter.RATIO, "1.0001");
            var preview = draft.preview();
            double exact = 0;
            for (int point = 1; point <= preview.end(); point++) exact += preview.rule(point).cost(point);
            double approximate = preview.cumulativeCost(preview.end());
            assertTrue(approximate <= exact + 0.01, function.toString());
            assertTrue(exact - approximate < 9000 - 8192, function.toString());
            draft.limit = Integer.toString(Integer.MAX_VALUE);
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                double total = draft.preview().cumulativeCost(Integer.MAX_VALUE);
                assertTrue(total > 0 && !Double.isNaN(total));
            });
        }
    }

    @Test
    void emptyRangeHasNoCumulativeCost() {
        var draft = new CurvePreviewDraft();
        draft.limit = "0";
        assertEquals(0, draft.preview().cumulativeCost(100));
    }

    @Test
    void validationIdentifiesTheInputThatNeedsHighlighting() {
        var draft = new CurvePreviewDraft();
        draft.segments.getFirst().values.put(CurvePreviewDraft.Parameter.BASE, "");
        var error = draft.validate().errors().getFirst();
        assertEquals(0, error.segment());
        assertEquals("base", error.field());
        draft.segments.getFirst().values.put(CurvePreviewDraft.Parameter.BASE, "10");
        draft.segments.get(1).start = "1";
        error = draft.validate().errors().getFirst();
        assertEquals(1, error.segment());
        assertEquals("start", error.field());
        draft.limit = "-1";
        error = draft.validate().errors().getFirst();
        assertEquals(-1, error.segment());
        assertEquals("limit", error.field());
    }

    @Test
    void validatesEveryFieldAndSegmentEvenWhenTheLimitAndOtherNumbersAreInvalid() {
        var draft = new CurvePreviewDraft();
        draft.limit = "";
        draft.segments.getFirst().values.put(CurvePreviewDraft.Parameter.BASE, "");
        draft.segments.getFirst().values.put(CurvePreviewDraft.Parameter.SLOPE, "-1");
        draft.segments.get(1).start = "1";
        draft.segments.get(1).function = CurvePreviewDraft.Function.EXPONENTIAL;
        draft.segments.get(1).values.put(CurvePreviewDraft.Parameter.RATIO, "0.5");
        draft.segments.get(2).function = CurvePreviewDraft.Function.CONVERGENT;
        draft.segments.get(2).values.put(CurvePreviewDraft.Parameter.TARGET, "1");
        draft.segments.get(2).values.put(CurvePreviewDraft.Parameter.RATE, "0");
        var result = draft.validate();
        assertFalse(result.valid());
        assertNull(result.preview());
        assertEquals(java.util.Set.of("-1:limit", "0:base", "0:slope", "1:start", "1:ratio", "2:target", "2:rate"),
                result.errors().stream().map(error -> error.segment() + ":" + error.field()).collect(java.util.stream.Collectors.toSet()));
        assertThrows(IllegalArgumentException.class, draft::preview);
        draft.segments.get(1).start = "bad";
        result = assertDoesNotThrow(draft::validate);
        assertEquals(7, result.errors().size(), "invalid start syntax must not hide errors in later fields or segments");
    }

    @Test
    void skipsOnlyDependentChecksAndIgnoresParametersNotUsedByTheFunction() {
        var draft = new CurvePreviewDraft();
        var first = draft.segments.getFirst();
        first.function = CurvePreviewDraft.Function.CONVERGENT;
        first.values.put(CurvePreviewDraft.Parameter.BASE, "bad");
        first.values.put(CurvePreviewDraft.Parameter.TARGET, "bad");
        first.values.put(CurvePreviewDraft.Parameter.RATE, "1");
        var result = draft.validate();
        assertEquals(3, result.errors().size());
        assertEquals(1, result.errors().stream().filter(error -> error.field().equals("rate")).count());
        first.function = CurvePreviewDraft.Function.CONSTANT;
        first.values.put(CurvePreviewDraft.Parameter.BASE, "10");
        assertTrue(draft.validate().valid());
    }
}
