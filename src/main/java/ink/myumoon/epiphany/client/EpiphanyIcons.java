package ink.myumoon.epiphany.client;

import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

// Icon Handle
public final class EpiphanyIcons {

    // Default items per type
    private static final ItemStack DEFAULT_MODULE = new ItemStack(Items.WRITABLE_BOOK);
    private static final ItemStack DEFAULT_INSIGHT = new ItemStack(Items.DIAMOND);
    private static final ItemStack DEFAULT_EPIPHANY = new ItemStack(Items.GOAT_HORN);

    private EpiphanyIcons() {
    }

    // Module
    public static Optional<ResourceLocation> iconTexture(ModuleData data, ResourceLocation registryId) {
        return resolve(data.icon(), registryId, "module");
    }

    // Insight
    public static Optional<ResourceLocation> iconTexture(InsightData data, ResourceLocation registryId) {
        return resolve(data.icon(), registryId, "insight");
    }

    // Epiphany
    public static Optional<ResourceLocation> iconTexture(EpiphanyData data, ResourceLocation registryId) {
        return resolve(data.icon(), registryId, "epiphany");
    }

    // Module Default
    public static ItemStack defaultModule() { return DEFAULT_MODULE.copy(); }

    // Insight Default
    public static ItemStack defaultInsight() { return DEFAULT_INSIGHT.copy(); }

    // Epiphany Default
    public static ItemStack defaultEpiphany() { return DEFAULT_EPIPHANY.copy(); }

    // Internal Handle
    private static Optional<ResourceLocation> resolve(Optional<ResourceLocation> explicit,
                                                       ResourceLocation registryId,
                                                       String typeDir) {
        if (explicit.isPresent()) return explicit;
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                registryId.getNamespace(),
                "textures/gui/" + typeDir + "/" + registryId.getPath() + ".png"));
    }
}
