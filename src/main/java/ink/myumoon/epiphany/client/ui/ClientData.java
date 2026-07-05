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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Client-side read helpers for the Epiphany UI.
 * <p>
 * Two common patterns:
 * <ul>
 *   <li>{@link #clientData()} — read the synced {@link PlayerEpiphanyData} attachment</li>
 *   <li>{@link #clientLookup(ResourceKey)} — fetch a datapack registry view from the client's registry access</li>
 * </ul>
 * <p>
 * <b>Thread safety:</b> These helpers access {@code Minecraft.getInstance().player} (a {@link LocalPlayer})
 * and are safe only in <b>client-only contexts</b> (e.g., tooltips, hover events).
 * They must NOT be used inside {@link com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder}
 * S→C getters — those lambdas execute on the server thread. For server-side reads, fetch the
 * {@code ServerPlayer} directly and use {@code player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA)}.
 * <p>
 * <b>Why {@link HolderLookup.RegistryLookup} instead of {@link Registry}?</b>
 * In 1.21.1, {@code RegistryAccess.lookup(key)} returns {@code HolderLookup.RegistryLookup<T>},
 * not {@code Registry<T>}. Casting to {@code Registry} compiles but throws {@link ClassCastException}
 * at runtime.
 */
public final class ClientData {

    private ClientData() {
    }

    @Nullable
    public static PlayerEpiphanyData clientData() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        return player.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
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
