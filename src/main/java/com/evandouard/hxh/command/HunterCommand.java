package com.evandouard.hxh.command;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.HatsuKits;
import com.evandouard.hxh.hatsu.Techniques;
import com.evandouard.hxh.hatsu.Technique;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * /hunter : consulter ses stats. Sous-commandes d'administration (niveau 2) pour tester.
 */
public final class HunterCommand {
    private static final DynamicCommandExceptionType UNKNOWN_STAT = new DynamicCommandExceptionType(
            id -> Component.translatable("command.hxh.unknown_stat", id));
    private static final DynamicCommandExceptionType UNKNOWN_KIT = new DynamicCommandExceptionType(
            id -> Component.translatable("message.hxh.hatsu_unknown_id", id));
    private static final DynamicCommandExceptionType BAD_COLOR = new DynamicCommandExceptionType(
            hex -> Component.translatable("command.hxh.bad_color", hex));
    private static final DynamicCommandExceptionType UNKNOWN_NEN = new DynamicCommandExceptionType(
            id -> Component.translatable("command.hxh.unknown_nen", id));

    private HunterCommand() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("hunter")
                .executes(ctx -> show(ctx.getSource(), ctx.getSource().getEntityOrException()))
                .then(Commands.literal("show")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> show(ctx.getSource(), EntityArgument.getEntity(ctx, "target")))))
                .then(statCommand())
                // Tout au maximum : stats, Nen, maîtrises, soin, aura (tests)
                .then(targeted("max", HunterCommand::maxAll).requires(s -> s.hasPermission(2)))
                .then(nenCommand())
                .then(hatsuCommand())
                .then(vowCommand())
                // Bras de fer contre un adversaire de cette Force (admin, tests)
                .then(Commands.literal("armwrestle").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("strength", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100)).executes(ctx ->
                                com.evandouard.hxh.minigame.ArmWrestle.start(ctx.getSource().getPlayerOrException(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "strength"), 0) ? 1 : 0)))
                // Licence Hunter (admin, tests) : donner ou retirer
                .then(Commands.literal("license").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("give").executes(ctx -> {
                            var p = ctx.getSource().getPlayerOrException();
                            com.evandouard.hxh.quest.QuestManager.data(p).setHunter(true);
                            com.evandouard.hxh.quest.QuestManager.sync(p);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.license", Component.translatable("command.hxh.yes")), false);
                            return 1;
                        }))
                        .then(Commands.literal("remove").executes(ctx -> {
                            var p = ctx.getSource().getPlayerOrException();
                            com.evandouard.hxh.quest.QuestManager.data(p).setHunter(false);
                            com.evandouard.hxh.quest.QuestManager.sync(p);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.license", Component.translatable("command.hxh.no")), false);
                            return 1;
                        })))
                .then(Commands.literal("blacklist")
                        .executes(ctx -> {
                            com.evandouard.hxh.contract.Contracts.showBlacklist(ctx.getSource().getPlayerOrException());
                            return 1;
                        })
                        .then(Commands.literal("take").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                            var p = ctx.getSource().getPlayerOrException();
                            var c = com.evandouard.hxh.contract.Contracts.get(StringArgumentType.getString(ctx, "id"));
                            String r = c == null ? "message.hxh.blacklist_unknown" : com.evandouard.hxh.contract.Contracts.takeBlacklist(p, c);
                            if (r != null) ctx.getSource().sendFailure(Component.translatable(r));
                            return r == null ? 1 : 0;
                        }))))
                .then(Commands.literal("bet").then(Commands.argument("fighter", StringArgumentType.word())
                        .then(Commands.argument("side", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("player", "opponent"), b))
                                .then(Commands.argument("stake", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(ctx -> {
                                    var p = ctx.getSource().getPlayerOrException();
                                    java.util.UUID f = com.evandouard.hxh.arena.ArenaBets.fighterByName(StringArgumentType.getString(ctx, "fighter"));
                                    var r = com.evandouard.hxh.arena.ArenaBets.place(p, f == null ? new java.util.UUID(0, 0) : f,
                                            !"opponent".equals(StringArgumentType.getString(ctx, "side")),
                                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "stake"), p.level().getGameTime());
                                    return r == com.evandouard.hxh.arena.ArenaBets.Result.OK ? 1 : 0;
                                })))))
                // Événements aléatoires : en déclencher un (au hasard ou par nom) près de soi
                .then(Commands.literal("event").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        com.evandouard.hxh.world.WorldEvents.all().keySet().stream().map(net.minecraft.resources.ResourceLocation::getPath), b))
                                .executes(ctx -> {
                                    var ev = com.evandouard.hxh.world.WorldEvents.get(StringArgumentType.getString(ctx, "id"));
                                    if (ev == null) {
                                        ctx.getSource().sendFailure(Component.translatable("command.hxh.event_unknown", StringArgumentType.getString(ctx, "id")));
                                        return 0;
                                    }
                                    return com.evandouard.hxh.world.WorldEvents.trigger(ev, ctx.getSource().getPlayerOrException()).size();
                                })))
                // Knov : sortir du refuge par une porte marquée (liens cliquables) — ouvert à tous
                .then(Commands.literal("knov").then(Commands.literal("exit").then(Commands.argument("n", IntegerArgumentType.integer(0, 9))
                        .executes(ctx -> com.evandouard.hxh.hatsu.Specials.exitRefuge(ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "n")) ? 1 : 0))))
                // Vol en dirigeable (lien cliquable du guichetier) : ouvert à tous
                .then(Commands.literal("fly").then(Commands.argument("airport", IntegerArgumentType.integer(1))
                        .executes(ctx -> com.evandouard.hxh.travel.AirTravel.fly(ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "airport")) ? 1 : 0)))
                // Sanctuaires de méditation : acheter des coordonnées (lien de l'archéologue) ; en bâtir un (admin)
                // Forme de l'En (maîtrise 50) : /hunter en shape <sphere|half_sphere|cube|cylinder> [rayon] [hauteur+]
                .then(Commands.literal("en").then(Commands.literal("shape")
                        .then(Commands.argument("shape", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(
                                        com.evandouard.hxh.nen.EnShape.values()).map(com.evandouard.hxh.nen.EnShape::id), b))
                                .executes(ctx -> enShape(ctx.getSource(), StringArgumentType.getString(ctx, "shape"), 3f, 1f))
                                .then(Commands.argument("a", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0, 64))
                                        .executes(ctx -> enShape(ctx.getSource(), StringArgumentType.getString(ctx, "shape"),
                                                com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "a"), 1f))
                                        .then(Commands.argument("b", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0, 64))
                                                .executes(ctx -> enShape(ctx.getSource(), StringArgumentType.getString(ctx, "shape"),
                                                        com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "a"),
                                                        com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "b"))))))))
                // Contrat en cours : aller jusqu'à la cible (admin, pour tester)
                .then(Commands.literal("contract").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("goto").executes(ctx -> {
                            net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                            net.minecraft.core.BlockPos at = com.evandouard.hxh.contract.Contracts.targetPos(p);
                            if (at == null) {
                                ctx.getSource().sendFailure(Component.translatable("command.hxh.contract_none"));
                                return 0;
                            }
                            var level = p.serverLevel();
                            level.getChunk(at.getX() >> 4, at.getZ() >> 4);
                            var ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
                            p.teleportTo(level, ground.getX() + 4.5, ground.getY(), ground.getZ() + 0.5, 90f, 10f);
                            return 1;
                        })))
                .then(Commands.literal("shrine")
                        .then(Commands.literal("buy").then(Commands.argument("tier", IntegerArgumentType.integer(1, 5))
                                .executes(ctx -> com.evandouard.hxh.meditation.MeditationShrines.buy(ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "tier")) ? 1 : 0)))
                        .then(Commands.literal("nearest").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            var p = ctx.getSource().getPlayerOrException();
                            long[] sh = com.evandouard.hxh.meditation.MeditationShrines.nearest(p.serverLevel(), p.blockPosition());
                            if (sh == null) {
                                ctx.getSource().sendFailure(Component.translatable("command.hxh.shrine_none"));
                                return 0;
                            }
                            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(sh[0]);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.shrine_nearest", sh[1], pos.getX(), pos.getY(), pos.getZ(),
                                    (int) Math.sqrt(pos.distSqr(p.blockPosition()))), false);
                            return 1;
                        }))
                        .then(Commands.literal("build").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("tier", IntegerArgumentType.integer(1, 5)).executes(ctx -> {
                                    var p = ctx.getSource().getPlayerOrException();
                                    return com.evandouard.hxh.meditation.MeditationShrines.build(p.serverLevel(),
                                            p.blockPosition().offset(0, 0, 6), IntegerArgumentType.getInteger(ctx, "tier"), false) != null ? 1 : 0;
                                }))))
                // Familles : liste ; fixer la famille d'un joueur (admin)
                .then(Commands.literal("family")
                        .then(Commands.literal("list").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.translatable("family.hxh.list",
                                    String.join(", ", com.evandouard.hxh.family.Families.all().keySet())), false);
                            return 1;
                        }))
                        .then(Commands.literal("set").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    com.evandouard.hxh.family.Families.set(ctx.getSource().getPlayerOrException(), id.equals("none") ? "" : id);
                                    return 1;
                                }))))
                // Organisations : quitter (tous), rejoindre directement (admin), liste
                .then(Commands.literal("org")
                        .then(Commands.literal("leave").executes(ctx ->
                                com.evandouard.hxh.org.Organizations.leave(ctx.getSource().getPlayerOrException()) ? 1 : 0))
                        .then(Commands.literal("list").executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.translatable("org.hxh.list",
                                    String.join(", ", com.evandouard.hxh.org.Organizations.all().keySet())), false);
                            return 1;
                        }))
                        .then(Commands.literal("join").requires(src -> src.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                                    com.evandouard.hxh.org.Organizations.join(ctx.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(ctx, "id"));
                                    return 1;
                                }))))
                // Renommer l'aéroport le plus proche (admin)
                .then(Commands.literal("airport").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> com.evandouard.hxh.travel.AirTravel.rename(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "name")) ? 1 : 0))))
                .then(Commands.literal("exam")
                        // Réponse à un quiz de l'Examen (lien cliquable du chat) : ouvert à tous
                        // Embarquer à bord de son dirigeable (lien cliquable de l'hôtesse) : ouvert à tous
                        .then(Commands.literal("board").executes(ctx ->
                                        com.evandouard.hxh.exam.ExamDeparture.board(ctx.getSource().getPlayerOrException()) ? 1 : 0)
                                .then(Commands.argument("ticket", com.mojang.brigadier.arguments.LongArgumentType.longArg())
                                        .executes(ctx -> com.evandouard.hxh.exam.ExamDeparture.board(ctx.getSource().getPlayerOrException(),
                                                com.mojang.brigadier.arguments.LongArgumentType.getLong(ctx, "ticket")) ? 1 : 0)))
                        // Pari contre Leroute (Tour de Trick) : 1 = pile, 2 = face
                        .then(Commands.literal("bet").then(Commands.argument("n", IntegerArgumentType.integer(1, 2))
                                .executes(ctx -> com.evandouard.hxh.exam.TrickTower.bet(ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "n")) ? 1 : 0)))
                        .then(Commands.literal("answer").then(Commands.argument("n", IntegerArgumentType.integer(0, 9))
                                .executes(ctx -> com.evandouard.hxh.quest.QuestManager.answer(ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "n")) ? 1 : 0)))
                        .then(Commands.literal("setup").requires(src -> src.hasPermission(2)).executes(ctx -> examSetup(ctx.getSource())))
                        // Raccourci (tests) : aller au point de départ donné par le guide
                        .then(Commands.literal("go").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                            long dep = com.evandouard.hxh.quest.QuestManager.data(p).examDeparture();
                            if (dep == Long.MIN_VALUE) {
                                ctx.getSource().sendFailure(Component.translatable("command.hxh.exam_go_none"));
                                return 0;
                            }
                            net.minecraft.core.BlockPos b = net.minecraft.core.BlockPos.of(dep);
                            p.teleportTo(p.server.overworld(), b.getX() - 1.5, b.getY(), b.getZ() + 0.5, -90f, 10f); // sur le pont, face à l'hôtesse
                            return 1;
                        }))
                        // Épreuves tirées : les afficher, ou imposer le prochain tirage (tests, essais)
                        .then(Commands.literal("plan").requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    com.evandouard.hxh.quest.QuestData qd = com.evandouard.hxh.quest.QuestManager.data(p);
                                    java.util.List<String> t = qd.planTrials(com.evandouard.hxh.quest.QuestRegistry.HUNTER_EXAM.toString());
                                    StringBuilder places = new StringBuilder();
                                    for (com.evandouard.hxh.exam.ExamSite site : com.evandouard.hxh.exam.ExamSite.values())
                                        if (qd.examUses(site.id()) > 0 || qd.examInstance(site.id()) > 0)
                                            places.append(" ").append(site.id()).append("#").append(qd.examInstance(site.id()));
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.exam_plan",
                                            String.join(" → ", t) + " |" + places), false);
                                    return t.size();
                                })
                                .then(Commands.argument("trials", StringArgumentType.greedyString()).executes(ctx -> {
                                    net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    java.util.List<String> t = java.util.List.of(StringArgumentType.getString(ctx, "trials").trim().split("\\s+"));
                                    com.evandouard.hxh.quest.Quest q = com.evandouard.hxh.quest.QuestRegistry.get(com.evandouard.hxh.quest.QuestRegistry.HUNTER_EXAM);
                                    for (String id : t) if (q == null || !q.trials().containsKey(id)) {
                                        ctx.getSource().sendFailure(Component.translatable("command.hxh.exam_plan_unknown", id));
                                        return 0;
                                    }
                                    com.evandouard.hxh.quest.TrialPlan.forcedForTests = t;
                                    if (!com.evandouard.hxh.quest.QuestManager.redrawExam(p)) com.evandouard.hxh.quest.QuestManager.startFirstQuest(p);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.exam_plan", String.join(" → ", t)), false);
                                    return 1;
                                })))
                        .then(Commands.literal("where").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            long nav = com.evandouard.hxh.exam.ExamSavedData.get(ctx.getSource().getLevel()).navigator();
                            if (nav == Long.MIN_VALUE) {
                                ctx.getSource().sendFailure(Component.translatable("command.hxh.exam_where_none"));
                                return 0;
                            }
                            net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.of(nav);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.exam_where", p.getX(), p.getY(), p.getZ()), false);
                            return 1;
                        }))
                        .then(Commands.literal("rebuild").requires(src -> src.hasPermission(2)).executes(ctx -> {
                            net.minecraft.server.level.ServerLevel exam = com.evandouard.hxh.exam.ExamWorld.level(ctx.getSource().getServer());
                            if (exam != null) com.evandouard.hxh.exam.ExamWorld.forget(exam);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.exam_rebuild"), true);
                            return 1;
                        }))
                        .then(Commands.literal("site").requires(src -> src.hasPermission(2)).then(Commands.argument("site", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(
                                        com.evandouard.hxh.exam.ExamSite.values()).map(com.evandouard.hxh.exam.ExamSite::id), b))
                                .executes(ctx -> {
                                    com.evandouard.hxh.exam.ExamSite site = com.evandouard.hxh.exam.ExamSite.byId(StringArgumentType.getString(ctx, "site"));
                                    return site != null && com.evandouard.hxh.exam.ExamTrips.sendTo(ctx.getSource().getPlayerOrException(), site) ? 1 : 0;
                                }))))
                .then(arenaCommand())
                .then(structureCommand())
                .then(Commands.literal("quest").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reset").executes(ctx -> {
                            net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                            com.evandouard.hxh.quest.QuestManager.data(p).reset();
                            com.evandouard.hxh.exam.ExamTournament.reset(p);
                            com.evandouard.hxh.exam.Exam288.reset(p);
                            com.evandouard.hxh.quest.QuestManager.sync(p);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.quest_reset"), false);
                            return 1;
                        }))
                        // Valide l'étape « custom » en cours de la quête suivie (conditions pas encore codées)
                        .then(Commands.literal("validate").executes(ctx -> {
                            net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String tracked = com.evandouard.hxh.quest.QuestManager.data(p).tracked();
                            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(tracked);
                            boolean ok = id != null && com.evandouard.hxh.quest.QuestManager.completeCustom(p, id);
                            if (!ok) ctx.getSource().sendFailure(Component.translatable("command.hxh.quest_not_custom"));
                            return ok ? 1 : 0;
                        }))
                        // Erreurs repérées dans les fichiers de quête
                        .then(Commands.literal("check").executes(ctx -> {
                            java.util.List<String> problems = com.evandouard.hxh.quest.QuestRegistry.problems();
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.quest_check",
                                    com.evandouard.hxh.quest.QuestRegistry.all().size(), problems.size()), false);
                            for (String p : problems) ctx.getSource().sendSuccess(() -> Component.literal(" ⚠ " + p)
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW), false);
                            return problems.isEmpty() ? 1 : 0;
                        }))
                        .then(Commands.literal("skip").executes(ctx -> {
                            com.evandouard.hxh.quest.QuestManager.advance(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("start")
                                .then(Commands.argument("quest", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                                com.evandouard.hxh.quest.QuestRegistry.all().stream().map(com.evandouard.hxh.quest.Quest::id), b))
                                        .executes(ctx -> {
                                            net.minecraft.resources.ResourceLocation id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "quest");
                                            boolean ok = com.evandouard.hxh.quest.QuestManager.start(ctx.getSource().getPlayerOrException(), id);
                                            if (!ok) ctx.getSource().sendFailure(Component.translatable("command.hxh.quest_start_failed", id.toString()));
                                            return ok ? 1 : 0;
                                        }))))
                // Où est la Tour Céleste ? (cliquer pour s'y téléporter)
                .then(Commands.literal("tower")
                        .then(Commands.literal("tp").executes(ctx -> {
                            net.minecraft.core.BlockPos c = com.evandouard.hxh.arena.TowerPlacer.center(ctx.getSource().getServer());
                            if (c == null) return 0;
                            net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                            // À 70 blocs au nord, regard vers la tour et vers le haut
                            p.teleportTo(ctx.getSource().getServer().overworld(), c.getX() + 0.5, c.getY() + 8, c.getZ() - 70.5,
                                    0f, -30f);
                            return 1;
                        }))
                        .executes(ctx -> {
                    net.minecraft.core.BlockPos c = com.evandouard.hxh.arena.TowerPlacer.center(ctx.getSource().getServer());
                    if (c == null) {
                        ctx.getSource().sendFailure(Component.translatable("command.hxh.tower_none"));
                        return 0;
                    }
                    String tp = "/tp @s " + c.getX() + " " + (c.getY() + 3) + " " + (c.getZ() - 40);
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.tower_at", c.getX(), c.getY(), c.getZ())
                            .withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GOLD)
                                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, tp))), false);
                    return 1;
                }))
                // Mobs d'exemple (data/hxh/mob_presets) : /hunter spawn <nom> [position] [nombre]
                .then(Commands.literal("spawn").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("preset", StringArgumentType.string())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        com.evandouard.hxh.mob.MobPresets.all().keySet().stream().map(ResourceLocation::getPath), b))
                                .executes(ctx -> spawnPreset(ctx.getSource(), StringArgumentType.getString(ctx, "preset"),
                                        ctx.getSource().getPosition(), 1))
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3())
                                        .executes(ctx -> spawnPreset(ctx.getSource(), StringArgumentType.getString(ctx, "preset"),
                                                net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3(ctx, "pos"), 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 20))
                                                .executes(ctx -> spawnPreset(ctx.getSource(), StringArgumentType.getString(ctx, "preset"),
                                                        net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3(ctx, "pos"),
                                                        IntegerArgumentType.getInteger(ctx, "count")))))))
                // Jenny du carnet (tests des boutiques)
                .then(Commands.literal("jenny").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(ctx -> jenny(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"), false))))
                        .then(Commands.literal("set").then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> jenny(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"), true)))))
                // Tempérament de l'aura : calme (vapeur au repos) ou sauvage (flammes même au repos)
                .then(Commands.literal("aura").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("calm").executes(ctx -> setTemper(ctx.getSource(), false)))
                        .then(Commands.literal("wild").executes(ctx -> setTemper(ctx.getSource(), true))))
                // Frappe chargée immédiate avec une valeur de barre donnée (tests des paliers d'impact)
                .then(Commands.literal("strike").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("value", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0f, 1f))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntityOrException() instanceof LivingEntity living)) return 0;
                                    float value = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "value");
                                    com.evandouard.hxh.combat.Impacts.Tier tier = com.evandouard.hxh.combat.Impacts.tierOf(living);
                                    float dmg = com.evandouard.hxh.combat.Combat.chargedStrike(living, value);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.strike",
                                            Component.translatable("impact.hxh." + tier.name().toLowerCase(java.util.Locale.ROOT)),
                                            String.format(java.util.Locale.ROOT, "%.1f", dmg)), false);
                                    return dmg > 0 ? 1 : 0;
                                })))
                // Joue une animation de combat sur des entités (tests, mise en scène)
                .then(Commands.literal("anim").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("anim", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(com.evandouard.hxh.combat.CombatAnim.values())
                                                        .map(a -> a.name().toLowerCase(java.util.Locale.ROOT)), b))
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "anim").toUpperCase(java.util.Locale.ROOT);
                                            com.evandouard.hxh.combat.CombatAnim anim;
                                            try {
                                                anim = com.evandouard.hxh.combat.CombatAnim.valueOf(name);
                                            } catch (IllegalArgumentException ex) {
                                                ctx.getSource().sendFailure(Component.literal("? " + name));
                                                return 0;
                                            }
                                            int n = 0;
                                            for (Entity e : EntityArgument.getEntities(ctx, "targets")) {
                                                if (e instanceof LivingEntity living) {
                                                    com.evandouard.hxh.combat.CombatAnimations.play(living, anim);
                                                    n++;
                                                }
                                            }
                                            return n;
                                        }))))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                        .executes(ctx -> apply(ctx, List.of(ctx.getSource().getEntityOrException()), e -> resetAll(e)))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> apply(ctx, EntityArgument.getEntities(ctx, "targets"), e -> resetAll(e))))));
    }

    private static int spawnPreset(CommandSourceStack source, String name, net.minecraft.world.phys.Vec3 pos, int count) {
        com.evandouard.hxh.mob.MobPreset preset = com.evandouard.hxh.mob.MobPresets.get(name);
        if (preset == null) {
            source.sendFailure(Component.translatable("command.hxh.spawn_unknown", name,
                    String.join(", ", com.evandouard.hxh.mob.MobPresets.all().keySet().stream().map(ResourceLocation::getPath).toList())));
            return 0;
        }
        // Tourné vers celui qui lance la commande
        float yaw = source.getEntity() != null ? source.getEntity().getYRot() + 180f : 0f;
        int n = 0;
        for (int i = 0; i < count; i++) {
            net.minecraft.world.phys.Vec3 p = count == 1 ? pos : pos.add((i % 5) * 1.5 - 3, 0, (i / 5) * 1.5);
            if (preset.spawn(source.getLevel(), p, yaw) != null) n++;
        }
        int spawned = n;
        source.sendSuccess(() -> Component.translatable("command.hxh.spawned", spawned, Component.translatable(preset.nameKey())), true);
        return n;
    }

    private static void maxAll(LivingEntity entity) {
        HunterData d = HunterStats.get(entity);
        for (Stat s : Stat.values()) d.setLevel(s, Stat.MAX_LEVEL);
        if (d.nenType() == null) d.setNenType(NenType.ENHANCER);
        if (!d.nenUnlocked()) NenModes.unlock(entity);
        d.setNenRevealed(true);
        d.setNenLevel(com.evandouard.hxh.data.Mastery.MAX_LEVEL);
        for (com.evandouard.hxh.data.Mastery m : com.evandouard.hxh.data.Mastery.values()) {
            d.setMasteryLevel(m, com.evandouard.hxh.data.Mastery.MAX_LEVEL);
        }
        d.setHealUnlocked(true);
        d.setHealLevel(HunterData.HEAL_MAX_LEVEL);
        com.evandouard.hxh.nen.NenLearning.learnAll(entity); // toutes les capacités apprises
        d.setAuraMax(NenModes.computeAuraMax(d));
        d.setAura(d.auraMax());
        HunterStats.applyAttributes(entity);
        entity.setHealth(entity.getMaxHealth());
    }

    private static void resetAll(LivingEntity entity) {
        HunterStats.get(entity).reset();
        HunterStats.applyAttributes(entity);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statCommand() {
        return Commands.literal("stat").requires(s -> s.hasPermission(2))
                .then(Commands.argument("stat", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(Stat.values()).map(Stat::id), b))
                        .then(Commands.literal("set")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, Stat.MAX_LEVEL))
                                        .executes(ctx -> setStat(ctx, List.of(ctx.getSource().getEntityOrException())))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(ctx -> setStat(ctx, EntityArgument.getEntities(ctx, "targets"))))))
                        .then(Commands.literal("addxp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addXp(ctx, List.of(ctx.getSource().getEntityOrException())))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(ctx -> addXp(ctx, EntityArgument.getEntities(ctx, "targets")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nenCommand() {
        return Commands.literal("nen").requires(s -> s.hasPermission(2))
                .then(Commands.literal("type")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(NenType.values()).map(NenType::id), b))
                                .executes(ctx -> setNenType(ctx, List.of(ctx.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(ctx -> setNenType(ctx, EntityArgument.getEntities(ctx, "targets"))))))
                .then(targeted("unlock", NenModes::unlock))
                .then(targeted("lock", e -> {
                    HunterData d = HunterStats.get(e);
                    d.setNenUnlocked(false);
                    d.setNenRevealed(false);
                    d.setMode(com.evandouard.hxh.data.NenMode.NONE);
                    d.setHealing(false);
                    d.setMeditationTicks(0);
                }))
                .then(targeted("reveal", e -> HunterStats.get(e).setNenRevealed(true)))
                // Apprendre une capacité (ou toutes) sans passer par le maître
                .then(Commands.literal("learn")
                        .then(Commands.argument("ability", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(
                                        java.util.stream.Stream.of("all"),
                                        Arrays.stream(com.evandouard.hxh.nen.NenLearning.Ability.values()).map(com.evandouard.hxh.nen.NenLearning.Ability::id)), b))
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "ability");
                                    LivingEntity e = (LivingEntity) ctx.getSource().getEntityOrException();
                                    if (id.equals("all")) com.evandouard.hxh.nen.NenLearning.learnAll(e);
                                    else if (com.evandouard.hxh.nen.NenLearning.Ability.byId(id) != null) HunterStats.get(e).learn(id);
                                    else return 0;
                                    return 1;
                                })))
                .then(targeted("refill", e -> {
                    HunterData d = HunterStats.get(e);
                    d.setAura(d.auraMax());
                    // et les jauges du personnage (Prière de Netero…)
                    for (var bar : com.evandouard.hxh.hatsu.KitBars.of(d.kit())) d.setKitValue(bar.key(), bar.max());
                }))
                .then(Commands.literal("talent")
                        .then(Commands.argument("talent", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(com.evandouard.hxh.data.NenTalent.values()).map(com.evandouard.hxh.data.NenTalent::id), b))
                                .executes(ctx -> {
                                    com.evandouard.hxh.data.NenTalent t = com.evandouard.hxh.data.NenTalent.byId(StringArgumentType.getString(ctx, "talent"));
                                    if (t == null) return 0;
                                    HunterStats.get(ctx.getSource().getPlayerOrException()).setTalent(t);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.talent_set",
                                            Component.translatable(t.translationKey())), false);
                                    return 1;
                                })))
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(com.evandouard.hxh.data.NenMode.values()).map(com.evandouard.hxh.data.NenMode::id), b))
                                .executes(ctx -> setMode(ctx, List.of(ctx.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(ctx -> setMode(ctx, EntityArgument.getEntities(ctx, "targets"))))))
                .then(Commands.literal("level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, com.evandouard.hxh.data.Mastery.MAX_LEVEL))
                                .executes(ctx -> apply(ctx, List.of(ctx.getSource().getEntityOrException()),
                                        e -> HunterStats.get(e).setNenLevel(IntegerArgumentType.getInteger(ctx, "level"))))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(ctx -> apply(ctx, EntityArgument.getEntities(ctx, "targets"),
                                                e -> HunterStats.get(e).setNenLevel(IntegerArgumentType.getInteger(ctx, "level")))))))
                .then(Commands.literal("mastery")
                        .then(Commands.argument("mastery", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(com.evandouard.hxh.data.Mastery.values()).map(com.evandouard.hxh.data.Mastery::id), b))
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, com.evandouard.hxh.data.Mastery.MAX_LEVEL))
                                        .executes(ctx -> {
                                            com.evandouard.hxh.data.Mastery m = com.evandouard.hxh.data.Mastery.byId(
                                                    StringArgumentType.getString(ctx, "mastery"));
                                            if (m == null) throw UNKNOWN_STAT.create(StringArgumentType.getString(ctx, "mastery"));
                                            return apply(ctx, List.of(ctx.getSource().getEntityOrException()),
                                                    e -> HunterStats.get(e).setMasteryLevel(m, IntegerArgumentType.getInteger(ctx, "level")));
                                        }))))
                .then(Commands.literal("meditation")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(ctx -> apply(ctx, List.of(ctx.getSource().getEntityOrException()),
                                        e -> HunterStats.get(e).setMeditationTicks(
                                                IntegerArgumentType.getInteger(ctx, "seconds") * 20)))))
                .then(Commands.literal("heal")
                        .then(targeted("unlock", e -> HunterStats.get(e).setHealUnlocked(true)))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, HunterData.HEAL_MAX_LEVEL))
                                        .executes(ctx -> apply(ctx, List.of(ctx.getSource().getEntityOrException()),
                                                e -> HunterStats.get(e).setHealLevel(
                                                        IntegerArgumentType.getInteger(ctx, "level")))))))
                .then(Commands.literal("color")
                        .then(Commands.argument("hex", StringArgumentType.word())
                                .executes(ctx -> {
                                    String hex = StringArgumentType.getString(ctx, "hex").replace("#", "");
                                    int rgb;
                                    try {
                                        rgb = Integer.parseInt(hex, 16);
                                    } catch (NumberFormatException e) {
                                        throw BAD_COLOR.create(hex);
                                    }
                                    return apply(ctx, List.of(ctx.getSource().getEntityOrException()),
                                            e -> HunterStats.get(e).setAuraColor(rgb));
                                })))
                // Part du Ryu en attaque (0 à 100 %), sans passer par l'écran du Ryu
                .then(Commands.literal("ryu")
                        .then(Commands.argument("attack", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> apply(ctx, List.of(ctx.getSource().getEntityOrException()),
                                        e -> HunterStats.get(e).setRyuAttack(IntegerArgumentType.getInteger(ctx, "attack"))))));
    }

    /** Administration du Hatsu : forcer un personnage (y compris sur un mob), effacer, faire lancer. */
    /** Serments : prêter (tous), renoncer (= rompre, le Hatsu est scellé), effacer sans pénalité (admin). */
    private static LiteralArgumentBuilder<CommandSourceStack> vowCommand() {
        return Commands.literal("vow")
                .then(Commands.literal("list").executes(ctx -> {
                    for (var v : com.evandouard.hxh.hatsu.Vows.Vow.values()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(v.id() + " ×" + v.power + " — ")
                                .append(Component.translatable(v.translationKey())), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("take").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(
                                com.evandouard.hxh.hatsu.Vows.Vow.values()).map(com.evandouard.hxh.hatsu.Vows.Vow::id), b))
                        .executes(ctx -> {
                            LivingEntity e = (LivingEntity) ctx.getSource().getEntityOrException();
                            String refusal = com.evandouard.hxh.hatsu.Vows.take(e, StringArgumentType.getString(ctx, "id"));
                            if (refusal != null) ctx.getSource().sendFailure(Component.translatable(refusal));
                            return refusal == null ? 1 : 0;
                        })))
                .then(Commands.literal("toggle").executes(ctx -> {
                    String key = com.evandouard.hxh.hatsu.Vows.toggle((LivingEntity) ctx.getSource().getEntityOrException());
                    ctx.getSource().sendSuccess(() -> Component.translatable(key), false);
                    return 1;
                }))
                .then(Commands.literal("renounce").executes(ctx -> {
                    com.evandouard.hxh.hatsu.Vows.breakVow((LivingEntity) ctx.getSource().getEntityOrException(), "vow.hxh.reason.renounced");
                    return 1;
                }))
                .then(Commands.literal("clear").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    HunterData d = HunterStats.get((LivingEntity) ctx.getSource().getEntityOrException());
                    d.setVow("", "", 0L);
                    d.setSealedUntil(0L);
                    d.setNenSealedUntil(0L);
                    return 1;
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> hatsuCommand() {
        return Commands.literal("hatsu").requires(s -> s.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("kit", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        java.util.stream.Stream.concat(java.util.stream.Stream.of("none"),
                                                HatsuKits.all().stream().map(HatsuKit::id)), b))
                                .executes(ctx -> setKit(ctx, List.of(ctx.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(ctx -> setKit(ctx, EntityArgument.getEntities(ctx, "targets"))))))
                .then(targeted("clear", e -> HunterStats.get(e).setKit("")))
                .then(targeted("cast", e -> Hatsu.requestCast(e)))
                .then(targeted("release", e -> Hatsu.releaseCast(e)))
                .then(targeted("next", e -> Hatsu.cycle(e)))
                .then(Commands.literal("select").then(Commands.argument("technique", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Techniques.allIds(), b))
                        .executes(ctx -> Hatsu.select((LivingEntity) ctx.getSource().getEntityOrException(),
                                StringArgumentType.getString(ctx, "technique")) ? 1 : 0)));
    }

    private static int setKit(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "kit");
        if (id.equals("none")) {
            // Retour à l'état d'avant le choix (le Nen et le type restent)
            return apply(ctx, targets, e -> HunterStats.get(e).setKit(""));
        }
        HatsuKit kit = HatsuKits.get(id);
        if (kit == null) throw UNKNOWN_KIT.create(id);
        return apply(ctx, targets, e -> {
            HunterData d = HunterStats.get(e);
            com.evandouard.hxh.fx.NenFx.clearAll(e);
            d.setNenType(kit.type());
            if (!d.nenUnlocked()) NenModes.unlock(e);
            d.setNenRevealed(true);
            d.setKit(kit.id());
            d.setSelected(0); // première technique du personnage
        });
    }

    /** Sous-commande appliquée à soi-même ou à des cibles : {@code <nom> [cibles]}. */
    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String name, Consumer<LivingEntity> action) {
        return Commands.literal(name)
                .executes(ctx -> apply(ctx, List.of(ctx.getSource().getEntityOrException()), action))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> apply(ctx, EntityArgument.getEntities(ctx, "targets"), action)));
    }

    private static Stat statArg(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "stat");
        Stat stat = Stat.byId(id);
        if (stat == null) throw UNKNOWN_STAT.create(id);
        return stat;
    }

    private static int setStat(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets)
            throws CommandSyntaxException {
        Stat stat = statArg(ctx);
        int level = IntegerArgumentType.getInteger(ctx, "level");
        return apply(ctx, targets, e -> HunterStats.setLevel(e, stat, level));
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets)
            throws CommandSyntaxException {
        Stat stat = statArg(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        // Même chemin que l'XP gagnée en jeu (montées de niveau notifiées)
        return apply(ctx, targets, e -> HunterStats.addXp(e, stat, amount));
    }

    /**
     * /hunter structure save &lt;nom&gt; &lt;coin1&gt; &lt;coin2&gt; : sauvegarde une zone de n'importe quelle taille en structure
     * (le bloc de structure vanilla est limité à 48 × 48 × 48). Le fichier va dans generated/&lt;ns&gt;/structures/.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> structureCommand() {
        return Commands.literal("structure").requires(s -> s.hasPermission(2))
                .then(Commands.literal("save")
                        .then(Commands.argument("name", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                .then(Commands.argument("from", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .then(Commands.argument("to", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                                .executes(ctx -> saveStructure(ctx.getSource(),
                                                        net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "name"),
                                                        net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                                        net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "to")))))));
    }

    private static int saveStructure(CommandSourceStack source, net.minecraft.resources.ResourceLocation id,
                                     net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b) {
        net.minecraft.core.Vec3i size = com.evandouard.hxh.arena.StructureSaver.save(source.getLevel(), id, a, b);
        if (size == null) {
            source.sendFailure(Component.translatable("command.hxh.structure.failed", id.toString()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.hxh.structure.saved", id.toString(),
                size.getX(), size.getY(), size.getZ(),
                "generated/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /hunter arena : bilan pour tous ; entrer, sortir, combattre et régler l'étage pour les admins (tests). */
    private static LiteralArgumentBuilder<CommandSourceStack> arenaCommand() {
        return Commands.literal("arena")
                .executes(ctx -> arenaInfo(ctx.getSource()))
                .then(Commands.literal("info").executes(ctx -> arenaInfo(ctx.getSource())))
                .then(Commands.literal("enter").requires(s -> s.hasPermission(2)).executes(ctx ->
                        com.evandouard.hxh.arena.ArenaManager.enter(ctx.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("leave").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    com.evandouard.hxh.arena.ArenaManager.leave(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("fight").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    Component refusal = com.evandouard.hxh.arena.ArenaManager.startFight(ctx.getSource().getPlayerOrException());
                    if (refusal != null) {
                        ctx.getSource().sendFailure(refusal);
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("goto").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("floor", IntegerArgumentType.integer(0, com.evandouard.hxh.arena.ArenaRules.TOP_FLOOR))
                                .executes(ctx -> {
                                    int floor = IntegerArgumentType.getInteger(ctx, "floor");
                                    int dest = floor == 0 ? 0 : com.evandouard.hxh.arena.ArenaWorld.locationOfBracket(
                                            com.evandouard.hxh.arena.ArenaBrackets.indexOf(floor));
                                    String refusal = com.evandouard.hxh.arena.ArenaManager.goTo(ctx.getSource().getPlayerOrException(), dest);
                                    if (refusal != null) {
                                        ctx.getSource().sendFailure(Component.translatable(refusal));
                                        return 0;
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("room").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    String refusal = com.evandouard.hxh.arena.ArenaManager.goTo(ctx.getSource().getPlayerOrException(),
                            com.evandouard.hxh.arena.ArenaManager.ROOM);
                    if (refusal != null) {
                        ctx.getSource().sendFailure(Component.translatable(refusal));
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("register").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                    com.evandouard.hxh.quest.QuestData d = com.evandouard.hxh.quest.QuestManager.data(p);
                    d.setArenaRegistered(true);
                    d.setRegisteredBracket(com.evandouard.hxh.arena.ArenaBrackets.indexOf(d.arenaFloor()));
                    if (d.arenaFloor() >= com.evandouard.hxh.arena.ArenaRules.NEN_FLOOR) d.setRegistered200(true);
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.arena_register"), false);
                    return 1;
                }))
                .then(Commands.literal("rebuild").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    net.minecraft.server.level.ServerLevel arena = com.evandouard.hxh.arena.ArenaWorld.level(ctx.getSource().getServer());
                    if (arena != null) com.evandouard.hxh.arena.ArenaWorld.forgetBuilds(arena);
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.arena_rebuild"), false);
                    return 1;
                }))
                .then(Commands.literal("floor").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1, com.evandouard.hxh.arena.ArenaRules.TOP_FLOOR))
                                .executes(ctx -> {
                                    net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    int floor = IntegerArgumentType.getInteger(ctx, "floor");
                                    com.evandouard.hxh.quest.QuestData d = com.evandouard.hxh.quest.QuestManager.data(p);
                                    d.setArenaFloor(floor);
                                    d.setArenaRegistered(true);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.arena_floor", floor), false);
                                    return 1;
                                })));
    }

    private static int arenaInfo(CommandSourceStack source) throws CommandSyntaxException {
        com.evandouard.hxh.quest.QuestData d = com.evandouard.hxh.quest.QuestManager.data(source.getPlayerOrException());
        source.sendSuccess(() -> Component.translatable("command.hxh.arena_info",
                d.arenaFloor(), d.arenaWins(), d.arenaLosses(), d.jenny()).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    /** Fait apparaître autour du joueur le site d'examen provisoire (en attendant les structures). */
    /** Un Navigateur à côté de soi (tests ; en jeu, il attend près du point d'apparition). */
    private static int examSetup(CommandSourceStack source) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        com.evandouard.hxh.npc.NpcEntity npc = com.evandouard.hxh.registry.ModEntities.NPC.get().create(level);
        if (npc == null) return 0;
        net.minecraft.core.BlockPos pos = player.blockPosition().offset(3, 0, 0);
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        npc.setNpcId("navigator");
        npc.setHome(pos);
        level.addFreshEntity(npc);
        source.sendSuccess(() -> Component.translatable("command.hxh.exam_setup"), false);
        return 1;
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "mode");
        com.evandouard.hxh.data.NenMode mode = com.evandouard.hxh.data.NenMode.byId(id);
        if (mode == null) throw UNKNOWN_NEN.create(id);
        return apply(ctx, targets, e -> NenModes.trySetMode(e, mode));
    }

    private static int setNenType(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "type");
        NenType type = NenType.byId(id);
        if (type == null) throw UNKNOWN_NEN.create(id);
        return apply(ctx, targets, e -> HunterStats.get(e).setNenType(type));
    }

    /** Applique une modification à chaque entité vivante de la liste puis affiche son résumé. */
    private static int apply(CommandContext<CommandSourceStack> ctx, Collection<? extends Entity> targets,
                             Consumer<LivingEntity> action) {
        int count = 0;
        for (Entity e : targets) {
            if (e instanceof LivingEntity living) {
                action.accept(living);
                show(ctx.getSource(), living);
                count++;
            }
        }
        return count;
    }

    private static int show(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return 0;
        HunterData data = HunterStats.get(living);
        List<Component> lines = new ArrayList<>(com.evandouard.hxh.stats.ProfileText.statsAndNen(living));
        if (living instanceof net.minecraft.server.level.ServerPlayer sp) {
            lines.add(Component.translatable("command.hxh.show.hunter", Component.translatable(
                    com.evandouard.hxh.quest.QuestManager.data(sp).isHunter() ? "command.hxh.yes" : "command.hxh.no")));
        }
        Component nenType = data.nenType() == null
                ? Component.translatable("nen.hxh.none")
                : Component.translatable(data.nenType().translationKey());
        if (source.hasPermission(2)) {
            lines.add(Component.translatable("command.hxh.show.nen_admin", nenType).withStyle(ChatFormatting.DARK_GRAY));
        }
        for (Component line : lines) source.sendSuccess(() -> line, false);
        return 1;
    }

    private static int setTemper(CommandSourceStack source, boolean wild) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!(source.getEntityOrException() instanceof LivingEntity living)) return 0;
        HunterStats.get(living).setWildAura(wild);
        source.sendSuccess(() -> Component.translatable(wild ? "command.hxh.aura.wild" : "command.hxh.aura.calm"), false);
        return 1;
    }

    private static int jenny(CommandSourceStack source, int amount, boolean set) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        com.evandouard.hxh.quest.QuestData d = com.evandouard.hxh.quest.QuestManager.data(player);
        d.addJenny(set ? amount - d.jenny() : amount);
        int now = d.jenny();
        source.sendSuccess(() -> Component.translatable("command.hxh.jenny", now), false);
        return 1;
    }

    private static int enShape(net.minecraft.commands.CommandSourceStack src, String shape, float a, float b)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        LivingEntity e = (LivingEntity) src.getEntityOrException();
        String refusal = com.evandouard.hxh.nen.NenSenses.chooseEnShape(e, shape, a, b);
        if (refusal != null) {
            src.sendFailure(Component.translatable(refusal));
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("message.hxh.en_shape_set"), false);
        return 1;
    }
}
