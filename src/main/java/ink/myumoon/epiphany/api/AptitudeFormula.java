package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.config.ProgressionConfig;

/** Server-side price API for the active segmented curve. Client displays use the synced rule snapshot. */
public final class AptitudeFormula {

    private AptitudeFormula() {
    }

    /**
     * Calculates the aptitude required based on total Insight Points ever acquired.
     *
     * @param totalSpent  how many Insight Points spent
     * @param insightPoints how many available (unspent)
     * @return required aptitude
     */
    public static long calcRequiredAptitude(long totalSpent, int insightPoints) {
        return ProgressionConfig.serverRules().requiredAptitude(totalSpent, insightPoints);
    }
}
