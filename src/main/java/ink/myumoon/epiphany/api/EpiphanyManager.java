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
        EpiphanyPlayerState state = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .epiphanies().get(epiphanyId);
        if (state != null) return state.unlocked();
        EpiphanyData epiphany = epiphanyRegistry(player).get(epiphanyId);
        return epiphany != null && epiphany.initialState() == InitialState.SELECTABLE;
    }

    public static boolean isSelected(ServerPlayer player, ResourceLocation epiphanyId) {
        EpiphanyPlayerState state = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)
                .epiphanies().get(epiphanyId);
        return state != null && state.selected();
    }

    // unlock
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

    // select
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

        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);

        epiphany.reward().ifPresent(r -> r.apply(player, epiphanyId));

        NeoForge.EVENT_BUS.post(new EpiphanySelectedEvent(player, epiphanyId));
    }

    // select (force)
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

        player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, newData);

        epiphany.reward().ifPresent(r -> r.apply(player, epiphanyId));
        NeoForge.EVENT_BUS.post(new EpiphanySelectedEvent(player, epiphanyId));
    }

    // reset
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

            if (epiphany.initialState() != InitialState.LOCKED) continue;
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
