package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.event.AptitudeChangedEvent;
import ink.myumoon.epiphany.event.AptitudeLevelUpEvent;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Controls aptitude (experience-like points) and Insight Point economy.
 * <p>
 * All methods are server-side. Aptitude is clamped to the current cap
 * (calculated via {@link AptitudeFormula}) and overflowing aptitude
 * automatically triggers Insight Point level-ups.
 */
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
     * Adds aptitude. Excess beyond the cap triggers Insight Point level-ups,
     * consuming the required amount and carrying over the remainder.
     * <p>
     * Multiple {@link AptitudeLevelUpEvent}s may fire in a single call.
     * {@code totalInsightPointsSpent} is NOT increased — that only happens
     * when the player spends Insight Points.
     */
    public static void addAptitude(ServerPlayer player, long amount) {
        if (amount <= 0) return;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        long aptitude = data.aptitude() + amount;
        long oldValue = data.aptitude();
        int pointsEarned = 0;

        // Level-up loop: keep consuming until below the cap
        long required;
        while ((required = AptitudeFormula.calcRequiredAptitude(data.totalInsightPointsSpent(), data.insightPoints())) <= aptitude) {
            aptitude -= required;
            pointsEarned++;
            data = data.withAptitude(aptitude).withInsightPoints(data.insightPoints() + 1);
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data);
            // totalInsightPointsSpent unchanged — earning is not spending
            NeoForge.EVENT_BUS.post(new AptitudeLevelUpEvent(player, data.insightPoints()));
        }

        // If no level-up, just update aptitude
        if (pointsEarned == 0) {
            data = data.withAptitude(aptitude);
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data);
        }

        NeoForge.EVENT_BUS.post(new AptitudeChangedEvent(player, oldValue, aptitude));
    }

    /** Admin: directly set Insight Points without firing events. */
    public static void setInsightPoints(ServerPlayer player, int value) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                data.withInsightPoints(Math.max(0, value)));
    }
}
