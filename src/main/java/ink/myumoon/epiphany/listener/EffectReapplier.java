package ink.myumoon.epiphany.listener;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.content.reward.EffectReward;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Protects permanent effects (duration = -1) authored by {@link EffectReward}
 * from being cleared by in-game cures (milk buckets, totem of undying, etc.).
 * <p>
 * Permanent effect records live in the player's persistent NBT under the key
 * {@code epiphany_permanent_effects} as {@code "id|amplifier"} strings, written
 * by {@link EffectReward#apply} when a duration of {@code -1} is applied.
 * <p>
 * This class is event-driven (no per-tick polling):
 * <ul>
 *   <li>{@link MobEffectEvent.Remove} — if the effect is in the permanent list
 *       and the removal is caused by a recognized cure (milk / totem), the event
 *       is cancelled so the effect stays on the player.</li>
 *   <li>{@link PlayerEvent.PlayerLoggedInEvent} / {@link PlayerEvent.PlayerRespawnEvent}
 *       — re-applies every entry in the NBT list to the player. This closes the
 *       gap that exists between server restart / death-respawn and the next time
 *       {@code PersistentReward.reapplyAll} runs.</li>
 * </ul>
 * <p>
 * Manual revocation (via {@link EffectReward#remove} → {@code removePermanent})
 * unconditionally calls {@code player.removeEffect(...)}; that internal removal
 * does NOT come from a cure and therefore does not fork through this listener —
 * so admin/insight-reset paths still work cleanly.
 */
@EventBusSubscriber(modid = Epiphany.MODID)
public final class EffectReapplier {

    static final String KEY = "epiphany_permanent_effects";

    private EffectReapplier() {}

    /**
     * Block cure-driven removal of effects this mod has marked permanent.
     * <p>
     * {@code cure == null} means the removal is programmatic (e.g. our own
     * {@link EffectReward#remove} path), so we let those through. Only in-game
     * cure sources (milk/honey/totem) are blocked.
     */
    @SubscribeEvent
    static void onMobEffectRemove(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        List<String> perm = permanentList(sp);
        if (perm.isEmpty()) return;

        var cure = event.getCure();
        if (cure == null) return;  // programmatic removal, allow
        // Only the "force cure any effect" sources should be blocked. Honey only
        // cures poison by default (vanilla), so it can't touch a permanent Regen
        // anyway; we still match by cure name to be defensive against future cures.
        if (cure != EffectCures.MILK && cure != EffectCures.PROTECTED_BY_TOTEM) return;

        Holder<MobEffect> beingRemoved = event.getEffect();
        String rl = beingRemoved.unwrapKey().map(k -> k.location().toString()).orElse("");
        if (rl.isEmpty()) return;

        boolean isProtected = perm.stream().anyMatch(entry -> entry.startsWith(rl + "|"));
        if (isProtected) {
            event.setCanceled(true);
            Epiphany.LOGGER.debug("Protected permanent effect {} on {} from cure {}",
                    rl, sp.getGameProfile().getName(), cure.name());
        }
    }

    /**
     * Re-applies every permanent effect on login. Covers the "server restarted
     * while player had a permanent effect that wasn't persisted to the player.dat
     * active effects list" edge case.
     */
    @SubscribeEvent
    static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        reapplyAll(sp);
    }

    /**
     * Re-applies every permanent effect after respawn. Attachment data survives
     * via {@code copyOnDeath}, but the in-memory {@link MobEffectInstance} on
     * the new ServerPlayer entity is empty.
     * <p>
     * Note: {@code PlayerRespawnEvent} also triggers
     * {@code PersistentReward.reapplyAll} (registered in {@code Epiphany}), which
     * calls {@link EffectReward#apply} for every permanent reward the player owns
     * — which in turn re-writes the NBT list and re-adds the effect. This login
     * path covers the "server restart" gap; both paths are idempotent.
     */
    @SubscribeEvent
    static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        reapplyAll(sp);
    }

    /**
     * Iterates the persistent-effect NBT list and re-applies each entry.
     * The {@code "namespace:path|amplifier"} string format is owned by
     * {@link EffectReward#effectKey}, this is the only reader.
     * Entries whose MobEffect registry id is no longer valid are skipped silently.
     */
    private static void reapplyAll(ServerPlayer sp) {
        List<String> perm = permanentList(sp);
        if (perm.isEmpty()) return;
        for (String entry : perm) {
            Holder<MobEffect> holder = parseHolder(entry);
            if (holder == null) continue;
            int amplifier = parseAmplifier(entry);
            if (!sp.hasEffect(holder)) {
                sp.addEffect(new MobEffectInstance(holder, -1, amplifier, false, false, true), null);
            }
        }
    }

    /** Parses {@code "id|amp"} → Holder, or null if malformed / unknown. */
    @Nullable
    private static Holder<MobEffect> parseHolder(String entry) {
        String[] parts = entry.split("\\|", 2);
        if (parts.length != 2) return null;
        var id = ResourceLocation.tryParse(parts[0]);
        if (id == null) return null;
        return BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
    }

    /** Parses the {@code amp} segment, or 0 on malformed input. */
    private static int parseAmplifier(String entry) {
        String[] parts = entry.split("\\|", 2);
        if (parts.length != 2) return 0;
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> permanentList(ServerPlayer sp) {
        return sp.getPersistentData().getList(KEY, Tag.TAG_STRING).stream()
                .map(t -> ((net.minecraft.nbt.StringTag) t).getAsString())
                .toList();
    }
}
