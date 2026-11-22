package ink.myumoon.epiphany.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import ink.myumoon.epiphany.Epiphany;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerData;
import ink.myumoon.epiphany.attachment.EpiphanyPlayerState;
import ink.myumoon.epiphany.attachment.InsightPlayerState;
import ink.myumoon.epiphany.attachment.ModulePlayerState;
import ink.myumoon.epiphany.attachment.PlayerDisplayData;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.config.ProgressionRules;
import ink.myumoon.epiphany.content.InitialState;
import ink.myumoon.epiphany.content.condition.Condition;
import ink.myumoon.epiphany.content.condition.logic.AlwaysCondition;
import ink.myumoon.epiphany.content.reward.NoOpEpiphanyReward;
import ink.myumoon.epiphany.content.reward.NoOpInsightReward;
import ink.myumoon.epiphany.network.EpiphanyNetwork;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Development commands for the R1 fixture in fixtures. */
public final class R1Probe {
    private static final ResourceLocation SAMPLE = ResourceLocation.fromNamespaceAndPath("epiphany_r1", "sample");

    private R1Probe() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(R1Probe::registerCommands);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("epiphany_r1")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("registries").executes(context ->
                        report(context.getSource(), inspectRegistries(context.getSource().getServer().registryAccess()))))
                .then(Commands.literal("fallback").executes(context -> {
                    var decoded = Condition.CODEC.parse(JsonOps.INSTANCE,
                            JsonParser.parseString("{\"type\":\"epiphany_r1:missing\"}")).result().orElseThrow();
                    return report(context.getSource(), "unknown condition falls back to always: "
                            + (decoded == AlwaysCondition.INSTANCE));
                }))
                .then(Commands.literal("progress")
                        .then(Commands.argument("player", EntityArgument.player()).executes(context ->
                                report(context.getSource(), EpiphanyPlayerData.get(
                                        EntityArgument.getPlayer(context, "player")).toString()))))
                .then(Commands.literal("seed")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("aptitude", IntegerArgumentType.integer(0))
                                        .executes(context -> seed(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "aptitude"))))))
                .then(Commands.literal("rules")
                        .then(Commands.argument("limit", IntegerArgumentType.integer(-1, 10000)).executes(context -> {
                            var rules = new ProgressionRules(IntegerArgumentType.getInteger(context, "limit"), 10, 1);
                            EpiphanyNetwork.publishRules(context.getSource().getServer(), rules);
                            return report(context.getSource(), rules.toString());
                        }))));
    }

    public static String inspectRegistries(RegistryAccess registries) {
        var module = registries.registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY).get(SAMPLE);
        var insight = registries.registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY).get(SAMPLE);
        var epiphany = registries.registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY).get(SAMPLE);
        var path = registries.registryOrThrow(EpiphanyRegistries.PATH_REGISTRY_KEY).get(SAMPLE);
        var aptitude = registries.registryOrThrow(EpiphanyRegistries.APTITUDE_SOURCE_REGISTRY_KEY).get(SAMPLE);
        if (module == null || insight == null || epiphany == null || path == null || aptitude == null) {
            return "R1 fixture entries missing";
        }
        var name = module.name().orElseThrow();
        boolean expected = name.getStyle().isBold() && insight.cost() == 1 && module.weight() == 100
                && module.initialState() == InitialState.SELECTABLE && module.condition().orElseThrow() == AlwaysCondition.INSTANCE
                && module.onSelectReward().equals(List.of(NoOpInsightReward.INSTANCE))
                && module.onCompleteReward().equals(List.of(NoOpInsightReward.INSTANCE))
                && insight.reward().equals(List.of(NoOpInsightReward.INSTANCE))
                && epiphany.reward().equals(List.of(NoOpEpiphanyReward.INSTANCE))
                && epiphany.path().orElseThrow().equals(SAMPLE) && module.insights().get(0).id().equals(SAMPLE)
                && aptitude.defaultAmountOrZero().min() == 2 && aptitude.defaultAmountOrZero().max() == 6;
        return "R1 fixture module=" + name.getString() + ", path=" + path.name().orElseThrow().getString()
                + ", defaults and dispatch=" + expected;
    }

    private static int seed(CommandSourceStack source, ServerPlayer player, int aptitude) {
        var data = new PlayerEpiphanyData(aptitude, 5, 3,
                Map.of(SAMPLE, new ModulePlayerState(true, true, true, Set.of(SAMPLE))),
                Map.of(SAMPLE, new InsightPlayerState(true, true)),
                Map.of(SAMPLE, new EpiphanyPlayerState(true, true)),
                2, 1, Map.of(SAMPLE, 123456L), new PlayerDisplayData(List.of(SAMPLE), List.of(SAMPLE)),
                Map.of("r1-fixture", List.of(2, 3)));
        EpiphanyPlayerData.set(player, data);
        return report(source, "Seeded " + player.getScoreboardName() + " with aptitude " + aptitude);
    }

    private static int report(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
