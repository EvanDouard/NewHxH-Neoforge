package com.evandouard.hxh.exam;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Tour de Trick (Phase 3) : 72 heures pour descendre du toit — ici quelques minutes.
 * <ul>
 *   <li>le toit cache plusieurs trappes ; une seule s'ouvre sur l'étage du dessous (les autres sur la pierre) ;</li>
 *   <li>au 1er étage, le chemin de la majorité : cinq candidats votent O ou X, la majorité choisit la porte (Tonpa
 *       vote toujours contre toi) ; perdre le vote rallonge le chemin ;</li>
 *   <li>au 2e étage, un prisonnier défie le candidat (on gagne en le faisant abandonner) ;</li>
 *   <li>puis l'escalier jusqu'à la sortie, avant la fin du compte à rebours.</li>
 * </ul>
 * Les points à atteindre sont la route du lieu ({@code route} : 0 = étage de la trappe, 1 = 2e étage, 2 = sortie,
 * 3 = début du long chemin).
 * <p>Le temps court depuis l'annonce de Lippo : la sortie doit être atteinte avant {@code seconds} au total, moins
 * le temps perdu aux paris de Leroute. Le prisonnier du 2e étage dépend de l'exemplaire de la tour : Majitani,
 * Jo-ness (plus coriace) ou Leroute (des paris à pile ou face où l'on mise du temps).
 */
public final class TrickTower {
    private TrickTower() {}

    /** Rayon autour d'un point de la route pour le considérer atteint. */
    static final double REACH = 3.0;

    /** Le joueur est-il au point {@code index} de la route du lieu où il se trouve ? */
    public static boolean reached(ServerPlayer player, int index) {
        ExamSite site = ExamHazards.siteAt(player.blockPosition());
        if (site == null) return false;
        ExamWorld.Site s = ExamWorld.cached(site, ExamWorld.instanceAt(player.blockPosition()));
        if (s == null || index < 0 || index >= s.route().size()) return false;
        Vec3 p = Vec3.atBottomCenterOf(s.route().get(index));
        return Math.abs(player.getY() - p.y) < 2.5 && player.position().subtract(p).horizontalDistanceSqr() < REACH * REACH;
    }

    /**
     * Vote de la majorité : le joueur (choix 1 = O, 2 = X), Tonpa (toujours l'inverse) et trois autres au hasard.
     * La majorité ouvre une porte ; si elle va contre le joueur, le chemin est plus long (on le dit, et on y va
     * quand même). Le candidat est ensuite conduit au 2e étage.
     */
    public static boolean vote(ServerPlayer player, int choice) {
        boolean mineIsO = choice != 2;
        int forO = mineIsO ? 1 : 0;
        if (!mineIsO) forO++; // Tonpa vote toujours contre
        for (int i = 0; i < 3; i++) if (player.getRandom().nextBoolean()) forO++;
        int forX = 5 - forO;
        boolean oWins = forO > forX;
        boolean won = oWins == mineIsO;
        player.sendSystemMessage(Component.translatable("exam.hxh.tower_vote", forO, forX,
                Component.translatable(oWins ? "exam.hxh.tower_o" : "exam.hxh.tower_x")).withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable(won ? "exam.hxh.tower_vote_won" : "exam.hxh.tower_vote_lost")
                .withStyle(won ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        // la porte mène au 2e étage (un long détour si la majorité a voté contre toi)
        ExamSite site = ExamHazards.siteAt(player.blockPosition());
        ExamWorld.Site s = site == null ? null : ExamWorld.cached(site, ExamWorld.instanceAt(player.blockPosition()));
        if (s != null && s.route().size() > 3 && ExamTrips.enabled(player)) {
            BlockPos to = s.route().get(won ? 1 : 3);
            player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        }
        return won;
    }

    /** Le joueur est-il au 2e étage (où l'attend le prisonnier) ? */
    public static boolean onSecondFloor(ServerPlayer player) {
        ExamSite site = ExamHazards.siteAt(player.blockPosition());
        if (site != ExamSite.TOWER) return false;
        ExamWorld.Site s = ExamWorld.cached(site, ExamWorld.instanceAt(player.blockPosition()));
        if (s == null || s.route().size() < 2) return false;
        BlockPos f2 = s.route().get(1);
        BlockPos c = ExamWorld.centerAt(player.blockPosition());
        return Math.abs(player.getY() - f2.getY()) < 3 && Math.abs(player.getX() - c.getX() - 0.5) < 7 && Math.abs(player.getZ() - c.getZ() - 0.5) < 7;
    }

    // ------------------------------------------------------------------ Horloge de la tour

    /** Tick de jeu où l'épreuve a commencé (annonce de Lippo), et secondes perdues aux paris. */
    private static final java.util.Map<java.util.UUID, Long> START = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Integer> PENALTY = new java.util.HashMap<>();
    /** Secondes perdues par pari raté contre Leroute ; nombre de paris. */
    static final int BET_COST = 25, BETS = 3;

    public static void startClock(ServerPlayer player) {
        START.put(player.getUUID(), player.serverLevel().getGameTime());
        PENALTY.remove(player.getUUID());
        BETS_DONE.remove(player.getUUID());
    }

    /** Ticks écoulés dans la tour (paris perdus compris), ou -1 si l'horloge ne tourne pas. */
    public static int elapsedTicks(ServerPlayer player) {
        Long t0 = START.get(player.getUUID());
        if (t0 == null) return -1;
        return (int) (player.serverLevel().getGameTime() - t0) + PENALTY.getOrDefault(player.getUUID(), 0) * 20;
    }

    public static int penaltySeconds(ServerPlayer player) {
        return PENALTY.getOrDefault(player.getUUID(), 0);
    }

    public static void stopClock(ServerPlayer player) {
        START.remove(player.getUUID());
        PENALTY.remove(player.getUUID());
        BETS_DONE.remove(player.getUUID());
        ASKED.remove(player.getUUID());
    }

    // ------------------------------------------------------------------ Prisonniers

    public enum Prisoner { MAJITANI, JONESS, LEROUTE }

    /** Pour les tests : prisonnier imposé. */
    public static Prisoner forcedForTests;

    /** Le prisonnier du 2e étage, selon l'exemplaire de la tour qu'a reçu le joueur. */
    public static Prisoner prisoner(ServerPlayer player) {
        if (forcedForTests != null) return forcedForTests;
        int instance = com.evandouard.hxh.quest.QuestManager.data(player).examInstance(ExamSite.TOWER.id());
        return Prisoner.values()[Math.floorMod(instance, Prisoner.values().length)];
    }

    private static final java.util.Map<java.util.UUID, Integer> BETS_DONE = new java.util.HashMap<>();
    private static final java.util.Set<java.util.UUID> ASKED = new java.util.HashSet<>();

    /** Chaque seconde de l'étape du prisonnier, quand c'est Leroute : proposer le pari s'il ne l'est pas. */
    public static void gambleTick(ServerPlayer player) {
        if (ASKED.contains(player.getUUID())) return;
        ASKED.add(player.getUUID());
        int n = BETS_DONE.getOrDefault(player.getUUID(), 0) + 1;
        sayLeroute(player, Component.translatable(n == 1 ? "exam.hxh.leroute_intro" : "exam.hxh.leroute_next", n, BETS, BET_COST));
        var line = Component.literal("  ");
        for (int i = 1; i <= 2; i++) {
            final int k = i;
            line.append(Component.translatable(i == 1 ? "exam.hxh.leroute_heads" : "exam.hxh.leroute_tails").withStyle(st -> st
                    .withColor(ChatFormatting.GREEN).withUnderlined(true)
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/hunter exam bet " + k))))
                    .append("   ");
        }
        player.sendSystemMessage(line);
    }

    private static void sayLeroute(ServerPlayer player, Component line) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.literal("Leroute").withStyle(ChatFormatting.YELLOW), line));
    }

    /** Un pari : 1 = pile, 2 = face. Perdu, on perd du temps. Après {@link #BETS} paris, Leroute laisse passer. */
    public static boolean bet(ServerPlayer player, int choice) {
        if (!ASKED.remove(player.getUUID()) || prisoner(player) != Prisoner.LEROUTE) return false;
        boolean heads = player.getRandom().nextBoolean();
        boolean won = (choice == 1) == heads;
        if (!won) PENALTY.merge(player.getUUID(), BET_COST, Integer::sum);
        sayLeroute(player, Component.translatable(won ? "exam.hxh.leroute_won" : "exam.hxh.leroute_lost",
                Component.translatable(heads ? "exam.hxh.leroute_heads" : "exam.hxh.leroute_tails"), penaltySeconds(player)));
        int done = BETS_DONE.merge(player.getUUID(), 1, Integer::sum);
        if (done >= BETS) {
            sayLeroute(player, Component.translatable("exam.hxh.leroute_done"));
            BETS_DONE.remove(player.getUUID());
            com.evandouard.hxh.quest.QuestManager.duelWon(player);
        }
        return won;
    }
}
