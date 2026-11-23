package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.EpiphanyPlayerData;

import ink.myumoon.epiphany.config.CommonConfig;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.condition.Condition;
import ink.myumoon.epiphany.content.reward.RewardListHelper;
import ink.myumoon.epiphany.event.*;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class EpiphanyManager {

    private EpiphanyManager() {
    }

    // registry handle
    private static Registry<EpiphanyData> epiphanyRegistry(ServerPlayer player) {
        return player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);
    }

    // query
    public static boolean isUnlocked(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyPlayerState state = EpiphanyPlayerData.get(player)
                .epiphanies().get(epiphanyId);
        if (state != null) return state.unlocked();
        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        return epiphany != null && epiphany.initialState() == InitialState.SELECTABLE;
    }

    public static boolean isSelected(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyPlayerState state = EpiphanyPlayerData.get(player)
                .epiphanies().get(epiphanyId);
        return state != null && state.selected();
    }

    // ─── Convenience getters (slots) ───────────────────────────────

    /** Slots the player has unlocked (one per completed Module, up to {@link #getMaxEpiphanySlots()}). */
    public static int getEpiphanySlots(ServerPlayer player) {
        return EpiphanyPlayerData.get(player).epiphanySlots();
    }

    /** Slots currently occupied by a selected Epiphany. */
    public static int getUsedEpiphanySlots(ServerPlayer player) {
        return EpiphanyPlayerData.get(player).usedEpiphanySlots();
    }

    /** Hard cap on selectable Epiphanies, from {@link CommonConfig#MAX_EPIPHANY_SLOTS}. */
    public static int getMaxEpiphanySlots() {
        return CommonConfig.MAX_EPIPHANY_SLOTS.get();
    }

    // unlock
    public static void setUnlocked(ServerPlayer player, ResourceLocation epiphanyId, boolean unlocked) {
        PlayerEpiphanyData data = EpiphanyPlayerData.get(player);
        EpiphanyPlayerState state = data.epiphanies()
                .getOrDefault(epiphanyId, EpiphanyPlayerState.createDefault());

        if (state.unlocked() == unlocked) return;

        if (unlocked) {
            EpiphanyUnlockEvent pre = new EpiphanyUnlockEvent(player, epiphanyId);
            MinecraftForge.EVENT_BUS.post(pre);
            if (pre.isCanceled()) return;
        }

        EpiphanyPlayerState newState = new EpiphanyPlayerState(state.selected(), unlocked);
        EpiphanyPlayerData.set(player, data.withEpiphanyState(epiphanyId, newState));

        if (unlocked) {
            MinecraftForge.EVENT_BUS.post(new EpiphanyUnlockedEvent(player, epiphanyId));
        }
    }

    // select
    public static void select(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        if (epiphany == null) return;

        PlayerEpiphanyData data = EpiphanyPlayerData.get(player);
        EpiphanyPlayerState state = data.epiphanies()
                .getOrDefault(epiphanyId, EpiphanyPlayerState.createDefault());

        if (!isUnlocked(player, epiphanyId)) return;
        if (state.selected()) return;

        int maxSlots = CommonConfig.MAX_EPIPHANY_SLOTS.get();
        if (data.usedEpiphanySlots() >= data.epiphanySlots()
                || data.usedEpiphanySlots() >= maxSlots) return;

        EpiphanySelectEvent pre = new EpiphanySelectEvent(player, epiphanyId);
        MinecraftForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;

        int newUsedSlots = data.usedEpiphanySlots() + 1;
        EpiphanyPlayerState newState = new EpiphanyPlayerState(true, true);  // select implies unlock
        PlayerEpiphanyData newData = data.withUsedEpiphanySlots(newUsedSlots)
                .withEpiphanyState(epiphanyId, newState);
        newData = DisplayDataManager.recordEpiphanySelected(newData, epiphanyId);

        EpiphanyPlayerData.set(player, newData);

        RewardListHelper.applyEpiphany(epiphany.reward(), player, epiphanyId);

        MinecraftForge.EVENT_BUS.post(new EpiphanySelectedEvent(player, epiphanyId));
    }

    // select (force)
    public static void forceSelect(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        if (epiphany == null) return;

        PlayerEpiphanyData data = EpiphanyPlayerData.get(player);
        EpiphanyPlayerState state = data.epiphanies()
                .getOrDefault(epiphanyId, EpiphanyPlayerState.createDefault());
        if (state.selected()) return;

        EpiphanyPlayerState newState = new EpiphanyPlayerState(true, true);
        PlayerEpiphanyData newData = data.withEpiphanyState(epiphanyId, newState)
                .withUsedEpiphanySlots(data.usedEpiphanySlots() + 1);
        newData = DisplayDataManager.recordEpiphanySelected(newData, epiphanyId);

        EpiphanyPlayerData.set(player, newData);

        RewardListHelper.applyEpiphany(epiphany.reward(), player, epiphanyId);
        MinecraftForge.EVENT_BUS.post(new EpiphanySelectedEvent(player, epiphanyId));
    }

    // reset
    public static void resetEpiphany(ServerPlayer player, ResourceLocation epiphanyId) {
        PlayerEpiphanyData data = EpiphanyPlayerData.get(player);
        EpiphanyPlayerState state = data.epiphanies().get(epiphanyId);
        if (state == null) return;

        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        if (epiphany != null && state.selected()) {
            RewardListHelper.removeEpiphany(epiphany.reward(), player, epiphanyId);
        }

        int refund = state.selected() ? 1 : 0;
        data = EpiphanyPlayerData.get(player);
        EpiphanyPlayerState newState = EpiphanyPlayerState.createDefault();
        PlayerEpiphanyData newData = data.withEpiphanyState(epiphanyId, newState)
                .withUsedEpiphanySlots(Math.max(0, data.usedEpiphanySlots() - refund));
        if (state.selected()) newData = DisplayDataManager.removeEpiphany(newData, epiphanyId);
        EpiphanyPlayerData.set(player, newData);
    }

    /**
     * Removes epiphany states whose registry entries no longer exist (e.g. after a
     * datapack update removed an Epiphany definition). Frees up used epiphany slots.
     * <p>
     * Limitation: rewards applied by the epiphany cannot be removed if the
     * EpiphanyData is gone; only persistent-wrapped rewards clean
     * themselves via their own respawn hook. Irreversible rewards are simply
     * discarded. Call on player login.
     */
    public static void cleanupOrphanedData(ServerPlayer player) {
        Registry<EpiphanyData> registry = epiphanyRegistry(player);
        PlayerEpiphanyData data = EpiphanyPlayerData.get(player);
        PlayerEpiphanyData newData = data;
        boolean changed = false;
        java.util.List<ResourceLocation> removedEpiphanies = new java.util.ArrayList<>();

        for (var entry : data.epiphanies().entrySet()) {
            ResourceLocation epiphanyId = entry.getKey();
            if (registry.containsKey(epiphanyId)) continue;

            EpiphanyPlayerState state = entry.getValue();
            if (state.selected()) {
                newData = newData.withUsedEpiphanySlots(Math.max(0, newData.usedEpiphanySlots() - 1));
            }
            newData = newData.withoutEpiphany(epiphanyId);
            newData = DisplayDataManager.removeEpiphany(newData, epiphanyId);
                removedEpiphanies.add(epiphanyId);
            changed = true;
        }

        if (changed) {
            EpiphanyPlayerData.set(player, newData);
                String playerName = player.getGameProfile().getName();
                Epiphany.LOGGER.warn("Removed orphaned Epiphany entries for player {}: {}",
                    playerName, removedEpiphanies);
        }
    }

    /** Evaluate conditions and auto-unlock matching epiphanies. */
    public static void checkAutoUnlock(ServerPlayer player) {
        checkAutoUnlock(player, false, false);
    }

    /**
     * @param skipEventDriven if true, skip conditions marked {@link Condition#isEventDriven()}
     *                        (used by polling to avoid redundant checks)
     */
    public static void checkAutoUnlock(ServerPlayer player, boolean skipEventDriven) {
        checkAutoUnlock(player, skipEventDriven, false);
    }

    /**
     * @param skipEventDriven if true, skip conditions marked {@link Condition#isEventDriven()}
     *                        (used by polling to avoid redundant checks)
     * @param silent          if true, the resulting {@link EpiphanyUnlockedEvent} will have
     *                        {@code isSilent() == true} so notification listeners can skip it
     */
    public static void checkAutoUnlock(ServerPlayer player, boolean skipEventDriven, boolean silent) {
        Registry<EpiphanyData> registry = epiphanyRegistry(player);
        PlayerEpiphanyData data = EpiphanyPlayerData.get(player);
        boolean changed = false;

        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            EpiphanyData epiphany = entry.getValue();

            if (epiphany.initialState() != InitialState.LOCKED) continue;
            if (epiphany.condition().isEmpty()) continue;

            EpiphanyPlayerState state = data.epiphanies().get(id);
            if (state != null && state.unlocked()) continue;

            Condition cond = epiphany.condition().get();
            if (skipEventDriven && cond.isEventDriven()) continue;
            if (cond.test(player)) {
                state = state != null ? state : EpiphanyPlayerState.createDefault();
                EpiphanyPlayerState newState = new EpiphanyPlayerState(state.selected(), true);
                data = data.withEpiphanyState(id, newState);
                MinecraftForge.EVENT_BUS.post(new EpiphanyUnlockedEvent(player, id, silent));
                changed = true;
            }
        }

        if (changed) EpiphanyPlayerData.set(player, data);
    }
}
