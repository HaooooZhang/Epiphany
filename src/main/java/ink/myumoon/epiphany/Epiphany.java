package ink.myumoon.epiphany;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyConditionTypes;
import ink.myumoon.epiphany.registry.EpiphanyEpiphanyRewardTypes;
import ink.myumoon.epiphany.registry.EpiphanyInsightRewardTypes;
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
        EpiphanyAttachmentTypes.REGISTRY.register(modEventBus);
        EpiphanyConditionTypes.REGISTRY.register(modEventBus);
        EpiphanyInsightRewardTypes.REGISTRY.register(modEventBus);
        EpiphanyEpiphanyRewardTypes.REGISTRY.register(modEventBus);

        LOGGER.info("Epiphany initialized");
    }
}
