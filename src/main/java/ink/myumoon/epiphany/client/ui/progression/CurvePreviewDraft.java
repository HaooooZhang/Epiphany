package ink.myumoon.epiphany.client.ui.progression;

import net.minecraft.network.chat.Component;
import ink.myumoon.epiphany.config.AptitudeCurve;
import ink.myumoon.epiphany.config.ProgressionRules;
import java.util.LinkedHashMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Editable text draft; exact prices use the same evaluator as server settlement. */
final class CurvePreviewDraft {
    enum Parameter {
        BASE("base", "10"), SLOPE("slope", "1"), QUADRATIC("quadratic", "0.5"),
        RATIO("ratio", "1.05"), TARGET("target", "100"), RATE("rate", "0.1");

        final String key;
        final String initial;

        Parameter(String key, String initial) {
            this.key = key;
            this.initial = initial;
        }
    }

    enum Function {
        CONSTANT("constant", "f(t) = base", Parameter.BASE),
        LINEAR("linear", "f(t) = base + slope * t", Parameter.BASE, Parameter.SLOPE),
        QUADRATIC("quadratic", "f(t) = base + slope * t + q * t²", Parameter.BASE, Parameter.SLOPE, Parameter.QUADRATIC),
        EXPONENTIAL("exponential", "f(t) = base * ratio^t", Parameter.BASE, Parameter.RATIO),
        CONVERGENT("convergent", "f(t) = target - (target - base) * (1 - rate)^t", Parameter.BASE, Parameter.TARGET, Parameter.RATE);

        final String key;
        final String formula;
        final List<Parameter> parameters;

        Function(String key, String formula, Parameter... parameters) {
            this.key = key;
            this.formula = formula;
            this.parameters = List.of(parameters);
        }

        Component title() {
            return Component.translatable("epiphany.curve.function." + key);
        }
    }

    static final class Segment {
        String start;
        Function function;
        final EnumMap<Parameter, String> values = new EnumMap<>(Parameter.class);

        Segment(int start, Function function, String base, String slope) {
            this.start = Integer.toString(start);
            this.function = function;
            for (var parameter : Parameter.values()) values.put(parameter, parameter.initial);
            values.put(Parameter.BASE, base);
            values.put(Parameter.SLOPE, slope);
        }
    }

    record Rule(int start, Function function, EnumMap<Parameter, Double> values, AptitudeCurve.Segment evaluator) {
        double cost(int point) {
            try { return evaluator.required(Math.max(evaluator.from(), (long) point - 1)); }
            catch (ArithmeticException exception) { return Double.POSITIVE_INFINITY; }
        }

        double value(Parameter parameter) {
            return values.getOrDefault(parameter, 0.0);
        }
    }

    static final class Preview {
        private static final int EXACT_PREFIX = 8192;
        private final int end;
        private final boolean unlimited;
        private final List<Rule> rules;
        private final double[][] totals;

        Preview(int end, boolean unlimited, List<Rule> rules) {
            this.end = end;
            this.unlimited = unlimited;
            this.rules = rules;
            this.totals = new double[rules.size()][];
        }

        int end() { return end; }
        boolean unlimited() { return unlimited; }
        List<Rule> rules() { return rules; }

        Rule rule(int point) {
            Rule result = rules.getFirst();
            for (var rule : rules) {
                if (rule.start > point) break;
                result = rule;
            }
            return result;
        }

        double cumulativeCost(int point) {
            double total = 0;
            point = Math.min(end, point);
            for (int i = 0; i < rules.size(); i++) {
                var rule = rules.get(i);
                if (point < rule.start()) break;
                int last = i + 1 < rules.size() ? Math.min(point, rules.get(i + 1).start() - 1) : point;
                int count = last - rule.start() + 1;
                if (totals[i] == null) {
                    int available = i + 1 < rules.size() ? Math.min(end, rules.get(i + 1).start() - 1) : end;
                    var prefix = new double[Math.min(EXACT_PREFIX, available - rule.start() + 1) + 1];
                    for (int k = 1; k < prefix.length; k++) prefix[k] = prefix[k - 1] + rule.cost(rule.start() + k - 1);
                    totals[i] = prefix;
                }
                int exact = Math.min(count, totals[i].length - 1);
                total += totals[i][exact];
                // Large ranges use the unrounded analytic tail for bounded UI
                // work. Its rounding error is less than one aptitude per point;
                // this screen always labels the result as an approximation.
                if (count > exact) total += rawSum(rule, exact, count - exact);
            }
            return total;
        }

        private static double rawSum(Rule rule, double start, double count) {
            double base = rule.value(Parameter.BASE);
            double linear = count * (2 * start + count - 1) / 2;
            double quadratic = count * start * start + start * count * (count - 1)
                    + count * (count - 1) * (2 * count - 1) / 6;
            return switch (rule.function()) {
                case CONSTANT -> Math.ceil(base) * count;
                case LINEAR -> base * count + rule.value(Parameter.SLOPE) * linear;
                case QUADRATIC -> base * count + rule.value(Parameter.SLOPE) * linear
                        + rule.value(Parameter.QUADRATIC) * quadratic;
                case EXPONENTIAL -> base * Math.pow(rule.value(Parameter.RATIO), start)
                        * geometricSum(rule.value(Parameter.RATIO), count);
                case CONVERGENT -> {
                    double delta = rule.value(Parameter.TARGET) - base;
                    double ratio = 1 - rule.value(Parameter.RATE);
                    yield base * count + delta * (count - Math.pow(ratio, start) * geometricSum(ratio, count));
                }
            };
        }

        private static double geometricSum(double ratio, double count) {
            if (ratio == 1) return count;
            double log = Math.log(ratio);
            return Math.expm1(count * log) / Math.expm1(log);
        }
    }

    record InputError(int segment, String field, String message) {}

    record Validation(Preview preview, Integer end, List<InputError> errors) {
        boolean valid() { return errors.isEmpty(); }
    }

    final List<Segment> segments = new ArrayList<>();
    String limit = "100";
    String previewEnd = "200";
    boolean unlimited;
    int selected;

    CurvePreviewDraft() {
        segments.add(new Segment(1, Function.LINEAR, "10", "1"));
        segments.add(new Segment(21, Function.LINEAR, "30", "3"));
        segments.add(new Segment(51, Function.QUADRATIC, "120", "5"));
    }

    CurvePreviewDraft(ProgressionRules rules) {
        segments.clear();
        unlimited = rules.maxTotalInsightPoints() < 0;
        limit = Integer.toString(unlimited ? 100 : rules.maxTotalInsightPoints());
        for (var rule : rules.curve().segments()) {
            var segment = new Segment(rule.from() + 1, Function.valueOf(rule.type().name()), "10", "1");
            for (var parameter : segment.function.parameters)
                segment.values.put(parameter, rule.value(parameter.key).toPlainString());
            segments.add(segment);
        }
    }

    ProgressionRules toRules() {
        var result = validate();
        if (!result.valid()) throw new IllegalArgumentException(result.errors().getFirst().message());
        return new ProgressionRules(unlimited ? -1 : Integer.parseInt(limit),
                new AptitudeCurve(result.preview().rules().stream().map(Rule::evaluator).toList()));
    }

    Preview preview() {
        var validation = validate();
        if (!validation.valid()) throw new IllegalArgumentException(validation.errors().getFirst().message());
        return validation.preview();
    }

    Validation validate() {
        List<InputError> errors = new ArrayList<>();
        Integer end = readInteger(unlimited ? previewEnd : limit, unlimited ? 1 : 0, -1, "limit", errors);
        List<Rule> rules = new ArrayList<>();
        Integer previous = null;
        for (int index = 0; index < segments.size(); index++) {
            var segment = segments.get(index);
            Integer start = readInteger(segment.start, 1, index, "start", errors);
            if (start != null && ((index == 0 && start != 1) || (previous != null && start <= previous)))
                errors.add(error(index, "start", "starts"));
            previous = start;
            var values = new EnumMap<Parameter, Double>(Parameter.class);
            var exact = new LinkedHashMap<String, BigDecimal>();
            for (var parameter : segment.function.parameters) {
                String text = segment.values.get(parameter);
                if (text == null || !text.matches("\\d+(\\.\\d{1,4})?")) {
                    errors.add(error(index, parameter.key, "number"));
                    continue;
                }
                BigDecimal number = new BigDecimal(text);
                if (number.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
                    errors.add(error(index, parameter.key, "number"));
                    continue;
                }
                values.put(parameter, number.doubleValue());
                exact.put(parameter.key, number);
            }
            var base = values.get(Parameter.BASE);
            var ratio = values.get(Parameter.RATIO);
            if (base != null && exact.get("base").compareTo(BigDecimal.ONE) < 0) errors.add(error(index, "base", "base"));
            if (segment.function == Function.EXPONENTIAL && ratio != null && exact.get("ratio").compareTo(BigDecimal.ONE) < 0)
                errors.add(error(index, "ratio", "ratio"));
            if (segment.function == Function.CONVERGENT) {
                var target = values.get(Parameter.TARGET);
                var rate = values.get(Parameter.RATE);
                if (base != null && target != null && exact.get("target").compareTo(exact.get("base")) < 0)
                    errors.add(error(index, "target", "convergent"));
                if (rate != null && (exact.get("rate").signum() <= 0 || exact.get("rate").compareTo(BigDecimal.ONE) >= 0))
                    errors.add(error(index, "rate", "convergent"));
            }
            final int current = index;
            if (start != null && errors.stream().noneMatch(error -> error.segment() == current))
                rules.add(new Rule(start, segment.function, values,
                        new AptitudeCurve.Segment(start - 1, AptitudeCurve.Function.valueOf(segment.function.name()), exact)));
        }
        return new Validation(errors.isEmpty() ? new Preview(end, unlimited, List.copyOf(rules)) : null,
                end, List.copyOf(errors));
    }

    private static Integer readInteger(String text, int minimum, int segment, String field, List<InputError> errors) {
        try {
            if (text == null || !text.matches("\\d+")) throw new NumberFormatException();
            int result = Integer.parseInt(text);
            if (result < minimum) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException exception) {
            errors.add(error(segment, field, "integer"));
            return null;
        }
    }

    private static InputError error(int segment, String field, String key) {
        return new InputError(segment, field, "epiphany.curve.error." + key);
    }
}
