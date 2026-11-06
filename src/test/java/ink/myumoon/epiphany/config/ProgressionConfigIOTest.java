package ink.myumoon.epiphany.config;

import com.electronwill.nightconfig.toml.TomlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static ink.myumoon.epiphany.config.AptitudeCurveTest.segment;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionConfigIOTest {
    @TempDir Path directory;

    @Test
    void importsExistingLinearValuesAndTablesTakePrecedence() throws Exception {
        Path file = directory.resolve("growth.toml");
        String old = "# pack values\nmaxTotalInsightPoints=10\nbaseAptitudeCap=23\naptitudeCapGrowth=4\n";
        Files.writeString(file, old);
        var migrated = ProgressionConfigIO.readFile(old, ProgressionRules.DEFAULT);
        assertEquals(new ProgressionRules(10, 23, 4), migrated);
        ProgressionConfigIO.save(file, old, migrated);
        String saved = Files.readString(file);
        assertTrue(saved.contains("[[curve]]"));
        assertTrue(saved.contains("pack values"));
        var staleLegacy = new TomlParser().parse(saved);
        staleLegacy.set("aptitudeCapGrowth", -4);
        staleLegacy.set("baseAptitudeCap", "unused");
        assertEquals(migrated, ProgressionConfigIO.read(staleLegacy, ProgressionRules.DEFAULT));
    }

    @Test
    void allFunctionsRoundTripAndKeepLargeDecimalsExact() throws Exception {
        var expected = new ProgressionRules(-1, new AptitudeCurve(List.of(
                segment(0, AptitudeCurve.Function.CONSTANT, "9007199254740993.0001"),
                segment(10, AptitudeCurve.Function.LINEAR, "10.25", "0.125"),
                segment(20, AptitudeCurve.Function.QUADRATIC, "20", "1", "0.5"),
                segment(30, AptitudeCurve.Function.EXPONENTIAL, "50", "1.0001"),
                segment(40, AptitudeCurve.Function.CONVERGENT, "100", "200.0001", "0.1"))));
        Path file = directory.resolve("new/growth.toml");
        ProgressionConfigIO.save(file, null, expected);
        assertEquals(expected, ProgressionConfigIO.readFile(Files.readString(file), ProgressionRules.DEFAULT));
        var nativeConfig = new TomlParser().parse(Files.readString(file));
        ProgressionConfig.SPEC.correct(nativeConfig);
        assertEquals(expected, ProgressionConfig.readRules(nativeConfig));
        assertTrue(Files.readString(file).contains("\"9007199254740993.0001\""));
        try (var entries = Files.list(file.getParent())) { assertEquals(1, entries.count()); }
    }

    @Test
    void rejectsMalformedTablesInsteadOfSilentlyUsingDefaults() {
        for (String curve : List.of("curve=[]", "curve=\"invalid\"", "[[curve]]\nfrom=0\ntype=\"unknown\"",
                "[[curve]]\nfrom=1\ntype=\"constant\"\nbase=10", "[[curve]]\nfrom=0\ntype=\"constant\"\nbase=nan")) {
            assertThrows(IllegalArgumentException.class, () -> ProgressionConfigIO.read(new TomlParser().parse(curve), ProgressionRules.DEFAULT));
        }
    }

    @Test
    void conflictsValidationAndIoFailuresLeaveThePreviousFileIntact() throws Exception {
        Path file = directory.resolve("growth.toml");
        ProgressionConfigIO.save(file, null, ProgressionRules.DEFAULT);
        String original = Files.readString(file);
        assertThrows(java.io.IOException.class, () -> ProgressionConfigIO.save(file, "stale", new ProgressionRules(3, 10, 0)));
        assertEquals(original, Files.readString(file));
        var overflow = new ProgressionRules(100, new AptitudeCurve(List.of(segment(0, AptitudeCurve.Function.EXPONENTIAL, "10", "2"))));
        assertThrows(ArithmeticException.class, () -> ProgressionConfigIO.save(file, original, overflow));
        assertEquals(original, Files.readString(file));
        Path parentFile = directory.resolve("not-a-directory");
        Files.writeString(parentFile, "preserved");
        assertThrows(java.io.IOException.class, () -> ProgressionConfigIO.save(parentFile.resolve("growth.toml"), null, ProgressionRules.DEFAULT));
        assertEquals("preserved", Files.readString(parentFile));
    }
}
