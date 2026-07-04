package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Config;

/**
 * Calculates the aptitude required to earn the next Insight Point.
 * <p>
 * Current formula: {@code baseAptitudeCap + totalEarned × aptitudeCapGrowth}
 * where {@code totalEarned = totalSpent + insightPoints}.
 * <p>
 * Example (default: base=10, growth=1):
 * <ul>
 *   <li>1st (0 earned): 10</li>
 *   <li>2nd (1 earned): 11</li>
 *   <li>3rd (2 earned): 12</li>
 * </ul>
 */
public final class AptitudeFormula {

    private AptitudeFormula() {
    }

    // TODO: Design more complex formulas (exponential growth, diminishing returns, etc.)

    /**
     * Calculates the aptitude required based on total Insight Points ever acquired.
     *
     * @param totalSpent  how many Insight Points spent
     * @param insightPoints how many available (unspent)
     * @return required aptitude
     */
    public static long calcRequiredAptitude(long totalSpent, int insightPoints) {
        long totalEarned = totalSpent + insightPoints;
        return Config.BASE_APTITUDE_CAP.get()
                + totalEarned * Config.APTITUDE_CAP_GROWTH.get();
    }
}
