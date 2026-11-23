package ink.myumoon.epiphany.content;

import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Utility for navigating the Insight tree within a Module.
 * <p>
 * Insights are arranged by {@code depth}: all depth-0 Insights form the root layer
 * (AND-related), depth-1 requires all depth-0 to be unlocked, etc.
 * A depth-N Insight's parent is the nearest preceding depth-(N-1) entry in the array.
 */
public final class InsightTreeResolver {

    private InsightTreeResolver() {
    }

    /**
     * Finds the parent Insight ID for a given Insight within a Module.
     * <p>
     * Searches backward from the target's position for the nearest
     * preceding entry with {@code depth = targetDepth - 1}.
     *
     * @param module    the module definition
     * @param insightId the insight whose parent to find
     * @return the parent's ID, or empty if depth is 0 or no parent found
     */
    public static Optional<ResourceLocation> findParent(ModuleData module, ResourceLocation insightId) {
        List<InsightEntry> entries = module.insights();
        int targetIndex = indexOf(entries, insightId);
        if (targetIndex < 0) return Optional.empty();

        int targetDepth = entries.get(targetIndex).depth();
        if (targetDepth <= 0) return Optional.empty();

        // Search backward for the first entry with depth = targetDepth - 1
        for (int i = targetIndex - 1; i >= 0; i--) {
            if (entries.get(i).depth() == targetDepth - 1) {
                return Optional.of(entries.get(i).id());
            }
        }
        return Optional.empty();
    }

    /**
     * Finds all ancestor Insight IDs (all lower depths) for a given Insight.
     *
     * @param module    the module definition
     * @param insightId the insight whose ancestors to find
     * @return set of all ancestor IDs (empty for depth-0)
     */
    public static Set<ResourceLocation> findAncestors(ModuleData module, ResourceLocation insightId) {
        List<InsightEntry> entries = module.insights();
        int targetIndex = indexOf(entries, insightId);
        if (targetIndex < 0) return Set.of();

        int targetDepth = entries.get(targetIndex).depth();
        if (targetDepth <= 0) return Set.of();

        // Collect all unique IDs that appear before the target with strictly lower depth
        Set<ResourceLocation> ancestors = new LinkedHashSet<>();
        for (int i = 0; i < targetIndex; i++) {
            InsightEntry entry = entries.get(i);
            if (entry.depth() < targetDepth) {
                ancestors.add(entry.id());
            }
        }
        return Collections.unmodifiableSet(ancestors);
    }

    /**
     * Checks whether a player can unlock a specific Insight.
     * <p>
     * Requirements:
     * <ol>
     *   <li>The parent module is selected</li>
     *   <li>All ancestor Insights (lower depth) are already unlocked</li>
     *   <li>The insight itself is not yet unlocked</li>
     * </ol>
     *
     * @param data      the player's Epiphany data
     * @param moduleId  the registry ID of the module containing the insight
     * @param module    the module definition
     * @param insightId the insight to check
     * @return true if all requirements are met
     */
    public static boolean canUnlock(PlayerEpiphanyData data, ResourceLocation moduleId,
                                     ModuleData module, ResourceLocation insightId) {
        ModulePlayerState moduleState = data.modules().get(moduleId);
        if (moduleState == null || !moduleState.selected()) return false;

        // Already unlocked?
        if (moduleState.unlockedInsights().contains(insightId)) return false;

        // All ancestors must be unlocked
        Set<ResourceLocation> ancestors = findAncestors(module, insightId);
        return moduleState.unlockedInsights().containsAll(ancestors);
    }

    private static int indexOf(List<InsightEntry> entries, ResourceLocation id) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(id)) return i;
        }
        return -1;
    }
}
