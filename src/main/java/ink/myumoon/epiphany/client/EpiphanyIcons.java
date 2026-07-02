package ink.myumoon.epiphany.client;

import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Three-level icon resolution for Epiphany UI.
 * <ol>
 *   <li>{@code data.icon()} — direct texture path</li>
 *   <li>Convention-based texture from the entry's registry ID</li>
 *   <li>Default vanilla item icon per entry type</li>
 * </ol>
 */
public final class EpiphanyIcons {

    // Default items per type
    private static final ItemStack DEFAULT_MODULE = new ItemStack(Items.WRITABLE_BOOK);
    private static final ItemStack DEFAULT_INSIGHT = new ItemStack(Items.DIAMOND);
    private static final ItemStack DEFAULT_EPIPHANY = new ItemStack(Items.GOAT_HORN);

    private EpiphanyIcons() {
    }

    /** Resolve the icon texture for a Module. */
    public static Optional<ResourceLocation> iconTexture(ModuleData data, ResourceLocation registryId) {
        return resolve(data.icon(), registryId, "module");
    }

    /** Resolve the icon texture for an Insight. */
    public static Optional<ResourceLocation> iconTexture(InsightData data, ResourceLocation registryId) {
        return resolve(data.icon(), registryId, "insight");
    }

    /** Resolve the icon texture for an Epiphany. */
    public static Optional<ResourceLocation> iconTexture(EpiphanyData data, ResourceLocation registryId) {
        return resolve(data.icon(), registryId, "epiphany");
    }

    /** Default item icon for a Module. */
    public static ItemStack defaultModule() { return DEFAULT_MODULE.copy(); }

    /** Default item icon for an Insight. */
    public static ItemStack defaultInsight() { return DEFAULT_INSIGHT.copy(); }

    /** Default item icon for an Epiphany. */
    public static ItemStack defaultEpiphany() { return DEFAULT_EPIPHANY.copy(); }

    // ============================================================
    // internal
    // ============================================================

    private static Optional<ResourceLocation> resolve(Optional<ResourceLocation> explicit,
                                                       ResourceLocation registryId,
                                                       String typeDir) {
        if (explicit.isPresent()) return explicit;
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                registryId.getNamespace(),
                "textures/gui/" + typeDir + "/" + registryId.getPath() + ".png"));
    }
}
