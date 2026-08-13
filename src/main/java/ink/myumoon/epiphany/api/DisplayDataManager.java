package ink.myumoon.epiphany.api;

import ink.myumoon.epiphany.attachment.PlayerDisplayData;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Maintains player display-state invariants independently of gameplay managers. */
public final class DisplayDataManager {

    private DisplayDataManager() {
    }

    public static PlayerEpiphanyData recordModuleSelected(
            PlayerEpiphanyData data, ResourceLocation moduleId) {
        var order = new ArrayList<>(data.displayData().selectedModuleOrder());
        order.remove(moduleId);
        order.add(moduleId);
        return data.withDisplayData(data.displayData().withSelectedModuleOrder(order));
    }

    public static PlayerEpiphanyData removeModule(
            PlayerEpiphanyData data, ResourceLocation moduleId) {
        var order = new ArrayList<>(data.displayData().selectedModuleOrder());
        if (!order.removeIf(moduleId::equals)) return data;
        return data.withDisplayData(data.displayData().withSelectedModuleOrder(order));
    }

    /** Returns every selected module exactly once in stable display order. */
    public static List<ResourceLocation> orderedSelectedModules(PlayerEpiphanyData data) {
        Set<ResourceLocation> selectedIds = new HashSet<>();
        data.modules().forEach((id, state) -> {
            if (state.selected()) selectedIds.add(id);
        });

        var ordered = new ArrayList<ResourceLocation>(selectedIds.size());
        for (ResourceLocation id : data.displayData().selectedModuleOrder()) {
            if (selectedIds.remove(id)) ordered.add(id);
        }
        selectedIds.stream().sorted().forEach(ordered::add);
        return List.copyOf(ordered);
    }

    public static PlayerEpiphanyData recordEpiphanySelected(
            PlayerEpiphanyData data, ResourceLocation epiphanyId) {
        var order = new ArrayList<>(data.displayData().selectedEpiphanyOrder());
        order.remove(epiphanyId);
        order.add(epiphanyId);
        return data.withDisplayData(data.displayData().withSelectedEpiphanyOrder(order));
    }

    public static PlayerEpiphanyData removeEpiphany(
            PlayerEpiphanyData data, ResourceLocation epiphanyId) {
        var order = new ArrayList<>(data.displayData().selectedEpiphanyOrder());
        if (!order.removeIf(epiphanyId::equals)) return data;
        return data.withDisplayData(data.displayData().withSelectedEpiphanyOrder(order));
    }

    /** Returns every selected epiphany exactly once in stable display order. */
    public static List<ResourceLocation> orderedSelectedEpiphanies(PlayerEpiphanyData data) {
        Set<ResourceLocation> selectedIds = new HashSet<>();
        data.epiphanies().forEach((id, state) -> {
            if (state.selected()) selectedIds.add(id);
        });

        var ordered = new ArrayList<ResourceLocation>(selectedIds.size());
        for (ResourceLocation id : data.displayData().selectedEpiphanyOrder()) {
            if (selectedIds.remove(id)) ordered.add(id);
        }
        selectedIds.stream().sorted().forEach(ordered::add);
        return List.copyOf(ordered);
    }

    /** Repairs legacy or stale display data and writes it only when it changed. */
    public static void reconcile(ServerPlayer player) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        var registryAccess = player.server.registryAccess();

        // ── Module order ────────────────────────────────────────────
        var moduleRegistry = registryAccess.registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);

        var selectedModules = new HashSet<ResourceLocation>();
        data.modules().forEach((id, state) -> {
            if (state.selected() && moduleRegistry.containsKey(id)) selectedModules.add(id);
        });

        var reconciledModules = new ArrayList<ResourceLocation>(selectedModules.size());
        for (ResourceLocation id : data.displayData().selectedModuleOrder()) {
            if (selectedModules.remove(id)) reconciledModules.add(id);
        }
        selectedModules.stream().sorted().forEach(reconciledModules::add);

        // ── Epiphany order ──────────────────────────────────────────
        var epiphanyRegistry = registryAccess.registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);

        var selectedEpiphanies = new HashSet<ResourceLocation>();
        data.epiphanies().forEach((id, state) -> {
            if (state.selected() && epiphanyRegistry.containsKey(id)) selectedEpiphanies.add(id);
        });

        var reconciledEpiphanies = new ArrayList<ResourceLocation>(selectedEpiphanies.size());
        for (ResourceLocation id : data.displayData().selectedEpiphanyOrder()) {
            if (selectedEpiphanies.remove(id)) reconciledEpiphanies.add(id);
        }
        selectedEpiphanies.stream().sorted().forEach(reconciledEpiphanies::add);

        boolean moduleChanged = !reconciledModules.equals(data.displayData().selectedModuleOrder());
        boolean epiphanyChanged = !reconciledEpiphanies.equals(data.displayData().selectedEpiphanyOrder());
        if (moduleChanged || epiphanyChanged) {
            PlayerDisplayData displayData = data.displayData();
            if (moduleChanged) displayData = displayData.withSelectedModuleOrder(reconciledModules);
            if (epiphanyChanged) displayData = displayData.withSelectedEpiphanyOrder(reconciledEpiphanies);
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withDisplayData(displayData));
        }
    }
}