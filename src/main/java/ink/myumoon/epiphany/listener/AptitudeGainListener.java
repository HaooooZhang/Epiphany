package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.AptitudeSourceManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Epiphany's built-in game-event listeners for aptitude sources.
 * <p>
 * Wires two built-in behaviors defined in {@code data/epiphany/epiphany/aptitude/}:
 * <ul>
 *   <li>{@code epiphany:kill_entity} — fired by {@link LivingDeathEvent} when the
 *       killer is a {@link ServerPlayer}</li>
 *   <li>{@code epiphany:mine_block}  — fired by {@link BlockEvent.BreakEvent}</li>
 * </ul>
 * <p>
 * <strong>Third-party mods</strong>: do NOT add methods here. Instead register
 * your own NeoForge listener in your own mod and call
 * {@link AptitudeSourceManager#grant}. See {@code Docs/README.md} for a worked example.
 */
@EventBusSubscriber(modid = Epiphany.MODID)
public final class AptitudeGainListener {

    public static final ResourceLocation KILL_ENTITY =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "kill_entity");
    public static final ResourceLocation MINE_BLOCK =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "mine_block");

    private AptitudeGainListener() {
    }

    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;

        EntityType<?> type = event.getEntity().getType();
        ResourceLocation targetId = BuiltInRegistries.ENTITY_TYPE.getKey(type);

        AptitudeSourceManager.grant(sp, KILL_ENTITY, targetId, BuiltInRegistries.ENTITY_TYPE);
    }

    @SubscribeEvent
    static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;

        ResourceLocation targetId = event.getState().getBlockHolder().unwrapKey()
                .map(k -> k.location())
                .orElse(null);
        if (targetId == null) return;

        AptitudeSourceManager.grant(sp, MINE_BLOCK, targetId, BuiltInRegistries.BLOCK);
    }
}
