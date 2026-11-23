package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import ink.myumoon.epiphany.Epiphany;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import ink.myumoon.epiphany.client.ui.epiphany.EpiphanySelectController;
import ink.myumoon.epiphany.client.ui.epiphany.EpiphanySlotColumnController;
import ink.myumoon.epiphany.client.ui.module.ModuleGridController;
import ink.myumoon.epiphany.client.ui.module.ModuleSelectController;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client-only controller wiring for the main Epiphany UI. */
@OnlyIn(Dist.CLIENT)
final class EpiphanyUIClientHooks {

    private EpiphanyUIClientHooks() {
    }

    static ModularUI create(Player player) {
        var location = ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "ui/main.xml");
        var xml = XmlUtils.loadXml(location);
        if (xml == null) {
            Epiphany.LOGGER.error("Failed to load Epiphany main UI XML: {}", location);
            return ModularUI.of(UI.empty(), player);
        }
        var ui = UI.of(xml);
        attach(ui);
        return ModularUI.of(ui, player);
    }

    private static void attach(UI ui) {
        TopBarController.attach(ui);
        ModuleGridController.attach(ui);
        EpiphanySlotColumnController.attach(ui);
        ModuleSelectController.attach(ui);
        EpiphanySelectController.attach(ui);
    }
}
