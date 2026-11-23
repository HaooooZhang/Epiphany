package ink.myumoon.epiphany.network;

import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerData;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.client.ClientProgress;
import ink.myumoon.epiphany.config.AptitudeCurve;
import ink.myumoon.epiphany.config.CommonConfig;
import ink.myumoon.epiphany.config.SelectionRules;
import ink.myumoon.epiphany.config.ProgressionRules;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class EpiphanyNetwork {
    private static final String PROTOCOL = "2";
    private static SimpleChannel channel;

    private static ProgressionRules activeRules = ProgressionRules.DEFAULT;

    private EpiphanyNetwork() {
    }

    public static void register(IEventBus gameBus) {
        channel = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(Epiphany.MODID, "progress"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        channel.messageBuilder(ProgressSnapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> buffer.writeWithCodec(NbtOps.INSTANCE, PlayerEpiphanyData.CODEC, message.data()))
                .decoder(buffer -> new ProgressSnapshot(buffer.readWithCodec(NbtOps.INSTANCE, PlayerEpiphanyData.CODEC)))
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientProgress.receiveProgress(context.get().getNetworkManager(), message.data())))
                .add();

        channel.messageBuilder(ProgressionRules.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EpiphanyNetwork::encodeRules)
                .decoder(EpiphanyNetwork::decodeRules)
                .consumerMainThread((rules, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientProgress.receiveRules(context.get().getNetworkManager(), rules)))
                .add();

        channel.messageBuilder(SelectionRules.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((rules, buffer) -> {
                    buffer.writeVarInt(rules.moduleCost());
                    buffer.writeVarInt(rules.maxModules());
                    buffer.writeVarInt(rules.maxEpiphanySlots());
                })
                .decoder(buffer -> new SelectionRules(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()))
                .consumerMainThread((rules, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientProgress.receiveSelectionRules(context.get().getNetworkManager(), rules)))
                .add();

        gameBus.addListener((ServerStoppedEvent event) -> activeRules = ProgressionRules.DEFAULT);
        gameBus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> syncPlayer(event.getEntity()));
        gameBus.addListener((PlayerEvent.PlayerRespawnEvent event) -> syncPlayer(event.getEntity()));
        gameBus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> syncPlayer(event.getEntity()));
    }

    private static void syncPlayer(Player entity) {
        if (entity instanceof ServerPlayer player) {
            syncProgress(player, EpiphanyPlayerData.get(player));
            syncRules(player, activeRules);
            syncSelectionRules(player, SelectionRules.fromConfig());
        }
    }

    public static void syncProgress(ServerPlayer player, PlayerEpiphanyData data) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new ProgressSnapshot(data));
    }

    public static void publishRules(MinecraftServer server, ProgressionRules rules) {
        activeRules = rules;
        var players = server.getPlayerList();
        if (players != null) players.getPlayers().forEach(player -> syncRules(player, rules));
    }

    public static void onCommonConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != CommonConfig.SPEC) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.execute(() -> {
            if (server == ServerLifecycleHooks.getCurrentServer()) publishSelectionRules(server);
        });
    }

    public static void publishSelectionRules(MinecraftServer server) {
        var rules = SelectionRules.fromConfig();
        var players = server.getPlayerList();
        if (players != null) players.getPlayers().forEach(player -> syncSelectionRules(player, rules));
    }

    private static void syncSelectionRules(ServerPlayer player, SelectionRules rules) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), rules);
    }

    private static void syncRules(ServerPlayer player, ProgressionRules rules) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), rules);
    }

    private static void encodeRules(ProgressionRules rules, FriendlyByteBuf buffer) {
        buffer.writeVarInt(rules.maxTotalInsightPoints());
        buffer.writeVarInt(rules.curve().segments().size());
        for (var segment : rules.curve().segments()) {
            buffer.writeVarInt(segment.from());
            buffer.writeEnum(segment.type());
            for (String key : segment.type().parameters) {
                buffer.writeUtf(segment.value(key).toPlainString(), 32);
            }
        }
    }

    private static ProgressionRules decodeRules(FriendlyByteBuf buffer) {
        int limit = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 1 || count > AptitudeCurve.MAX_SEGMENTS) {
            throw new IllegalArgumentException("Invalid aptitude curve length: " + count);
        }
        var segments = new ArrayList<AptitudeCurve.Segment>(count);
        for (int i = 0; i < count; i++) {
            int from = buffer.readVarInt();
            var type = buffer.readEnum(AptitudeCurve.Function.class);
            var values = new LinkedHashMap<String, BigDecimal>();
            for (String key : type.parameters) {
                values.put(key, new BigDecimal(buffer.readUtf(32)));
            }
            segments.add(new AptitudeCurve.Segment(from, type, values));
        }
        return new ProgressionRules(limit, new AptitudeCurve(segments));
    }

    private record ProgressSnapshot(PlayerEpiphanyData data) {
    }
}
