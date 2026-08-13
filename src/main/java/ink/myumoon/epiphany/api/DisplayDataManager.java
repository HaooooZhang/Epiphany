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

    /** Repairs legacy or stale display data and writes it only when it changed. */
    public static void reconcile(ServerPlayer player) {
        PlayerEpiphanyData data = player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
        var registry = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);

        var selectedIds = new HashSet<ResourceLocation>();
        data.modules().forEach((id, state) -> {
            if (state.selected() && registry.containsKey(id)) selectedIds.add(id);
        });

        var reconciled = new ArrayList<ResourceLocation>(selectedIds.size());
        for (ResourceLocation id : data.displayData().selectedModuleOrder()) {
            if (selectedIds.remove(id)) reconciled.add(id);
        }
        selectedIds.stream().sorted().forEach(reconciled::add);

        if (!reconciled.equals(data.displayData().selectedModuleOrder())) {
            PlayerDisplayData displayData = data.displayData().withSelectedModuleOrder(reconciled);
            player.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, data.withDisplayData(displayData));
        }
    }
}