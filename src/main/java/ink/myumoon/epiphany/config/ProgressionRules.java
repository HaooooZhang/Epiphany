package ink.myumoon.epiphany.config;


/** One complete, validated rule snapshot. Shared by server settlement and client display. */
public record ProgressionRules(int maxTotalInsightPoints, AptitudeCurve curve) {
    public static final ProgressionRules DEFAULT = new ProgressionRules(-1, 10, 1);

    public ProgressionRules(int limit, long base, long growth) {
        this(limit, AptitudeCurve.linear(base, growth));
    }

    public ProgressionRules {
        if (maxTotalInsightPoints < -1) throw new IllegalArgumentException("maxTotalInsightPoints must be -1 or greater");
        if (curve == null) throw new IllegalArgumentException("Missing aptitude curve");
    }

    /** Compatibility accessors for the old single linear rule. */
    public long baseAptitudeCap() { return curve.segments().get(0).value("base").longValueExact(); }
    public long aptitudeCapGrowth() {
        var segment = curve.segments().get(0);
        if (curve.segments().size() != 1 || segment.type() != AptitudeCurve.Function.LINEAR)
            throw new IllegalStateException("Segmented curve has no single growth value");
        return segment.value("slope").longValueExact();
    }

    public long requiredAptitude(long totalSpent, int insightPoints) {
        return curve.required(Math.addExact(totalSpent, insightPoints));
    }

    public long remainingPoints(int totalSpent, int insightPoints) {
        long storage = (long) Integer.MAX_VALUE - insightPoints;
        long total = (long) totalSpent + insightPoints;
        return maxTotalInsightPoints < 0 ? storage
                : Math.max(0, Math.min(storage, (long) maxTotalInsightPoints - total));
    }
}
