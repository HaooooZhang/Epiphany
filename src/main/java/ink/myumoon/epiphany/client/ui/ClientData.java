package ink.myumoon.epiphany.client.ui;

import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.ModuleData;
import ink.myumoon.epiphany.content.PathData;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Client-side read helpers for the Epiphany UI.
 * <p>
 * Centralizes two patterns used across all UI widgets:
 * <ul>
 *   <li>{@link #clientData()} — read the synced {@link PlayerEpiphanyData} mirror</li>
 *   <li>{@link #clientLookup(ResourceKey)} — read a datapack registry from the
 *       client's connection-level {@link RegistryAccess} (datapack registries
 *       are mirrored to the client automatically by vanilla)</li>
 * </ul>
 * <p>
 * <b>Why {@link HolderLookup.RegistryLookup} and not {@code Registry}?</b><br>
 * In 1.21.1, {@code RegistryAccess.lookup(key)} returns
 * {@code Optional<HolderLookup.RegistryLookup<T>>}. The returned object (an
 * anonymous {@code MappedRegistry$1}) implements
 * {@code HolderLookup.RegistryLookup<T>} but <b>not</b> {@code Registry<T>}
 * (the latter extends the former, not vice versa). Casting to
 * {@code Registry<T>} compiles but throws {@link ClassCastException} at
 * runtime. We therefore expose the {@code RegistryLookup} and helper methods
 * that unwrap {@link Holder.Reference#value()} for direct value access.
 */
public final class ClientData {

    private ClientData() {
    }

    /** The synced player Epiphany data, or null if the local player isn't ready. */
    @Nullable
    public static PlayerEpiphanyData clientData() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
    }

    /**
     * Lookup a datapack registry view on the client. Returns the vanilla
     * {@link HolderLookup.RegistryLookup} (read view), never the full
     * {@code Registry}. Strongly-typed {@code ResourceKey<Registry<T>>} is
     * required so the generic T is preserved across the lookup boundary.
     */
    @Nullable
    public static <T> HolderLookup.RegistryLookup<T> clientLookup(
            ResourceKey<Registry<T>> registryKey) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        return mc.level.registryAccess().lookup(registryKey).orElse(null);
    }

    /**
     * Convenience: fetch a single value by id from the mirrored datapack
     * registry on the client. Returns null if the client level isn't ready
     * OR the entry doesn't exist.
     */
    @Nullable
    public static <T> T getValue(ResourceKey<Registry<T>> registryKey, ResourceLocation id) {
        HolderLookup.RegistryLookup<T> lookup = clientLookup(registryKey);
        if (lookup == null) return null;
        // HolderGetter.get takes ResourceKey<T>; we synthesize one with id+registryKey.
        return lookup.get(ResourceKey.create(registryKey, id))
                .map(Holder.Reference::value)
                .orElse(null);
    }

    // ============================================================
    // Typed fast paths for the four Epiphany registries
    // ============================================================

    /** Convenience: get a single module definition by id on the client. */
    @Nullable
    public static ModuleData module(ResourceLocation id) {
        return getValue(EpiphanyRegistries.MODULE_REGISTRY_KEY, id);
    }

    /** Convenience: get a single insight definition by id on the client. */
    @Nullable
    public static InsightData insight(ResourceLocation id) {
        return getValue(EpiphanyRegistries.INSIGHT_REGISTRY_KEY, id);
    }

    /** Convenience: get a single epiphany definition by id on the client. */
    @Nullable
    public static EpiphanyData epiphany(ResourceLocation id) {
        return getValue(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY, id);
    }

    /** Convenience: get a single path definition by id on the client. */
    @Nullable
    public static PathData path(ResourceLocation id) {
        return getValue(EpiphanyRegistries.PATH_REGISTRY_KEY, id);
    }

    /** Module registry lookup view. Useful for iterating. */
    @Nullable
    public static HolderLookup.RegistryLookup<ModuleData> moduleLookup() {
        return clientLookup(EpiphanyRegistries.MODULE_REGISTRY_KEY);
    }

    /** Insight registry lookup view. Useful for iterating. */
    @Nullable
    public static HolderLookup.RegistryLookup<InsightData> insightLookup() {
        return clientLookup(EpiphanyRegistries.INSIGHT_REGISTRY_KEY);
    }

    /** Epiphany registry lookup view. Useful for iterating. */
    @Nullable
    public static HolderLookup.RegistryLookup<EpiphanyData> epiphanyLookup() {
        return clientLookup(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY);
    }

    /** Path registry lookup view. Useful for iterating. */
    @Nullable
    public static HolderLookup.RegistryLookup<PathData> pathLookup() {
        return clientLookup(EpiphanyRegistries.PATH_REGISTRY_KEY);
    }
}

