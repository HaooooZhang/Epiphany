package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.api.AptitudeSourceManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Aptitude-source listeners that require per-player persistent state.
 * <p>
 * Wires two behaviors:
 * <ul>
 *   <li>{@code epiphany:enter_biome} — fires when the player's current biome changes
 *       (excludes first entry on login). Uses {@link EntityEvent.EnteringSection} for
 *       immediate reaction on chunk crossing, with a {@link ServerTickEvent.Post}
 *       fallback poll.</li>
 *   <li>{@code epiphany:enter_structure} — fires when the player's "inside structure"
 *       membership changes. ''Inside'' = at least one Structure from the live registry
 *       returns valid via {@code structureManager().getStructureWithPieceAt}. When the
 *       player is outside any structure, a sentinel id {@code epiphany:#none} is emitted,
 *       so JSON authors can either give a "leave structure" default reward (if any) or
 *       just leave default=0 to make it a no-op.</li>
 * </ul>
 * <p>
 * State is stored on the player's persistent NBT under an {@code epiphany/} sub-tag,
 * so it survives death, log-out, and world reload without polluting
 * {@code PlayerEpiphanyData}.
 */
@EventBusSubscriber(modid = Epiphany.MODID)
public final class AptitudeStateListener {

    public static final ResourceLocation ENTER_BIOME =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "enter_biome");
    public static final ResourceLocation ENTER_STRUCTURE =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "enter_structure");

    /** Sentinel target id emitted when the player is not inside any structure. */
    public static final ResourceLocation NONE_STRUCTURE =
            ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "none");

    private static final String NBT_ROOT = "epiphany";
    private static final String NBT_LAST_BIOME = "last_biome";
    private static final String NBT_LAST_STRUCT = "last_structure";

    private static final int POLL_INTERVAL_TICKS = 40;   // 2 s

    private static int tickCounter;

    private AptitudeStateListener() {
    }

    // ============================================================
    // Init — align last-known biome/structure on login so first poll doesn't fire
    // ============================================================

    @SubscribeEvent
    static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        // Force-align to current biome/structure so first poll doesn't emit rewards.
        CompoundTag root = stateTag(sp);
        if (sp.level().getBiome(sp.blockPosition()) instanceof Holder<Biome> holder) {
            holder.unwrapKey().map(ResourceKey::location).ifPresent(loc ->
                    root.putString(NBT_LAST_BIOME, loc.toString()));
        }
        root.putString(NBT_LAST_STRUCT, currentStructureId(sp).toString());
    }

    // ============================================================
    // Event-driven: chunk-section move → check biome/structure change
    // ============================================================

    @SubscribeEvent
    static void onEnteringSection(EntityEvent.EnteringSection event) {
        if (!event.didChunkChange()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        checkBiomeChange(sp);
        checkStructureChange(sp);
    }

    // ============================================================
    // Fallback poll — covers teleport and any case EnteringSection misses
    // ============================================================

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter % POLL_INTERVAL_TICKS != 0) return;
        for (var sp : event.getServer().getPlayerList().getPlayers()) {
            checkBiomeChange(sp);
            checkStructureChange(sp);
        }
    }

    // ============================================================
    // Biome detection
    // ============================================================

    private static void checkBiomeChange(ServerPlayer sp) {
        Holder<Biome> holder = sp.level().getBiome(sp.blockPosition());
        ResourceLocation currentId = holder.unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);
        if (currentId == null) return;

        CompoundTag root = stateTag(sp);
        String previous = root.contains(NBT_LAST_BIOME) ? root.getString(NBT_LAST_BIOME) : null;
        if (currentId.toString().equals(previous)) return;

        // BIOME registry is fetched from the live level registryAccess so mod/data
        // tags resolve properly (matches BiomeCondition's lookup approach).
        Registry<Biome> biomeRegistry = sp.level().registryAccess().registryOrThrow(Registries.BIOME);
        AptitudeSourceManager.grant(sp, ENTER_BIOME, currentId, biomeRegistry);
        root.putString(NBT_LAST_BIOME, currentId.toString());
    }

    // ============================================================
    // Structure detection
    // ============================================================

    private static void checkStructureChange(ServerPlayer sp) {
        ResourceLocation currentId = currentStructureId(sp);
        CompoundTag root = stateTag(sp);
        String previous = root.contains(NBT_LAST_STRUCT) ? root.getString(NBT_LAST_STRUCT) : null;
        if (currentId.toString().equals(previous)) return;

        // STRUCTURE registry: tag-aware. When outside any structure (sentinel), the
        // matcher falls back to plain-id comparison against '#none' which JSON authors
        // can match in specials / exclude as they please.
        Registry<Structure> structRegistry = sp.level().registryAccess().registryOrThrow(Registries.STRUCTURE);
        AptitudeSourceManager.grant(sp, ENTER_STRUCTURE, currentId, structRegistry);
        root.putString(NBT_LAST_STRUCT, currentId.toString());
    }

    /**
     * Returns the first structure id whose bounding box covers the player's current
     * position, or {@link #NONE_STRUCTURE} if none do.
     */
    private static ResourceLocation currentStructureId(ServerPlayer sp) {
        var mgr = sp.serverLevel().structureManager();
        Registry<Structure> registry = sp.level().registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (var entry : registry.entrySet()) {
            var start = mgr.getStructureWithPieceAt(sp.blockPosition(), entry.getValue());
            if (start.isValid()) {
                return entry.getKey().location();
            }
        }
        return NONE_STRUCTURE;
    }

    // ============================================================
    // NBT helpers
    // ============================================================

    /** Returns the per-player {@code epiphany/} sub-tag (auto-created). */
    private static CompoundTag stateTag(ServerPlayer sp) {
        CompoundTag persistent = sp.getPersistentData();
        if (persistent.contains(NBT_ROOT, CompoundTag.TAG_COMPOUND)) {
            return persistent.getCompound(NBT_ROOT);
        }
        CompoundTag root = new CompoundTag();
        persistent.put(NBT_ROOT, root);
        return root;
    }
}
