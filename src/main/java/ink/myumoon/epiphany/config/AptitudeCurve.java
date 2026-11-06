package ink.myumoon.epiphany.config;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Immutable, side-independent prices. Coordinates are total points before earning the next point. */
public record AptitudeCurve(List<Segment> segments) {
    public static final int MAX_SEGMENTS = 32;
    private static final BigDecimal MAX = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigDecimal OVER = MAX.add(BigDecimal.ONE);

    public enum Function {
        CONSTANT("base"), LINEAR("base", "slope"), QUADRATIC("base", "slope", "quadratic"),
        EXPONENTIAL("base", "ratio"), CONVERGENT("base", "target", "rate");

        public final List<String> parameters;
        Function(String... parameters) { this.parameters = List.of(parameters); }
    }

    public record Segment(int from, Function type, Map<String, BigDecimal> parameters) {
        public Segment {
            if (from < 0 || from == Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid curve start: " + from);
            if (type == null || parameters == null) throw new IllegalArgumentException("Missing curve function/parameters");
            var normalized = new LinkedHashMap<String, BigDecimal>();
            for (String key : type.parameters) {
                BigDecimal value = parameters.get(key);
                if (value == null || value.signum() < 0 || value.compareTo(MAX) > 0 || value.stripTrailingZeros().scale() > 4)
                    throw new IllegalArgumentException("Invalid curve parameter: " + key);
                normalized.put(key, value.stripTrailingZeros());
            }
            parameters = Map.copyOf(normalized);
            if (parameters.get("base").compareTo(BigDecimal.ONE) < 0) throw new IllegalArgumentException("base must be at least 1");
            if (type == Function.EXPONENTIAL && parameters.get("ratio").compareTo(BigDecimal.ONE) < 0)
                throw new IllegalArgumentException("ratio must be at least 1");
            if (type == Function.CONVERGENT && (parameters.get("target").compareTo(parameters.get("base")) < 0
                    || parameters.get("rate").signum() <= 0 || parameters.get("rate").compareTo(BigDecimal.ONE) >= 0))
                throw new IllegalArgumentException("target must be >= base and 0 < rate < 1");
        }

        public BigDecimal value(String key) { return parameters.get(key); }

        public long required(long total) {
            if (total < from) throw new IllegalArgumentException("Point precedes segment");
            long t = total - from;
            BigDecimal base = value("base"), x = BigDecimal.valueOf(t);
            return switch (type) {
                case CONSTANT -> price(base);
                case LINEAR -> price(base.add(value("slope").multiply(x)));
                case QUADRATIC -> price(base.add(value("slope").multiply(x)).add(value("quadratic").multiply(x).multiply(x)));
                case EXPONENTIAL -> powerPrice(base, value("ratio"), t, null);
                case CONVERGENT -> powerPrice(value("target").subtract(base), BigDecimal.ONE.subtract(value("rate")), t, value("target"));
            };
        }
    }

    public AptitudeCurve {
        segments = List.copyOf(segments);
        if (segments.isEmpty() || segments.size() > MAX_SEGMENTS || segments.getFirst().from() != 0)
            throw new IllegalArgumentException("Curve needs 1..32 segments, starting at 0");
        for (int i = 1; i < segments.size(); i++)
            if (segments.get(i).from() <= segments.get(i - 1).from()) throw new IllegalArgumentException("Curve starts must strictly increase");
    }

    public static AptitudeCurve linear(long base, long growth) {
        return new AptitudeCurve(List.of(new Segment(0, Function.LINEAR,
                Map.of("base", BigDecimal.valueOf(base), "slope", BigDecimal.valueOf(growth)))));
    }

    public long required(long total) {
        if (total < 0) throw new IllegalArgumentException("Negative total points");
        Segment selected = segments.getFirst();
        for (Segment segment : segments) {
            if (segment.from() > total) break;
            selected = segment;
        }
        return selected.required(total);
    }

    /** All functions are nondecreasing within a segment; endpoints suffice for a finite budget. */
    public void validateLimit(int limit) {
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            segment.required(segment.from());
            if (limit > segment.from()) {
                long last = i + 1 < segments.size() ? Math.min((long) limit - 1, segments.get(i + 1).from() - 1L) : limit - 1L;
                segment.required(last);
            }
        }
    }

    private static long price(BigDecimal value) {
        BigDecimal rounded = value.setScale(0, RoundingMode.CEILING);
        if (rounded.signum() <= 0 || rounded.compareTo(MAX) > 0) throw new ArithmeticException("Aptitude price exceeds long range");
        return rounded.longValueExact();
    }

    // Directed bounds avoid double rounding around integers and the convergent asymptote.
    // Products stay bounded in size, even with billions of points and tiny decay factors.
    private record Bounds(BigDecimal low, BigDecimal high) {
        Bounds multiply(Bounds other) {
            return new Bounds(product(low, other.low, RoundingMode.FLOOR), product(high, other.high, RoundingMode.CEILING));
        }
        private static BigDecimal product(BigDecimal a, BigDecimal b, RoundingMode mode) {
            return a.multiply(b, new MathContext(80, mode)).min(OVER).setScale(64, mode);
        }
    }

    private static long powerPrice(BigDecimal base, BigDecimal ratio, long exponent, BigDecimal target) {
        Bounds result = new Bounds(base, base), power = new Bounds(ratio, ratio);
        for (long n = exponent; n > 0; n >>>= 1) {
            if ((n & 1) != 0) result = result.multiply(power);
            if (n > 1) power = power.multiply(power);
        }
        BigDecimal low = target == null ? result.low : target.subtract(result.high);
        BigDecimal high = target == null ? result.high : target.subtract(result.low);
        // A convergent value approaches an integral target from strictly below.
        if (target != null && base.signum() > 0) {
            BigDecimal margin = target.subtract(target.setScale(0, RoundingMode.FLOOR));
            if (margin.signum() == 0) margin = BigDecimal.ONE;
            if (result.high.compareTo(margin) < 0) return price(target);
        }
        if (low.setScale(0, RoundingMode.CEILING).equals(high.setScale(0, RoundingMode.CEILING))) return price(high);
        if (exponent <= 8192) {
            BigDecimal exact = base.multiply(ratio.pow((int) exponent));
            return price(target == null ? exact : target.subtract(exact));
        }
        throw new ArithmeticException("Cannot resolve aptitude price at an integer boundary");
    }
}
