package ink.myumoon.epiphany.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AptitudeCurveTest {
    static AptitudeCurve.Segment segment(int from, AptitudeCurve.Function type, String... parameters) {
        var values = new LinkedHashMap<String, BigDecimal>();
        for (int i = 0; i < parameters.length; i++) values.put(type.parameters.get(i), new BigDecimal(parameters[i]));
        return new AptitudeCurve.Segment(from, type, values);
    }

    @Test
    void preservesIntegerPrecisionAndRoundsEveryPolynomialPriceUp() {
        var curve = new AptitudeCurve(List.of(
                segment(0, AptitudeCurve.Function.LINEAR, "9007199254740993", "1"),
                segment(3, AptitudeCurve.Function.QUADRATIC, "10.0001", "0.125", "0.5"),
                segment(6, AptitudeCurve.Function.CONSTANT, "2.0001")));
        assertEquals(9007199254740993L, curve.required(0));
        assertEquals(9007199254740995L, curve.required(2));
        assertEquals(11, curve.required(3));
        assertEquals(11, curve.required(4));
        assertEquals(13, curve.required(5));
        assertEquals(3, curve.required(6));
        assertEquals(3, curve.required(4_000_000_000L));
        assertThrows(ArithmeticException.class, () -> AptitudeCurve.linear(Long.MAX_VALUE, 1).required(1));
    }

    @Test
    void powersMatchExactDecimalArithmeticIncludingIntegerBoundaries() {
        for (String ratio : List.of("1", "1.0001", "1.01", "1.1", "1.25", "2")) {
            var exp = segment(0, AptitudeCurve.Function.EXPONENTIAL, "1.0001", ratio);
            for (int t = 0; t < 45; t++) {
                long expected = new BigDecimal("1.0001").multiply(new BigDecimal(ratio).pow(t))
                        .setScale(0, RoundingMode.CEILING).longValueExact();
                assertEquals(expected, exp.required(t), ratio + " at " + t);
            }
        }
        for (String rate : List.of("0.0001", "0.1", "0.5", "0.9999")) {
            var convergent = segment(0, AptitudeCurve.Function.CONVERGENT, "10.0001", "100.0001", rate);
            for (int t = 0; t < 100; t++) {
                long expected = new BigDecimal("100.0001").subtract(new BigDecimal("90")
                        .multiply(BigDecimal.ONE.subtract(new BigDecimal(rate)).pow(t)))
                        .setScale(0, RoundingMode.CEILING).longValueExact();
                assertEquals(expected, convergent.required(t), rate + " at " + t);
            }
        }
    }

    @Test
    void convergentTailIsBoundedAndDoesNotRoundPastItsAsymptote() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertEquals(100, segment(0, AptitudeCurve.Function.CONVERGENT, "10", "100", "0.9999").required(4_000_000_000L));
            assertEquals(101, segment(0, AptitudeCurve.Function.CONVERGENT, "10", "100.0001", "0.0001").required(4_000_000_000L));
            assertEquals(Long.MAX_VALUE, segment(0, AptitudeCurve.Function.CONVERGENT,
                    "1", Long.toString(Long.MAX_VALUE), "0.5").required(4_000_000_000L));
            assertThrows(ArithmeticException.class, () -> segment(0, AptitudeCurve.Function.EXPONENTIAL,
                    "10", "1.0001").required(4_000_000_000L));
        });
    }

    @Test
    void rejectsInvalidStructureAndFiniteReachableOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new AptitudeCurve(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new AptitudeCurve(List.of(segment(1, AptitudeCurve.Function.CONSTANT, "10"))));
        assertThrows(IllegalArgumentException.class, () -> new AptitudeCurve(List.of(
                segment(0, AptitudeCurve.Function.CONSTANT, "10"), segment(0, AptitudeCurve.Function.CONSTANT, "20"))));
        assertThrows(IllegalArgumentException.class, () -> segment(0, AptitudeCurve.Function.CONSTANT, "0.9999"));
        assertThrows(IllegalArgumentException.class, () -> segment(0, AptitudeCurve.Function.LINEAR, "10", "0.00001"));
        assertThrows(IllegalArgumentException.class, () -> segment(0, AptitudeCurve.Function.CONVERGENT, "10", "9", "0.1"));
        var curve = new AptitudeCurve(List.of(segment(0, AptitudeCurve.Function.EXPONENTIAL, "10", "2")));
        curve.validateLimit(20);
        curve.validateLimit(-1);
        assertThrows(ArithmeticException.class, () -> curve.validateLimit(100));
    }
}
