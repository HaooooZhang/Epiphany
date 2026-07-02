package ink.myumoon.epiphany.client.ui.insight;

import ink.myumoon.epiphany.content.InsightData;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Insight icon resolution — mirrors EpiphanyIcons but for Insights.
 * Falls back to default icon if PNG doesn't exist.
 */
public final class InsightIcons {

    private InsightIcons() {
    }

    public static Optional<ResourceLocation> iconTexture(InsightData data, ResourceLocation registryId) {
        if (data.icon().isPresent()) return data.icon();
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                registryId.getNamespace(),
                "textures/gui/insight/" + registryId.getPath() + ".png"));
    }

    public static boolean resourceExists(ResourceLocation rl) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(rl).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
