package com.evandouard.hxh.arena;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Paris sur les combats de la Tour Céleste. À chaque combat, le guichet des paris ouvre une cote d'après les
 * niveaux des deux combattants ; tous les joueurs de la Tour reçoivent des liens cliquables pour miser (le
 * combattant peut miser sur lui-même, jamais contre lui). Les mises sont prises tout de suite, les gains versés à
 * la fin du combat. Les paris ferment {@link #WINDOW_TICKS} après l'ouverture.
 */
public final class ArenaBets {
    private ArenaBets() {}

    public static final int WINDOW_TICKS = 20 * 20;
    public static final int[] STAKES = {100, 500, 1000};
    /** Part gardée par le guichet. */
    public static final float HOUSE_CUT = 0.1f;

    public record Bet(UUID bettor, boolean onPlayer, int stake, int payout) {}

    /** Un livre de paris par combat (clé : le joueur qui combat). */
    public static final class Book {
        final UUID fighterPlayer;
        final String playerName, opponentName;
        final float playerWinChance;
        final long closesAt;
        final List<Bet> bets = new ArrayList<>();

        Book(UUID fighterPlayer, String playerName, String opponentName, float playerWinChance, long closesAt) {
            this.fighterPlayer = fighterPlayer;
            this.playerName = playerName;
            this.opponentName = opponentName;
            this.playerWinChance = playerWinChance;
            this.closesAt = closesAt;
        }

        public float playerWinChance() {
            return playerWinChance;
        }

        public List<Bet> bets() {
            return List.copyOf(bets);
        }
    }

    public enum Result { OK, NO_FIGHT, CLOSED, NOT_ENOUGH_JENNY, AGAINST_SELF, INVALID }

    private static final Map<UUID, Book> BOOKS = new HashMap<>();

    /** Chance de victoire du joueur estimée par le guichet : niveau Hunter et de Nen contre ceux de l'adversaire. */
    public static float winChance(LivingEntity player, LivingEntity opponent) {
        HunterData a = HunterStats.get(player), b = HunterStats.get(opponent);
        float diff = (a.hunterLevel() - b.hunterLevel()) + 0.5f * (a.nenLevel() - b.nenLevel());
        return Math.max(0.1f, Math.min(0.9f, 0.5f + diff / 60f));
    }

    /** Gain total (mise comprise) pour une mise sur un camp qui a cette chance de gagner. */
    public static int payout(int stake, float chance) {
        return Math.max(stake, Math.round(stake / chance * (1f - HOUSE_CUT)));
    }

    /** Cote affichée, ex. « ×1,8 ». */
    public static String odds(float chance) {
        return String.format(java.util.Locale.FRANCE, "×%.1f", (1f - HOUSE_CUT) / chance);
    }

    @Nullable
    public static Book book(UUID fighterPlayer) {
        return BOOKS.get(fighterPlayer);
    }

    /** Ouvre les paris d'un combat et les annonce aux joueurs présents dans la Tour. */
    public static Book open(ServerPlayer fighter, LivingEntity opponent, long now) {
        Book book = new Book(fighter.getUUID(), fighter.getName().getString(), opponent.getDisplayName().getString(),
                winChance(fighter, opponent), now + WINDOW_TICKS);
        BOOKS.put(fighter.getUUID(), book);
        for (ServerPlayer p : fighter.server.getPlayerList().getPlayers()) {
            if (p.level().dimension().equals(ArenaWorld.DIMENSION) || p == fighter) announce(p, book, p == fighter);
        }
        return book;
    }

    private static void announce(ServerPlayer to, Book book, boolean self) {
        float pc = book.playerWinChance;
        MutableComponent line = Component.translatable("arena.hxh.bets.open", book.playerName, odds(pc),
                book.opponentName, odds(1f - pc), WINDOW_TICKS / 20).withStyle(ChatFormatting.GOLD);
        to.sendSystemMessage(line);
        to.sendSystemMessage(links(book, true, self ? Component.translatable("arena.hxh.bets.on_self") : Component.literal(book.playerName)));
        if (!self) to.sendSystemMessage(links(book, false, Component.literal(book.opponentName)));
    }

    private static Component links(Book book, boolean onPlayer, Component who) {
        MutableComponent row = Component.translatable("arena.hxh.bets.on", who).withStyle(ChatFormatting.GRAY);
        for (int stake : STAKES) {
            String cmd = "/hunter bet " + book.playerName + " " + (onPlayer ? "player" : "opponent") + " " + stake;
            row.append(" ").append(Component.literal("[" + stake + " J]").withStyle(s -> s.withColor(ChatFormatting.YELLOW)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("arena.hxh.bets.hover",
                            payout(stake, onPlayer ? book.playerWinChance : 1f - book.playerWinChance))))));
        }
        return row;
    }

    /** Mise de {@code bettor} sur le combat de {@code fighterPlayer}. */
    public static Result place(ServerPlayer bettor, UUID fighterPlayer, boolean onPlayer, int stake, long now) {
        Book book = BOOKS.get(fighterPlayer);
        if (book == null) return reply(bettor, Result.NO_FIGHT);
        if (stake <= 0) return reply(bettor, Result.INVALID);
        if (now > book.closesAt) return reply(bettor, Result.CLOSED);
        if (bettor.getUUID().equals(fighterPlayer) && !onPlayer) return reply(bettor, Result.AGAINST_SELF);
        QuestData d = QuestManager.data(bettor);
        if (d.jenny() < stake) return reply(bettor, Result.NOT_ENOUGH_JENNY);
        d.addJenny(-stake);
        int payout = payout(stake, onPlayer ? book.playerWinChance : 1f - book.playerWinChance);
        book.bets.add(new Bet(bettor.getUUID(), onPlayer, stake, payout));
        bettor.displayClientMessage(Component.translatable("arena.hxh.bets.placed", stake,
                onPlayer ? book.playerName : book.opponentName, payout).withStyle(ChatFormatting.YELLOW), false);
        return Result.OK;
    }

    private static Result reply(ServerPlayer p, Result r) {
        p.displayClientMessage(Component.translatable("arena.hxh.bets.refused." + r.name().toLowerCase(java.util.Locale.ROOT)), true);
        return r;
    }

    /** Fin du combat : on paie les gagnants. */
    public static void settle(MinecraftServer server, UUID fighterPlayer, boolean playerWon) {
        settle(id -> server.getPlayerList().getPlayer(id), fighterPlayer, playerWon);
    }

    /** Idem, avec la recherche des parieurs fournie (tests). */
    public static void settle(java.util.function.Function<UUID, ServerPlayer> players, UUID fighterPlayer, boolean playerWon) {
        Book book = BOOKS.remove(fighterPlayer);
        if (book == null) return;
        for (Bet bet : book.bets) {
            ServerPlayer p = players.apply(bet.bettor());
            if (p == null) continue; // parti : la mise est perdue
            if (bet.onPlayer() == playerWon) {
                QuestManager.data(p).addJenny(bet.payout());
                p.sendSystemMessage(Component.translatable("arena.hxh.bets.won", bet.payout()).withStyle(ChatFormatting.GREEN));
            } else {
                p.sendSystemMessage(Component.translatable("arena.hxh.bets.lost", bet.stake()).withStyle(ChatFormatting.RED));
            }
        }
    }

    /** Joueur dont on cherche le combat, par son nom (commande /hunter bet). */
    @Nullable
    public static UUID fighterByName(String name) {
        for (Book b : BOOKS.values()) if (b.playerName.equalsIgnoreCase(name)) return b.fighterPlayer;
        return null;
    }
}
