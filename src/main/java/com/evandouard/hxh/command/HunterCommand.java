package com.evandouard.hxh.command;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.HatsuKits;
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
                .then(nenCommand())
                .then(hatsuCommand())
                .then(Commands.literal("exam").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("setup").executes(ctx -> examSetup(ctx.getSource()))))
                .then(arenaCommand())
                .then(structureCommand())
                .then(Commands.literal("quest").requires(src -> src.hasPermission(2))
                        .then(Commands.literal("reset").executes(ctx -> {
                            net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayerOrException();
                            com.evandouard.hxh.quest.QuestManager.data(p).reset();
                            com.evandouard.hxh.quest.QuestManager.sync(p);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.hxh.quest_reset"), false);
                            return 1;
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
                                })));
    }

    /** Administration du Hatsu : forcer un personnage (y compris sur un mob), effacer, faire lancer. */
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
                .then(targeted("next", e -> Hatsu.cycle(e)));
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
            d.setNenType(kit.type());
            if (!d.nenUnlocked()) NenModes.unlock(e);
            d.setNenRevealed(true);
            d.setKit(kit.id());
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
        return apply(ctx, targets, e -> {
            HunterStats.get(e).addXp(stat, amount);
            HunterStats.applyAttributes(e);
        });
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
    private static int examSetup(CommandSourceStack source) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        String[] npcs = {"navigator", "beans", "satotz", "buhara", "netero"};
        int[][] offsets = {{3, 0}, {6, 2}, {9, 0}, {6, -4}, {-4, 0}};
        for (int i = 0; i < npcs.length; i++) {
            com.evandouard.hxh.npc.NpcEntity npc = com.evandouard.hxh.registry.ModEntities.NPC.get().create(level);
            if (npc == null) continue;
            net.minecraft.core.BlockPos pos = player.blockPosition().offset(offsets[i][0], 0, offsets[i][1]);
            npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            npc.setNpcId(npcs[i]);
            npc.setHome(pos);
            if (npcs[i].equals("satotz")) {
                // Parcours de démonstration : une boucle de 8 points autour du site, qui revient près du départ
                java.util.List<net.minecraft.core.BlockPos> route = new java.util.ArrayList<>();
                for (int k = 1; k <= 8; k++) {
                    double a = k * Math.PI / 4;
                    int x = (int) Math.round(Math.cos(a) * 14), z = (int) Math.round(Math.sin(a) * 14);
                    net.minecraft.core.BlockPos p = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            pos.offset(x - 14, 0, z));
                    route.add(p);
                }
                npc.setRoute(route);
            }
            level.addFreshEntity(npc);
        }
        for (int i = 0; i < 3; i++) {
            net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            if (pig == null) continue;
            pig.moveTo(player.getX() + 10 + i, player.getY(), player.getZ() - 8, 0, 0);
            level.addFreshEntity(pig);
        }
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
}
