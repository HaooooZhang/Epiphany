package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.Config;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerState;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.event.*;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Manages Epiphany lifecycle: unlock, select, and slot management.
 */
public final class EpiphanyManager {

    private EpiphanyManager() {
    }

    // ============================================================
    // Registry helper
    // ============================================================

    private static Registry<EpiphanyData> epiphanyRegistry(ServerPlayer player) {
        return player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);
    }

    // ============================================================
    // Queries
    // ============================================================

    public static boolean isUnlocked(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyPlayerState state = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .epiphanies().get(epiphanyId);
        if (state != null && state.unlocked()) return true;
        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        if (epiphany == null) return false;
        if (state == null && epiphany.initialState() == InitialState.SELECTABLE
                && epiphany.condition().isEmpty()) return true;
        return epiphany.condition().map(c -> c.test(player)).orElse(false);
    }

    public static boolean isSelected(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyPlayerState state = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .epiphanies().get(epiphanyId);
        return state != null && state.selected();
    }

    // ============================================================
    // Mutations
    // ============================================================

    /**
     * Manually set the unlocked state, firing Pre/Post events.
     */
    public static void setUnlocked(ServerPlayer player, ResourceLocation epiphanyId, boolean unlocked) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        EpiphanyPlayerState state = data.epiphanies()
                .getOrDefault(epiphanyId, EpiphanyPlayerState.createDefault());

        if (state.unlocked() == unlocked) return;

        if (unlocked) {
            EpiphanyUnlockEvent pre = new EpiphanyUnlockEvent(player, epiphanyId);
            NeoForge.EVENT_BUS.post(pre);
            if (pre.isCanceled()) return;
        }

        EpiphanyPlayerState newState = new EpiphanyPlayerState(state.selected(), unlocked);
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                data.withEpiphanyState(epiphanyId, newState));

        if (unlocked) {
            NeoForge.EVENT_BUS.post(new EpiphanyUnlockedEvent(player, epiphanyId));
        }
    }

    /**
     * Selects an Epiphany into a slot. Checks slot availability,
     * applies the reward, and increments the used slot counter.
     */
    public static void select(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        if (epiphany == null) return;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        EpiphanyPlayerState state = data.epiphanies()
                .getOrDefault(epiphanyId, EpiphanyPlayerState.createDefault());

        if (!isUnlocked(player, epiphanyId)) return;
        if (state.selected()) return;

        int maxSlots = Config.MAX_EPIPHANY_SLOTS.get();
        if (data.usedEpiphanySlots() >= data.epiphanySlots()
                || data.usedEpiphanySlots() >= maxSlots) return;

        EpiphanySelectEvent pre = new EpiphanySelectEvent(player, epiphanyId);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;

        int newUsedSlots = data.usedEpiphanySlots() + 1;
        EpiphanyPlayerState newState = new EpiphanyPlayerState(true, true);  // select implies unlock
        PlayerEpiphanyData newData = data.withUsedEpiphanySlots(newUsedSlots)
                .withEpiphanyState(epiphanyId, newState);

        epiphany.reward().ifPresent(r -> r.apply(player, epiphanyId));

        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);
        NeoForge.EVENT_BUS.post(new EpiphanySelectedEvent(player, epiphanyId));
    }

    /** Admin: force-select an Epiphany, ignoring slot limits. */
    public static void forceSelect(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        if (epiphany == null) return;

        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        EpiphanyPlayerState state = data.epiphanies()
                .getOrDefault(epiphanyId, EpiphanyPlayerState.createDefault());
        if (state.selected()) return;

        EpiphanyPlayerState newState = new EpiphanyPlayerState(true, true);
        PlayerEpiphanyData newData = data.withEpiphanyState(epiphanyId, newState)
                .withUsedEpiphanySlots(data.usedEpiphanySlots() + 1);

        epiphany.reward().ifPresent(r -> r.apply(player, epiphanyId));
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);
        NeoForge.EVENT_BUS.post(new EpiphanySelectedEvent(player, epiphanyId));
    }

    /** Admin: reset a single Epiphany, removing it from the slot and stopping its reward. */
    public static void resetEpiphany(ServerPlayer player, ResourceLocation epiphanyId) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        EpiphanyPlayerState state = data.epiphanies().get(epiphanyId);
        if (state == null) return;

        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        if (epiphany != null && state.selected()) {
            epiphany.reward().ifPresent(r -> r.remove(player, epiphanyId));
        }

        int refund = state.selected() ? 1 : 0;
        EpiphanyPlayerState newState = EpiphanyPlayerState.createDefault();
        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                data.withEpiphanyState(epiphanyId, newState)
                        .withUsedEpiphanySlots(Math.max(0, data.usedEpiphanySlots() - refund)));
    }

    /** Periodically evaluate conditions and auto-unlock matching epiphanies. */
    public static void checkAutoUnlock(ServerPlayer player) {
        Registry<EpiphanyData> registry = epiphanyRegistry(player);
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        boolean changed = false;

        for (var entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            EpiphanyData epiphany = entry.getValue();
            if (epiphany.condition().isEmpty()) continue;

            EpiphanyPlayerState state = data.epiphanies().get(id);
            if (state != null && state.unlocked()) continue;

            if (epiphany.condition().get().test(player)) {
                state = state != null ? state : EpiphanyPlayerState.createDefault();
                EpiphanyPlayerState newState = new EpiphanyPlayerState(state.selected(), true);
                data = data.withEpiphanyState(id, newState);
                NeoForge.EVENT_BUS.post(new EpiphanyUnlockedEvent(player, id));
                changed = true;
            }
        }

        if (changed) player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data);
    }
}
