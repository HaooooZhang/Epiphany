package ink.myumoon.epiphany.client.ui.progression;

import ink.myumoon.epiphany.config.ProgressionConfig;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/** Native configuration list with the custom editor for growth rules. */
@OnlyIn(Dist.CLIENT)
public final class EpiphanyConfigurationScreen {
    private EpiphanyConfigurationScreen() {}

    public static Screen create(ModContainer container, Screen parent) {
        return new ConfigurationScreen(container, parent, (screen, type, config, title) ->
                config.getSpec() == ProgressionConfig.SPEC
                        ? new CurveEditorScreen(screen)
                        : new ConfigurationScreen.ConfigurationSectionScreen(screen, type, config, title));
    }
}
