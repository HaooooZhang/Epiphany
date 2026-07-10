package ink.myumoon.epiphany.binding;

import ink.myumoon.epiphany.api.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * KubeJS binding for Epiphany Manager APIs.
 * <p>
 * Registered via {@code BindingRegistry.add("Epiphany", EpiphanyBinding.class)}.
 * All static methods become callable from JS as {@code Epiphany.methodName(...)}.
 * <p>
 * Event listeners live in {@code EpiphanyEvents.*} (KubeJS EventGroup), not here.
 */
public interface EpiphanyBinding {

    // ─── ResourceLocation factory ──────────────────────────────────

    static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    // ─── Module API ────────────────────────────────────────────────

    /** JS: {@code Epiphany.isModuleUnlocked(player, id)} */
    static boolean isModuleUnlocked(ServerPlayer player, ResourceLocation moduleId) {
        return ModuleManager.isUnlocked(player, moduleId);
    }

    /** JS: {@code Epiphany.isModuleSelected(player, id)} */
    static boolean isModuleSelected(ServerPlayer player, ResourceLocation moduleId) {
        return ModuleManager.isSelected(player, moduleId);
    }

    /** JS: {@code Epiphany.isModuleCompleted(player, id)} */
    static boolean isModuleCompleted(ServerPlayer player, ResourceLocation moduleId) {
        return ModuleManager.isCompleted(player, moduleId);
    }

    /** Unlocks a Module for the player. JS: {@code Epiphany.moduleUnlock(player, id)} */
    static void moduleUnlock(ServerPlayer player, ResourceLocation moduleId) {
        ModuleManager.setUnlocked(player, moduleId, true);
    }

    /** Locks a Module for the player. JS: {@code Epiphany.moduleLock(player, id)} */
    static void moduleLock(ServerPlayer player, ResourceLocation moduleId) {
        ModuleManager.setUnlocked(player, moduleId, false);
    }

    /** Selects a Module (consumes Insight Points). JS: {@code Epiphany.moduleSelect(player, id)} */
    static void moduleSelect(ServerPlayer player, ResourceLocation moduleId) {
        ModuleManager.select(player, moduleId);
    }

    /** Force-selects a Module (bypasses cost, limit, and condition checks). JS: {@code Epiphany.moduleForceSelect(player, id)} */
    static void moduleForceSelect(ServerPlayer player, ResourceLocation moduleId) {
        ModuleManager.forceSelect(player, moduleId);
    }

    /** Completes a Module (all Insights unlocked → Epiphany slot). JS: {@code Epiphany.moduleComplete(player, id)} */
    static void moduleComplete(ServerPlayer player, ResourceLocation moduleId) {
        ModuleManager.complete(player, moduleId);
    }

    /** Force-completes a Module (unlocks all Insights + grants slot). JS: {@code Epiphany.moduleForceComplete(player, id)} */
    static void moduleForceComplete(ServerPlayer player, ResourceLocation moduleId) {
        ModuleManager.forceComplete(player, moduleId);
    }

    /** Resets a Module for the player (refunds all points). JS: {@code Epiphany.moduleReset(player, id)} */
    static void moduleReset(ServerPlayer player, ResourceLocation moduleId) {
        ModuleManager.resetModule(player, moduleId);
    }

    // ─── Insight API ───────────────────────────────────────────────

    /** JS: {@code Epiphany.isInsightSelected(player, id)} */
    static boolean isInsightSelected(ServerPlayer player, ResourceLocation insightId) {
        return InsightManager.isSelected(player, insightId);
    }

    /** JS: {@code Epiphany.isInsightModuleSelected(player, id)} */
    static boolean isInsightModuleSelected(ServerPlayer player, ResourceLocation insightId) {
        return InsightManager.isModuleSelected(player, insightId);
    }

    /** Unlocks an Insight for the player. JS: {@code Epiphany.insightSelect(player, insightId, moduleId)} */
    static void insightSelect(ServerPlayer player, ResourceLocation insightId, ResourceLocation moduleId) {
        InsightManager.select(player, insightId, moduleId);
    }

    /** Force-unlocks an Insight (bypasses cost and tree checks). JS: {@code Epiphany.insightForceSelect(player, insightId, moduleId)} */
    static void insightForceSelect(ServerPlayer player, ResourceLocation insightId, ResourceLocation moduleId) {
        InsightManager.forceSelect(player, insightId, moduleId);
    }

    /** Resets an Insight for the player. JS: {@code Epiphany.insightReset(player, id)} */
    static void insightReset(ServerPlayer player, ResourceLocation insightId) {
        InsightManager.resetInsight(player, insightId);
    }

    // ─── Epiphany API ──────────────────────────────────────────────

    /** JS: {@code Epiphany.isEpiphanyUnlocked(player, id)} */
    static boolean isEpiphanyUnlocked(ServerPlayer player, ResourceLocation epiphanyId) {
        return EpiphanyManager.isUnlocked(player, epiphanyId);
    }

    /** JS: {@code Epiphany.isEpiphanySelected(player, id)} */
    static boolean isEpiphanySelected(ServerPlayer player, ResourceLocation epiphanyId) {
        return EpiphanyManager.isSelected(player, epiphanyId);
    }

    /** Unlocks an Epiphany. JS: {@code Epiphany.epiphanyUnlock(player, id)} */
    static void epiphanyUnlock(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyManager.setUnlocked(player, epiphanyId, true);
    }

    /** Locks an Epiphany. JS: {@code Epiphany.epiphanyLock(player, id)} */
    static void epiphanyLock(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyManager.setUnlocked(player, epiphanyId, false);
    }

    /** Selects an Epiphany (consumes a slot). JS: {@code Epiphany.epiphanySelect(player, id)} */
    static void epiphanySelect(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyManager.select(player, epiphanyId);
    }

    /** Force-selects an Epiphany (bypasses slot limits). JS: {@code Epiphany.epiphanyForceSelect(player, id)} */
    static void epiphanyForceSelect(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyManager.forceSelect(player, epiphanyId);
    }

    /** Resets an Epiphany for the player. JS: {@code Epiphany.epiphanyReset(player, id)} */
    static void epiphanyReset(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyManager.resetEpiphany(player, epiphanyId);
    }

    // ─── Aptitude / Points API ─────────────────────────────────────

    /** JS: {@code Epiphany.getAptitude(player)} → long */
    static long getAptitude(ServerPlayer player) {
        return AptitudeManager.getAptitude(player);
    }

    /** JS: {@code Epiphany.getInsightPoints(player)} → int */
    static int getInsightPoints(ServerPlayer player) {
        return AptitudeManager.getInsightPoints(player);
    }

    /** JS: {@code Epiphany.getTotalInsightPointsSpent(player)} → int */
    static int getTotalInsightPointsSpent(ServerPlayer player) {
        return AptitudeManager.getTotalInsightPointsSpent(player);
    }

    /** Sets aptitude to an exact value. JS: {@code Epiphany.setAptitude(player, 500)} */
    static void setAptitude(ServerPlayer player, long value) {
        AptitudeManager.setAptitude(player, value);
    }

    /** Adds aptitude (may trigger level-ups). JS: {@code Epiphany.addAptitude(player, 100)} */
    static void addAptitude(ServerPlayer player, long amount) {
        AptitudeManager.addAptitude(player, amount);
    }

    /** Sets Insight Points to an exact value. JS: {@code Epiphany.setInsightPoints(player, 5)} */
    static void setInsightPoints(ServerPlayer player, int value) {
        AptitudeManager.setInsightPoints(player, value);
    }

    /** Calculates aptitude required for the next Insight Point. JS: {@code Epiphany.calcRequiredAptitude(totalSpent, points)} */
    static long calcRequiredAptitude(long totalSpent, int insightPoints) {
        return AptitudeFormula.calcRequiredAptitude(totalSpent, insightPoints);
    }

    // ─── Aptitude Source API (third-party behavior integration) ────

    /**
     * Pure resolution: returns the reward for one behavior+target pair without applying side effects.
     * JS: {@code var r = Epiphany.resolveAptitudeSource(player, id("epiphany","kill_entity"), id("minecraft","zombie"), null)}
     */
    static AptitudeSourceResolver.Resolution resolveAptitudeSource(
            ServerPlayer sp, ResourceLocation behaviorId, ResourceLocation targetId, @Nullable Registry<?> registry
    ) {
        return AptitudeSourceManager.resolve(sp, behaviorId, targetId, registry);
    }

    /**
     * Resolve + grant: applies multiplier, marks first_reward claim, fires aptitude events.
     * JS: {@code Epiphany.grantAptitude(player, id("epiphany","kill_entity"), id("minecraft","zombie"), null)}
     *
     * @return true if any aptitude was granted
     */
    static boolean grantAptitude(
            ServerPlayer sp, ResourceLocation behaviorId, ResourceLocation targetId, @Nullable Registry<?> registry
    ) {
        return AptitudeSourceManager.grant(sp, behaviorId, targetId, registry);
    }
}
