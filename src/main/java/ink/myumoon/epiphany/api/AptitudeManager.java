package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.event.AptitudeChangedEvent;
import ink.myumoon.epiphany.event.AptitudeLevelUpEvent;
import ink.myumoon.epiphany.event.InsightPointsChangedEvent;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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

    public static long getTotalInsightPoints(ServerPlayer player) {
        return totalPoints(player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA));
    }

    public static boolean canGainInsightPoints(ServerPlayer player) {
        return remainingPoints(player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)) > 0;
    }

    private static long totalPoints(PlayerEpiphanyData data) {
        return (long) data.totalInsightPointsSpent() + data.insightPoints();
    }

    private static long remainingPoints(PlayerEpiphanyData data) {
        int limit = Config.MAX_TOTAL_INSIGHT_POINTS.get();
        long storage = (long) Integer.MAX_VALUE - data.insightPoints();
        return limit < 0 ? storage : Math.max(0, Math.min(storage, (long) limit - totalPoints(data)));
    }

    /**
     * Sets aptitude through normal settlement. Positive gains obey the total point limit.
     * Fires {@link AptitudeChangedEvent}.
     */
    public static void setAptitude(ServerPlayer player, long value) {
        long clamped = Math.max(0, value);
        long delta = clamped - getAptitude(player);
        if (delta == 0) return;
        if (delta > 0) {
            addAptitude(player, delta);
        } else {
            // Reducing aptitude: clamp to >=0, no level-down
            PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
            long newValue = clamped;
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
        grantAptitude(player, amount, null);
    }

    /** Commits a positive gain and an optional first claim together, before events. */
    static boolean grantAptitude(ServerPlayer player, long amount, @Nullable ResourceLocation claimKey) {
        if (amount <= 0) return false;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        long remaining = remainingPoints(data);
        if (remaining <= 0) return false;
        if (claimKey != null && data.claimedFirsts().containsKey(claimKey)) return false;
        long aptitude;
        long oldValue = data.aptitude();
        int oldInsightPoints = data.insightPoints();
        int pointsEarned = 0;

        // Level up loop in order to apply all changes in one setData call
        try {
            aptitude = Math.addExact(data.aptitude(), amount);
            while (aptitude > 0 && pointsEarned < remaining) {
                int points = Math.addExact(data.insightPoints(), pointsEarned);
                long required = AptitudeFormula.calcRequiredAptitude(data.totalInsightPointsSpent(), points);
                if (required <= 0) throw new ArithmeticException("Non-positive aptitude price");
                if (aptitude < required) break;
                aptitude -= required;
                pointsEarned = Math.incrementExact(pointsEarned);
                Math.addExact(data.insightPoints(), pointsEarned);
            }
            if (pointsEarned == remaining) aptitude = 0;
        } catch (ArithmeticException e) {
            Epiphany.LOGGER.warn("Rejected aptitude gain before settlement: {}", e.getMessage());
            return false;
        }

        if (pointsEarned > 0) {
            data = data.withAptitude(aptitude)
                    .withInsightPoints(data.insightPoints() + pointsEarned);
        } else {
            data = data.withAptitude(aptitude);
        }
        if (claimKey != null) data = data.withClaimedFirst(claimKey);
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
        return true;
    }

    /**
     * Adds aptitude after applying the player's {@code aptitude_gain_multiplier}
     * attribute value (base plus modifiers). The scaled amount is rounded down;
     * values that scale to zero or less are ignored.
     * <p>
     * Unlike {@link #addAptitude}, this method is intended for gains that should
     * benefit from the player's aptitude-gain multiplier.
     */
    public static void addAptitudeWithMultiplier(ServerPlayer player, long amount) {
        if (!canGainInsightPoints(player)) return;
        addAptitude(player, scaleAptitudeWithMultiplier(player, amount));
    }

    /**
     * Calculates an aptitude gain using the player's effective multiplier.
     * Package-private for source grants that must determine whether a
     * {@code first_reward} claim should be recorded before granting.
     */
    static long scaleAptitudeWithMultiplier(ServerPlayer player, long amount) {
        if (amount <= 0) return 0;

        AttributeInstance attr = player.getAttribute(EpiphanyAttributes.APTITUDE_GAIN_MULTIPLIER);
        double multiplier = attr != null ? attr.getValue() : 1.0;
        if (!Double.isFinite(multiplier)) throw new ArithmeticException("Non-finite aptitude multiplier");
        if (multiplier <= 0) return 0;
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(multiplier))
                .setScale(0, RoundingMode.FLOOR).longValueExact();
    }

    /**
     * Sets the balance, clipping only increases to the remaining total point budget.
     * Existing balances above a lowered limit can still be reduced normally.
     * Fires {@link InsightPointsChangedEvent} so command/award paths also notify the player.
     */
    public static void setInsightPoints(ServerPlayer player, int value) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        commitPointChange(player, data, pointBalance(data, value));
    }

    /**
     * Adds the given amount to the player's Insight Points.
     * Uses long arithmetic before clipping the balance, and fires
     * {@link InsightPointsChangedEvent} when the value changes. Symmetric with {@link #addAptitude}.
     *
     * @param amount may be negative; final value is clamped to {@code >= 0}
     */
    public static void addInsightPoints(ServerPlayer player, int amount) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        commitPointChange(player, data, pointBalance(data, (long) data.insightPoints() + amount));
    }

    private static PlayerEpiphanyData pointBalance(PlayerEpiphanyData data, long requested) {
        long balance = Math.max(0, Math.min(Integer.MAX_VALUE, requested));
        if (balance > data.insightPoints()) {
            balance = Math.min(balance, data.insightPoints() + remainingPoints(data));
        }
        PlayerEpiphanyData updated = data.withInsightPoints((int) balance);
        if (balance > data.insightPoints() && remainingPoints(updated) == 0) {
            updated = updated.withAptitude(0);
        }
        return updated;
    }

    private static void commitPointChange(ServerPlayer player, PlayerEpiphanyData before, PlayerEpiphanyData after) {
        if (before.equals(after)) return;
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, after);
        if (before.insightPoints() != after.insightPoints()) {
            NeoForge.EVENT_BUS.post(new InsightPointsChangedEvent(player, before.insightPoints(), after.insightPoints()));
        }
        if (before.aptitude() != after.aptitude()) {
            NeoForge.EVENT_BUS.post(new AptitudeChangedEvent(player, before.aptitude(), after.aptitude()));
        }
    }

    /** Records even a zero delta, so a clipped reward can never remove unrelated points. */
    public static void applyInsightPointsReward(ServerPlayer player, String sourceKey, int amount) {
        PlayerEpiphanyData before = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        PlayerEpiphanyData after = pointBalance(before, (long) before.insightPoints() + amount);
        var receipts = new ArrayList<>(before.insightPointRewards().getOrDefault(sourceKey, List.of()));
        receipts.add(after.insightPoints() - before.insightPoints());
        after = after.withInsightPointReward(sourceKey, receipts);
        commitPointChange(player, before, after);
    }

    /** Consumes the latest receipt before events. Unknown historical amounts are never guessed. */
    public static void removeInsightPointsReward(ServerPlayer player, String sourceKey) {
        PlayerEpiphanyData before = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        List<Integer> receipts = before.insightPointRewards().get(sourceKey);
        if (receipts == null || receipts.isEmpty()) {
            Epiphany.LOGGER.warn("Cannot reverse untracked historical insight-points reward {} for {}; preserving balance",
                    sourceKey, player.getUUID());
            return;
        }
        int delta = receipts.getLast();
        PlayerEpiphanyData after = pointBalance(before, (long) before.insightPoints() - delta)
                .withInsightPointReward(sourceKey, receipts.subList(0, receipts.size() - 1));
        commitPointChange(player, before, after);
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
        if (!canGainInsightPoints(player)) return 0;
        long required = getRequiredForNextPoint(player);
        long toAdd = required - getAptitude(player);
        if (toAdd > 0) {
            return grantAptitude(player, toAdd, null) ? toAdd : 0;
        }
        return 0L;
    }
}
