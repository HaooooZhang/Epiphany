package ink.myumoon.epiphany.client.ui;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import ink.myumoon.epiphany.client.ui.epiphany.EpiphanySelectController;
import ink.myumoon.epiphany.client.ui.epiphany.EpiphanySlotColumnController;
import ink.myumoon.epiphany.client.ui.module.ModuleGridController;
import ink.myumoon.epiphany.client.ui.module.ModuleSelectController;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-only controller wiring for the main Epiphany UI. */
@OnlyIn(Dist.CLIENT)
final class EpiphanyUIClientHooks {

    private EpiphanyUIClientHooks() {
    }

    static void attach(UI ui) {
        TopBarController.attach(ui);
        ModuleGridController.attach(ui);
        EpiphanySlotColumnController.attach(ui);
        ModuleSelectController.attach(ui);
        EpiphanySelectController.attach(ui);
    }
}
