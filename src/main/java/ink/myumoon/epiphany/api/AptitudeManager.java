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
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        long cap = AptitudeFormula.calcRequiredAptitude(data.totalInsightPointsSpent());
        long newValue = Math.max(0, Math.min(value, cap));
        if (newValue == data.aptitude()) return;

        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withAptitude(newValue));
        NeoForge.EVENT_BUS.post(new AptitudeChangedEvent(player, data.aptitude(), newValue));
    }

    /**
     * Adds aptitude. Excess beyond the current cap is discarded,
     * but level-ups may increase the cap and re-consume remaining aptitude.
     * <p>
     * Multiple {@link AptitudeLevelUpEvent}s may fire in a single call.
     */
    public static void addAptitude(ServerPlayer player, long amount) {
        if (amount <= 0) return;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        long aptitude = data.aptitude() + amount;
        long required = AptitudeFormula.calcRequiredAptitude(data.totalInsightPointsSpent());

        // Step 1: clamp to current cap and fire Changed
        aptitude = Math.min(aptitude, required);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withAptitude(aptitude));
        NeoForge.EVENT_BUS.post(new AptitudeChangedEvent(player, data.aptitude(), aptitude));

        // Step 2: level-up loop
        data = data.withAptitude(aptitude);
        int insightPoints = data.insightPoints();
        int totalSpent = data.totalInsightPointsSpent();
        int leveled = 0;

        while (data.aptitude() >= required) {
            aptitude = data.aptitude() - required;
            totalSpent++;
            insightPoints++;
            leveled++;
            data = data.withAptitude(aptitude)
                    .withTotalInsightPointsSpent(totalSpent)
                    .withInsightPoints(insightPoints);
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data);
            NeoForge.EVENT_BUS.post(new AptitudeLevelUpEvent(player, totalSpent));
            required = AptitudeFormula.calcRequiredAptitude(totalSpent);
        }

        if (leveled == 0) {
            // Ensure final setData if no level-up occurred (already done above but safe)
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data);
        }
    }

    /** Admin: directly set Insight Points without firing events. */
    public static void setInsightPoints(ServerPlayer player, int value) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                data.withInsightPoints(Math.max(0, value)));
    }
}
