package ink.myumoon.epiphany.client.ui;

import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.attachment.PlayerDisplayData;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.content.PathData;
import ink.myumoon.epiphany.client.ClientProgress;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import ink.myumoon.epiphany.config.ProgressionRules;
import net.minecraft.client.Minecraft;
import ink.myumoon.epiphany.config.SelectionRules;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** Client-side views of Epiphany snapshots and the current connection registries. */
public final class ClientData {

    private ClientData() {
    }

    @Nullable
    public static PlayerEpiphanyData clientData() {
        return ClientProgress.selectionRules().isEmpty() ? null : ClientProgress.progress().orElse(null);
    }

    public static SelectionRules selectionRules() {
        return ClientProgress.selectionRules().orElseThrow();
    }

    @Nullable
    public static ProgressionRules progressionRules() {
        return ClientProgress.rules().orElse(null);
    }

    @Nullable
    public static PlayerDisplayData displayData() {
        PlayerEpiphanyData data = clientData();
        return data != null ? data.displayData() : null;
    }

    /**
     * Returns the client-side datapack registry view for the given key, or null if not available.
     */
    @Nullable
    public static <T> HolderLookup.RegistryLookup<T> clientLookup(
            ResourceKey<Registry<T>> registryKey) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        return mc.level.registryAccess().lookup(registryKey).orElse(null);
    }

    /**
     * Fetches a single value by ID from a client registry view. Returns null if the lookup fails.
     */
    @Nullable
    public static <T> T getValue(ResourceKey<Registry<T>> registryKey, ResourceLocation id) {
        HolderLookup.RegistryLookup<T> lookup = clientLookup(registryKey);
        if (lookup == null) return null;
        return lookup.get(ResourceKey.create(registryKey, id))
                .map(Holder.Reference::value)
                .orElse(null);
    }

    // Typed convenience methods for the four Epiphany registries
    @Nullable
    public static ModuleData module(ResourceLocation id) {
        return getValue(EpiphanyRegistries.MODULE_REGISTRY_KEY, id);
    }

    @Nullable
    public static InsightData insight(ResourceLocation id) {
        return getValue(EpiphanyRegistries.INSIGHT_REGISTRY_KEY, id);
    }

    @Nullable
    public static EpiphanyData epiphany(ResourceLocation id) {
        return getValue(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY, id);
    }

    @Nullable
    public static PathData path(ResourceLocation id) {
        return getValue(EpiphanyRegistries.PATH_REGISTRY_KEY, id);
    }

    @Nullable
    public static HolderLookup.RegistryLookup<ModuleData> moduleLookup() {
        return clientLookup(EpiphanyRegistries.MODULE_REGISTRY_KEY);
    }

    @Nullable
    public static HolderLookup.RegistryLookup<InsightData> insightLookup() {
        return clientLookup(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
    }

    @Nullable
    public static HolderLookup.RegistryLookup<EpiphanyData> epiphanyLookup() {
        return clientLookup(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);
    }

    @Nullable
    public static HolderLookup.RegistryLookup<PathData> pathLookup() {
        return clientLookup(EpiphanyRegistries.PATH_REGISTRY_KEY);
    }
}
