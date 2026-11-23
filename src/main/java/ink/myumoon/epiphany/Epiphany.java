package ink.myumoon.epiphany;

import com.mojang.logging.LogUtils;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerData;
import ink.myumoon.epiphany.client.ui.EpiphanyUIFactory;
import ink.myumoon.epiphany.command.R1Probe;
import ink.myumoon.epiphany.command.EpiphanyCommand;
import ink.myumoon.epiphany.config.CommonConfig;
import ink.myumoon.epiphany.config.ProgressionConfig;
import ink.myumoon.epiphany.content.reward.PersistentReward;
import ink.myumoon.epiphany.content.condition.builtin.ftbq.FTBQHelper;
import ink.myumoon.epiphany.registry.EpiphanyAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
        modBus.addListener(EpiphanyNetwork::onCommonConfigReload);
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ProgressionConfig.register(context);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            EpiphanyUIFactory.register();
            FTBQHelper.init();
        }));
        EpiphanyAttributes.ATTRIBUTES.register(modBus);
        modBus.addListener((EntityAttributeModificationEvent event) ->
                event.add(EntityType.PLAYER, EpiphanyAttributes.APTITUDE_GAIN_MULTIPLIER.get()));
        gameBus.addListener((RegisterCommandsEvent event) -> EpiphanyCommand.register(event.getDispatcher()));
        gameBus.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                PersistentReward.reapplyAll(player);
                player.setHealth(player.getMaxHealth());
            }
        });
        if (!FMLEnvironment.production) {
            R1Probe.register(gameBus);
        }
    }
}
