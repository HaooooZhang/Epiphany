package ink.myumoon.epiphany;

import com.mojang.logging.LogUtils;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerData;
import ink.myumoon.epiphany.command.R1Probe;
import ink.myumoon.epiphany.network.EpiphanyNetwork;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Epiphany.MODID)
public final class Epiphany {
    public static final String MODID = "epiphany";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Epiphany(FMLJavaModLoadingContext context) {
        var modBus = context.getModEventBus();
        var gameBus = MinecraftForge.EVENT_BUS;
        EpiphanyRegistries.register(modBus);
        EpiphanyPlayerData.register(modBus, gameBus);
        EpiphanyNetwork.register(gameBus);
        if (!FMLEnvironment.production) {
            R1Probe.register(gameBus);
        }
    }
}
