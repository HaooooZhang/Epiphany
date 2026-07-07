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
}
