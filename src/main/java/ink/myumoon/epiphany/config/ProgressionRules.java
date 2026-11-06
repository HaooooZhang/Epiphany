package ink.myumoon.epiphany.config;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** One complete, validated rule snapshot. Shared by server settlement and client display. */
public record ProgressionRules(int maxTotalInsightPoints, AptitudeCurve curve) {
    public static final ProgressionRules DEFAULT = new ProgressionRules(-1, 10, 1);

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionRules> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ProgressionRules rules) {
            buffer.writeVarInt(rules.maxTotalInsightPoints());
            buffer.writeVarInt(rules.curve().segments().size());
            for (var segment : rules.curve().segments()) {
                buffer.writeVarInt(segment.from());
                buffer.writeEnum(segment.type());
                for (String key : segment.type().parameters) buffer.writeUtf(segment.value(key).toPlainString(), 32);
            }
        }

        @Override
        public ProgressionRules decode(RegistryFriendlyByteBuf buffer) {
            int limit = buffer.readVarInt(), count = buffer.readVarInt();
            if (count < 1 || count > AptitudeCurve.MAX_SEGMENTS) throw new IllegalArgumentException("Invalid curve length");
            var segments = new java.util.ArrayList<AptitudeCurve.Segment>();
            for (int i = 0; i < count; i++) {
                int from = buffer.readVarInt();
                var type = buffer.readEnum(AptitudeCurve.Function.class);
                var values = new java.util.LinkedHashMap<String, java.math.BigDecimal>();
                for (String key : type.parameters) values.put(key, new java.math.BigDecimal(buffer.readUtf(32)));
                segments.add(new AptitudeCurve.Segment(from, type, values));
            }
            return new ProgressionRules(limit, new AptitudeCurve(segments));
        }
    };

    public ProgressionRules(int limit, long base, long growth) {
        this(limit, AptitudeCurve.linear(base, growth));
    }

    public ProgressionRules {
        if (maxTotalInsightPoints < -1) throw new IllegalArgumentException("maxTotalInsightPoints must be -1 or greater");
        if (curve == null) throw new IllegalArgumentException("Missing aptitude curve");
    }

    /** Compatibility accessors for the old single linear rule. */
    public long baseAptitudeCap() { return curve.segments().getFirst().value("base").longValueExact(); }
    public long aptitudeCapGrowth() {
        var segment = curve.segments().getFirst();
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
