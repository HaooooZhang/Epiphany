package ink.myumoon.epiphany;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import ink.myumoon.epiphany.api.ModuleManager;
import ink.myumoon.epiphany.client.ui.EpiphanyUIFactory;
import ink.myumoon.epiphany.command.EpiphanyCommand;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyConditionTypes;
import ink.myumoon.epiphany.registry.EpiphanyEpiphanyRewardTypes;
import ink.myumoon.epiphany.registry.EpiphanyInsightRewardTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

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

        // Register the main UI on both sides. PlayerUIMenuType.register must
        // happen after registries, so defer to common setup via enqueueWork.
        modEventBus.addListener(FMLCommonSetupEvent.class, event ->
                event.enqueueWork(EpiphanyUIFactory::register));

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                EpiphanyCommand.register(event.getDispatcher()));

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event ->
                ModuleManager.checkAutoUnlock((net.minecraft.server.level.ServerPlayer) event.getEntity()));

        NeoForge.EVENT_BUS.addListener(AdvancementEvent.AdvancementEarnEvent.class, event ->
                ModuleManager.checkAutoUnlock((net.minecraft.server.level.ServerPlayer) event.getEntity()));

        LOGGER.info("Epiphany initialized");
    }
}
