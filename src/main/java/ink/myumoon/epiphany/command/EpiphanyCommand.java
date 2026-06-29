package ink.myumoon.epiphany.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import ink.myumoon.epiphany.api.*;
import ink.myumoon.epiphany.attachment.PlayerEpiphanyData;
import ink.myumoon.epiphany.content.InsightTreeResolver;
import ink.myumoon.epiphany.registry.EpiphanyAttachmentTypes;
import ink.myumoon.epiphany.registry.EpiphanyRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Root command {@code /epiphany} with sub-command groups for
 * aptitude, insight, module, epiphany, path, and reset.
 */
public final class EpiphanyCommand {

    private EpiphanyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epiphany")
                .requires(s -> s.hasPermission(2))
                .then(aptitudeGroup())
                .then(insightGroup())
                .then(moduleGroup())
                .then(epiphanyGroup())
                .then(pathGroup())
                .then(resetGroup())
        );
    }

    // ============================================================
    // aptitude
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> aptitudeGroup() {
        return Commands.literal("aptitude")
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            AptitudeManager.addAptitude(target, amount);
                                            ctx.getSource().sendSuccess(
                                                    () -> t("commands.epiphany.aptitude.add.success", amount, target.getGameProfile().getName()), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", LongArgumentType.longArg(0))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            long value = LongArgumentType.getLong(ctx, "value");
                                            AptitudeManager.setAptitude(target, value);
                                            ctx.getSource().sendSuccess(
                                                    () -> t("commands.epiphany.aptitude.set.success", value, target.getGameProfile().getName()), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("query")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    long apt = AptitudeManager.getAptitude(target);
                                    int pts = AptitudeManager.getInsightPoints(target);
                                    int spent = AptitudeManager.getTotalInsightPointsSpent(target);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.aptitude.query.success", target.getGameProfile().getName(), apt, pts, spent), false);
                                    return 1;
                                })))
                .then(Commands.literal("fill")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    long required = AptitudeFormula.calcRequiredAptitude(
                                            AptitudeManager.getTotalInsightPointsSpent(target),
                                            AptitudeManager.getInsightPoints(target));
                                    long toAdd = required - AptitudeManager.getAptitude(target);
                                    if (toAdd > 0) AptitudeManager.addAptitude(target, toAdd);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.aptitude.fill.success", required, target.getGameProfile().getName()), true);
                                    return 1;
                                })));
    }

    // ============================================================
    // insight
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> insightGroup() {
        var insightArg = Commands.argument("insight", ResourceLocationArgument.id())
                .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider
                        .suggestResource(ctx.getSource().registryAccess()
                                .registryOrThrow(EpiphanyRegistries.INSIGHT_REGISTRY_KEY).keySet(), builder));

        return Commands.literal("insight")
                .then(Commands.literal("select")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(insightArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "insight");
                                    InsightManager.forceSelect(target, id, findModuleForInsight(target, id));
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.insight.select.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("try_select")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(insightArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "insight");
                                    ResourceLocation mid = findModuleForInsight(target, id);
                                    if (!InsightTreeResolver.canUnlock(target.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA), mid, target.server.registryAccess().registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY).get(mid), id)) {
                                        ctx.getSource().sendFailure(Component.literal("Cannot select insight (check tree prerequisites and points)"));
                                        return 0;
                                    }
                                    InsightManager.select(target, id, mid);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.insight.select.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(insightArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "insight");
                                    InsightManager.resetInsight(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.insight.reset.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("query")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(insightArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "insight");
                                    boolean sel = InsightManager.isSelected(target, id);
                                    boolean modSel = InsightManager.isModuleSelected(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.insight.query.success", id, target.getGameProfile().getName(), sel, modSel), false);
                                    return 1;
                                }))))
                .then(Commands.literal("points")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                                    int current = AptitudeManager.getInsightPoints(target);
                                                    AptitudeManager.setInsightPoints(target, current + amt);
                                                    ctx.getSource().sendSuccess(
                                                            () -> t("commands.epiphany.insight.points.add.success", amt, target.getGameProfile().getName()), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                                    AptitudeManager.setInsightPoints(target, amt);
                                                    ctx.getSource().sendSuccess(
                                                            () -> t("commands.epiphany.insight.points.set.success", amt, target.getGameProfile().getName()), true);
                                                    return 1;
                                                })))));
    }

    // ============================================================
    // module
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> moduleGroup() {
        var moduleArg = Commands.argument("module", ResourceLocationArgument.id())
                .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider
                        .suggestResource(ctx.getSource().registryAccess()
                                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY).keySet(), builder));

        return Commands.literal("module")
                .then(Commands.literal("unlock")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(moduleArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "module");
                                    ModuleManager.setUnlocked(target, id, true);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.module.unlock.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("select")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(moduleArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "module");
                                    ModuleManager.forceSelect(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.module.select.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("try_select")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(moduleArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "module");
                                    if (!ModuleManager.isUnlocked(target, id)) {
                                        ctx.getSource().sendFailure(Component.literal("Module not unlocked or insufficient points"));
                                        return 0;
                                    }
                                    ModuleManager.select(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.module.select.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("complete")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(moduleArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "module");
                                    ModuleManager.forceComplete(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.module.complete.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(moduleArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "module");
                                    ModuleManager.resetModule(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.module.reset.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("query")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(moduleArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "module");
                                    boolean u = ModuleManager.isUnlocked(target, id);
                                    boolean s = ModuleManager.isSelected(target, id);
                                    boolean c = ModuleManager.isCompleted(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.module.query.success", id, target.getGameProfile().getName(), u, s, c), false);
                                    return 1;
                                }))));
    }

    // ============================================================
    // epiphany
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> epiphanyGroup() {
        var epiphanyArg = Commands.argument("epiphany", ResourceLocationArgument.id())
                .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider
                        .suggestResource(ctx.getSource().registryAccess()
                                .registryOrThrow(EpiphanyRegistries.EPIPHANY_REGISTRY_KEY).keySet(), builder));

        return Commands.literal("epiphany")
                .then(Commands.literal("unlock")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(epiphanyArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "epiphany");
                                    EpiphanyManager.setUnlocked(target, id, true);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.epiphany.unlock.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("select")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(epiphanyArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "epiphany");
                                    EpiphanyManager.forceSelect(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.epiphany.select.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("try_select")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(epiphanyArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "epiphany");
                                    if (!EpiphanyManager.isUnlocked(target, id)) {
                                        ctx.getSource().sendFailure(Component.literal("Epiphany not unlocked or no slots available"));
                                        return 0;
                                    }
                                    EpiphanyManager.select(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.epiphany.select.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(epiphanyArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "epiphany");
                                    EpiphanyManager.resetEpiphany(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.epiphany.reset.success", id, target.getGameProfile().getName()), true);
                                    return 1;
                                }))))
                .then(Commands.literal("query")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(epiphanyArg.executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "epiphany");
                                    boolean u = EpiphanyManager.isUnlocked(target, id);
                                    boolean s = EpiphanyManager.isSelected(target, id);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.epiphany.query.success", id, target.getGameProfile().getName(), u, s), false);
                                    return 1;
                                }))));
    }

    // ============================================================
    // path
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> pathGroup() {
        return Commands.literal("path")
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var paths = ctx.getSource().registryAccess()
                                    .registryOrThrow(EpiphanyRegistries.PATH_REGISTRY_KEY);
                            ctx.getSource().sendSuccess(
                                    () -> t("commands.epiphany.path.list.success", paths.keySet().toString()), false);
                            return 1;
                        }));
    }

    // ============================================================
    // reset
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> resetGroup() {
        return Commands.literal("reset")
                .then(Commands.literal("all")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    target.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA,
                                            PlayerEpiphanyData.createDefault());
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.reset.all.success", target.getGameProfile().getName()), true);
                                    return 1;
                                })))
                .then(Commands.literal("select")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    PlayerEpiphanyData data = target.getData(EpiphanyAttachmentTypes.EPIPHANY_DATA);
                                    PlayerEpiphanyData cleaned = new PlayerEpiphanyData(
                                            data.aptitude(),
                                            data.insightPoints(),
                                            data.totalInsightPointsSpent(),
                                            java.util.Collections.emptyMap(),
                                            java.util.Collections.emptyMap(),
                                            java.util.Collections.emptyMap(),
                                            0, 0
                                    );
                                    target.setData(EpiphanyAttachmentTypes.EPIPHANY_DATA, cleaned);
                                    ctx.getSource().sendSuccess(
                                            () -> t("commands.epiphany.reset.select.success", target.getGameProfile().getName()), true);
                                    return 1;
                                })));
    }

    // ============================================================
    // helper
    // ============================================================

    private static Component t(String key, Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ResourceLocation rl) args[i] = rl.toString();
            else if (args[i] instanceof Boolean b) args[i] = b.toString();
            else if (args[i] instanceof Number n) args[i] = n.toString();
        }
        return Component.translatable(key, args);
    }

    private static ResourceLocation findModuleForInsight(ServerPlayer player, ResourceLocation insightId) {
        var reg = player.server.registryAccess()
                .registryOrThrow(EpiphanyRegistries.MODULE_REGISTRY_KEY);
        for (var entry : reg.entrySet()) {
            for (var ie : entry.getValue().insights()) {
                if (ie.id().equals(insightId)) return entry.getKey().location();
            }
        }
        // fallback — the insight will fail to find its module but at least won't NPE
        return insightId;
    }
}
