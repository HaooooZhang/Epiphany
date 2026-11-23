package ink.myumoon.epiphany.content.condition;

import com.mojang.serialization.Codec;

/**
 * Comparison operators shared across multiple Condition types.
 */
public enum Comparison {
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<="),
    EQUAL("=="),
    GREATER(">"),
    LESS("<");

    public static final Codec<Comparison> CODEC = Codec.STRING.xmap(
            Comparison::fromKey,
            Comparison::getKey
    );

    private final String key;

    Comparison(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Comparison fromKey(String key) {
        return switch (key) {
            case ">=" -> GREATER_OR_EQUAL;
            case "<=" -> LESS_OR_EQUAL;
            case "==" -> EQUAL;
            case ">" -> GREATER;
            case "<" -> LESS;
            default -> GREATER_OR_EQUAL;
        };
    }

    public boolean test(long actual, long expected) {
        return test((double) actual, (double) expected);
    }

    public boolean test(int actual, int expected) {
        return test((double) actual, (double) expected);
    }

    public boolean test(double actual, double expected) {
        return switch (this) {
            case GREATER_OR_EQUAL -> actual >= expected;
            case LESS_OR_EQUAL -> actual <= expected;
            case EQUAL -> actual == expected;
            case GREATER -> actual > expected;
            case LESS -> actual < expected;
        };
    }
}
