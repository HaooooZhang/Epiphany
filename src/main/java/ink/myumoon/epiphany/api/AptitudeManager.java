package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.event.AptitudeChangedEvent;
import ink.myumoon.epiphany.event.AptitudeLevelUpEvent;
import ink.myumoon.epiphany.event.InsightPointsChangedEvent;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

public final class AptitudeManager {

    private AptitudeManager() {
    }

    public static long getAptitude(ServerPlayer player) {
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA).aptitude();
    }

    public static int getInsightPoints(ServerPlayer player) {
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA).insightPoints();
    }

    public static int getTotalInsightPointsSpent(ServerPlayer player) {
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA).totalInsightPointsSpent();
    }

    /**
     * Sets aptitude to an exact value, clamped to the current cap.
     * Fires {@link AptitudeChangedEvent}.
     */
    public static void setAptitude(ServerPlayer player, long value) {
        long delta = value - getAptitude(player);
        if (delta == 0) return;
        if (delta > 0) {
            addAptitude(player, delta);
        } else {
            // Reducing aptitude: clamp to >=0, no level-down
            PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
            long newValue = Math.max(0, value);
            if (newValue == data.aptitude()) return;
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withAptitude(newValue));
            NeoForge.EVENT_BUS.post(new AptitudeChangedEvent(player, data.aptitude(), newValue));
        }
    }

    /**
     * Adds aptitude. Excess beyond the cap triggers Insight Point level-ups.
     * Fires {@link AptitudeChangedEvent} and {@link AptitudeLevelUpEvent}.
     * <p>
     * Multiple {@link AptitudeLevelUpEvent}s may fire in a single call.
     */
    public static void addAptitude(ServerPlayer player, long amount) {
        if (amount <= 0) return;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        long aptitude = data.aptitude() + amount;
        long oldValue = data.aptitude();
        int oldInsightPoints = data.insightPoints();
        int pointsEarned = 0;

        // Level up loop in order to apply all changes in one setData call
        long required;
        while ((required = AptitudeFormula.calcRequiredAptitude(
                data.totalInsightPointsSpent(), data.insightPoints() + pointsEarned)) <= aptitude) {
            aptitude -= required;
            pointsEarned++;
        }

        if (pointsEarned > 0) {
            data = data.withAptitude(aptitude)
                    .withInsightPoints(data.insightPoints() + pointsEarned);
        } else {
            data = data.withAptitude(aptitude);
        }
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data);

        // Fire level-up events (per +1, for achievement/stat listeners)
        for (int i = 0; i < pointsEarned; i++) {
            NeoForge.EVENT_BUS.post(new AptitudeLevelUpEvent(player,
                    data.insightPoints() - pointsEarned + i + 1));
        }

        // Settlement-level event: fired once even when multiple points were earned.
        // NotificationListener subscribes to this; AptitudeLevelUpEvent is NOT for notifications.
        if (data.insightPoints() != oldInsightPoints) {
            NeoForge.EVENT_BUS.post(new InsightPointsChangedEvent(player, oldInsightPoints, data.insightPoints()));
        }

        NeoForge.EVENT_BUS.post(new AptitudeChangedEvent(player, oldValue, aptitude));
    }

    /**
     * Sets the available Insight Points to an exact value (clamped to {@code >= 0}).
     * Fires {@link InsightPointsChangedEvent} so command/award paths also notify the player.
     */
    public static void setInsightPoints(ServerPlayer player, int value) {
        int clamped = Math.max(0, value);
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        int old = data.insightPoints();
        if (old == clamped) return;
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                data.withInsightPoints(clamped));
        NeoForge.EVENT_BUS.post(new InsightPointsChangedEvent(player, old, clamped));
    }

    /**
     * Adds the given amount to the player's Insight Points.
     * Equivalent to {@code setInsightPoints(player, getInsightPoints(player) + amount)}, and fires
     * {@link InsightPointsChangedEvent} when the value changes. Symmetric with {@link #addAptitude}.
     *
     * @param amount may be negative; final value is clamped to {@code >= 0}
     */
    public static void addInsightPoints(ServerPlayer player, int amount) {
        setInsightPoints(player, getInsightPoints(player) + amount);
    }

    /**
     * Convenience getter: aptitude required for the player's next Insight Point, based on their
     * current {@code totalInsightPointsSpent} and {@code insightPoints}.
     * Wraps {@link AptitudeFormula#calcRequiredAptitude(long, int)} so callers (UI, commands, scripts)
     * don't have to combine three getters plus the formula.
     */
    public static long getRequiredForNextPoint(ServerPlayer player) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        return AptitudeFormula.calcRequiredAptitude(
                data.totalInsightPointsSpent(), data.insightPoints());
    }

    /**
     * Tops up the player's aptitude just enough to reach the threshold for the next Insight Point,
     * then defers to {@link #addAptitude} which runs the level-up loop (converting overflow into
     * Insight Points) and fires the usual aptitude/level-up events.
     * <p>
     * Extracted from the {@code /epiphany aptitude fill} command so the same behavior is available
     * to UI and KubeJS scripts without re-implementing the formula.
     *
     * @return the amount of aptitude actually granted (0 if the player was already at/above threshold)
     */
    public static long fillAptitude(ServerPlayer player) {
        long required = getRequiredForNextPoint(player);
        long toAdd = required - getAptitude(player);
        if (toAdd > 0) {
            addAptitude(player, toAdd);
            return toAdd;
        }
        return 0L;
    }
}
