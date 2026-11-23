package ink.myumoon.epiphany.client;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import ink.myumoon.epiphany.client.ui.ItemIconElement;
import ink.myumoon.epiphany.content.EpiphanyData;
import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.content.ModuleData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
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
    public static ResolvedIcon resolve(ModuleData data, ResourceLocation registryId) {
        return resolve(data.icon(), data.itemIcon(), registryId, "module", DEFAULT_MODULE);
    }

    // Insight
    public static ResolvedIcon resolve(InsightData data, ResourceLocation registryId) {
        return resolve(data.icon(), data.itemIcon(), registryId, "insight", DEFAULT_INSIGHT);
    }

    // Epiphany
    public static ResolvedIcon resolve(EpiphanyData data, ResourceLocation registryId) {
        return resolve(data.icon(), data.itemIcon(), registryId, "epiphany", DEFAULT_EPIPHANY);
    }

    // Module Default
    public static ItemStack defaultModule() { return DEFAULT_MODULE.copy(); }

    // Insight Default
    public static ItemStack defaultInsight() { return DEFAULT_INSIGHT.copy(); }

    // Epiphany Default
    public static ItemStack defaultEpiphany() { return DEFAULT_EPIPHANY.copy(); }

    public static UIElement createElement(ResolvedIcon resolvedIcon) {
        UIElement element;
        if (resolvedIcon instanceof TextureIcon textureIcon) {
            element = new UIElement();
            element.style(style -> style.background(SpriteTexture.of(textureIcon.texture())));
        } else if (resolvedIcon instanceof ItemIcon itemIcon) {
            element = new ItemIconElement(itemIcon.stack());
        } else {
            throw new IllegalStateException("Unknown resolved icon: " + resolvedIcon);
        }
        element.layout(layout -> layout.width(16).height(16));
        return element;
    }

    // Internal Handle
    private static ResolvedIcon resolve(Optional<ResourceLocation> explicit,
                                        Optional<ResourceLocation> itemIcon,
                                        ResourceLocation registryId,
                                        String typeDir,
                                        ItemStack fallback) {
        if (explicit.filter(EpiphanyIcons::resourceExists).isPresent()) {
            return new TextureIcon(explicit.orElseThrow());
        }
        var item = itemIcon.flatMap(BuiltInRegistries.ITEM::getOptional);
        if (item.isPresent() && item.get() != Items.AIR) {
            return new ItemIcon(new ItemStack(item.get()));
        }
        var conventional = ResourceLocation.fromNamespaceAndPath(
                registryId.getNamespace(),
                "textures/gui/" + typeDir + "/" + registryId.getPath() + ".png");
        if (resourceExists(conventional)) {
            return new TextureIcon(conventional);
        }
        return new ItemIcon(fallback.copy());
    }

    private static boolean resourceExists(ResourceLocation location) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public sealed interface ResolvedIcon permits TextureIcon, ItemIcon {}

    public record TextureIcon(ResourceLocation texture) implements ResolvedIcon {}

    public record ItemIcon(ItemStack stack) implements ResolvedIcon {}
}
