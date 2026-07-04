package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.event.AptitudeChangedEvent;
import ink.myumoon.epiphany.event.AptitudeLevelUpEvent;
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

        // Fire level-up events
        for (int i = 0; i < pointsEarned; i++) {
            NeoForge.EVENT_BUS.post(new AptitudeLevelUpEvent(player,
                    data.insightPoints() - pointsEarned + i + 1));
        }

        NeoForge.EVENT_BUS.post(new AptitudeChangedEvent(player, oldValue, aptitude));
    }

    // set Insight Points without firing events.
    public static void setInsightPoints(ServerPlayer player, int value) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                data.withInsightPoints(Math.max(0, value)));
    }
}
