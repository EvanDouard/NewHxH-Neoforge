package com.evandouard.hxh.command;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
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
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("command.hxh.show.header", living.getDisplayName(), data.hunterLevel())
                .withStyle(ChatFormatting.GOLD));
        for (Stat stat : Stat.values()) {
            int level = data.level(stat);
            MutableComponent xpPart = level >= Stat.MAX_LEVEL
                    ? Component.translatable("command.hxh.show.max")
                    : Component.translatable("command.hxh.show.xp", data.xp(stat), Stat.xpToNext(level));
            lines.add(Component.translatable("command.hxh.show.stat",
                    Component.translatable(stat.translationKey()), level, xpPart));
        }
        Component nenType = data.nenType() == null
                ? Component.translatable("nen.hxh.none")
                : Component.translatable(data.nenType().translationKey());
        if (!data.nenUnlocked()) {
            lines.add(Component.translatable("command.hxh.show.nen_locked"));
        } else if (data.nenRevealed()) {
            lines.add(Component.translatable("command.hxh.show.nen", nenType));
        } else {
            lines.add(Component.translatable("command.hxh.show.nen", "???"));
        }
        if (data.nenUnlocked()) {
            lines.add(Component.translatable("command.hxh.show.aura", (int) data.aura(), data.auraMax(),
                    Component.translatable(data.mode().translationKey())));
        }
        if (data.healUnlocked()) {
            lines.add(Component.translatable("command.hxh.show.heal", data.healLevel(),
                    data.healLevel() >= HunterData.HEAL_MAX_LEVEL
                            ? Component.translatable("command.hxh.show.max")
                            : Component.translatable("command.hxh.show.xp", data.healXp(), HunterData.healXpToNext(data.healLevel()))));
        }
        if (!data.nenUnlocked() && data.meditationTicks() > 0) {
            lines.add(Component.translatable("command.hxh.show.meditation", data.meditationTicks() / 20));
        }
        if (source.hasPermission(2)) {
            lines.add(Component.translatable("command.hxh.show.nen_admin", nenType).withStyle(ChatFormatting.DARK_GRAY));
        }
        for (Component line : lines) source.sendSuccess(() -> line, false);
        return 1;
    }
}
