package ink.myumoon.epiphany.attachment;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.network.EpiphanyNetwork;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** The server-side holder for a player's persisted progress. */
public final class EpiphanyPlayerData {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "epiphany_data");
    private static final class RegisteredCapability {
        private static final Capability<EpiphanyPlayerData> TOKEN =
                CapabilityManager.get(new CapabilityToken<>() {});
    }

    private PlayerEpiphanyData data = PlayerEpiphanyData.createDefault();

    private EpiphanyPlayerData() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(EpiphanyPlayerData.class));
        gameBus.addGenericListener(Entity.class, EpiphanyPlayerData::attach);
        gameBus.addListener(EpiphanyPlayerData::cloneProgress);
    }

    public static PlayerEpiphanyData get(ServerPlayer player) {
        return store(player).data;
    }

    public static void set(ServerPlayer player, PlayerEpiphanyData data) {
        Objects.requireNonNull(data);
        EpiphanyPlayerData store = store(player);
        if (!store.data.equals(data)) {
            store.data = data;
            EpiphanyNetwork.syncProgress(player, data);
        }
    }

    private static EpiphanyPlayerData store(ServerPlayer player) {
        return player.getCapability(RegisteredCapability.TOKEN).orElseThrow(
                () -> new IllegalStateException("Missing Epiphany capability for " + player.getScoreboardName()));
    }

    private static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer player) {
            Provider provider = new Provider(player);
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    private static void cloneProgress(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original)
                || !(event.getEntity() instanceof ServerPlayer replacement)) {
            return;
        }
        // Forge 47.4.28 removes the original before restoreFrom for both respawn paths.
        original.reviveCaps();
        try {
            store(replacement).data = get(original);
        } finally {
            original.invalidateCaps();
        }
    }

    private static final class Provider implements ICapabilitySerializable<Tag> {
        private final ServerPlayer player;
        private final EpiphanyPlayerData store = new EpiphanyPlayerData();
        private LazyOptional<EpiphanyPlayerData> optional;

        private Provider(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability != RegisteredCapability.TOKEN) {
                return LazyOptional.empty();
            }
            if (optional == null) {
                optional = LazyOptional.of(() -> store);
            }
            return optional.cast();
        }

        private void invalidate() {
            if (optional != null) {
                optional.invalidate();
                optional = null;
            }
        }

        @Override
        public CompoundTag serializeNBT() {
            return (CompoundTag) PlayerEpiphanyData.CODEC.encodeStart(NbtOps.INSTANCE, store.data)
                    .getOrThrow(false, Epiphany.LOGGER::error);
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            try {
                store.data = PlayerEpiphanyData.CODEC.parse(NbtOps.INSTANCE, nbt)
                        .getOrThrow(false, ignored -> {});
            } catch (RuntimeException cause) {
                throw new IllegalStateException(
                        "Failed to load capability " + ID + " for player " + player.getUUID(), cause);
            }
        }
    }
}
