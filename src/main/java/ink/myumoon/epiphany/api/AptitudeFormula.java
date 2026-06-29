package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Config;

/**
 * Calculates the aptitude required to earn the next Insight Point.
 * <p>
 * Current formula: {@code baseAptitudeCap + totalSpent × aptitudeCapGrowth}
 * <p>
 * Example progression (default config: base=100, growth=50):
 * <ul>
 *   <li>1st point: 100 aptitude</li>
 *   <li>2nd point: 150 aptitude</li>
 *   <li>3rd point: 200 aptitude</li>
 * </ul>
 */
public final class AptitudeFormula {

    private AptitudeFormula() {
    }

    // TODO: Design more complex formulas (exponential growth, diminishing returns, etc.)

    /**
     * Calculates the total aptitude required to earn the next Insight Point.
     *
     * @param totalSpent how many Insight Points the player has already spent
     * @return the required aptitude amount
     */
    public static long calcRequiredAptitude(long totalSpent) {
        return Config.BASE_APTITUDE_CAP.get()
                + totalSpent * Config.APTITUDE_CAP_GROWTH.get();
    }
}
