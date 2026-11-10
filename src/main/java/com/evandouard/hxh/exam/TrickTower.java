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
 * Les points à atteindre sont la route du lieu ({@code route} : 0 = étage de la trappe, 1 = 2e étage, 2 = sortie).
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
        if (s != null && s.route().size() > 1 && ExamTrips.enabled(player)) {
            BlockPos to = s.route().get(1);
            player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        }
        return won;
    }
}
