package ink.myumoon.epiphany.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** TOML tables and atomic file replacement. No client or loader state. */
public final class ProgressionConfigIO {
    private ProgressionConfigIO() {}

    public static ProgressionRules read(UnmodifiableConfig raw, ProgressionRules fallback) {
        int limit = raw.contains("maxTotalInsightPoints") ? Math.toIntExact(integer(raw, "maxTotalInsightPoints")) : fallback.maxTotalInsightPoints();
        Object curve = raw.get("curve");
        if (curve == null || "legacy".equals(curve)) {
            long base = raw.contains("baseAptitudeCap") ? integer(raw, "baseAptitudeCap") : fallback.baseAptitudeCap();
            long growth = raw.contains("aptitudeCapGrowth") ? integer(raw, "aptitudeCapGrowth") : fallback.aptitudeCapGrowth();
            return new ProgressionRules(limit, base, growth);
        }
        if (!(curve instanceof List<?> tables)) throw new IllegalArgumentException("curve must be an array of tables");
        var segments = new ArrayList<AptitudeCurve.Segment>();
        for (Object entry : tables) {
            if (!(entry instanceof UnmodifiableConfig table)) throw new IllegalArgumentException("Each curve entry must be a table");
            Object type = table.get("type");
            if (!(type instanceof String text)) throw new IllegalArgumentException("Missing curve type");
            AptitudeCurve.Function function = AptitudeCurve.Function.valueOf(text.toUpperCase(Locale.ROOT));
            var values = new LinkedHashMap<String, BigDecimal>();
            for (String key : function.parameters) {
                Object value = table.get(key);
                if (!(value instanceof Number) && !(value instanceof String textValue && textValue.matches("\\d+(\\.\\d{1,4})?")))
                    throw new IllegalArgumentException("Invalid parameter " + key);
                values.put(key, new BigDecimal(value.toString()));
            }
            segments.add(new AptitudeCurve.Segment(Math.toIntExact(integer(table, "from")), function, values));
        }
        return new ProgressionRules(limit, new AptitudeCurve(segments));
    }

    private static long integer(UnmodifiableConfig raw, String key) {
        Object value = raw.get(key);
        if (value instanceof Integer number) return number.longValue();
        if (value instanceof Long number) return number;
        throw new IllegalArgumentException(key + " must be an integer (got " + value + ")");
    }

    public static List<CommentedConfig> tables(AptitudeCurve curve) {
        return curve.segments().stream().map(segment -> {
            CommentedConfig table = CommentedConfig.inMemory();
            table.set("from", segment.from());
            table.set("type", segment.type().name().toLowerCase(Locale.ROOT));
            segment.type().parameters.forEach(key -> {
                BigDecimal value = segment.value(key);
                Object encoded = value.scale() <= 0 ? value.longValueExact() :
                        BigDecimal.valueOf(value.doubleValue()).compareTo(value) == 0 ? value : value.toPlainString();
                table.set(key, encoded);
            });
            return table;
        }).toList();
    }

    public static void put(CommentedConfig raw, ProgressionRules rules) {
        raw.set("maxTotalInsightPoints", rules.maxTotalInsightPoints());
        raw.set("curve", tables(rules.curve()));
    }

    public static String readFile(Path path) throws IOException {
        return Files.exists(path) ? Files.readString(path) : null;
    }

    public static ProgressionRules readFile(String text, ProgressionRules fallback) {
        return text == null ? fallback : read(new TomlParser().parse(text), fallback);
    }

    /** Compare to the opened file as well as the rule snapshot; never overwrite another editor's changes. */
    public static synchronized void save(Path path, String expected, ProgressionRules rules) throws IOException {
        rules.curve().validateLimit(rules.maxTotalInsightPoints());
        if (!java.util.Objects.equals(expected, readFile(path))) throw new IOException("epiphany.curve.error.changed");
        CommentedConfig raw = expected == null ? CommentedConfig.inMemory() : new TomlParser().parse(expected);
        put(raw, rules);
        // Round-trip before touching the file, including large integer and decimal TOML values.
        String text = new TomlWriter().writeToString(raw);
        if (!readFile(text, ProgressionRules.DEFAULT).equals(rules)) throw new IOException("TOML cannot preserve curve precision");
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "epiphany-progression-", ".tmp");
        try {
            Files.writeString(temporary, text);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
