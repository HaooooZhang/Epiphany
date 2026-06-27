package ink.myumoon.epiphany;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Epiphany.MODID)
public class Epiphany {
    public static final String MODID = "epiphany";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Epiphany(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("Epiphany initialized");
    }
}
