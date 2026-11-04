package com.evandouard.hxh.arena;

import com.evandouard.hxh.HxHConfig;

/**
 * Règles chiffrées de la Tour Céleste.
 * <ul>
 *     <li>Sous le 200e : victoire = +5 étages (+3 si elle est rapide), sans jamais sauter le 200e.</li>
 *     <li>À partir du 200e : victoire = +1 étage, jusqu'au 251e (Maître d'étage).</li>
 *     <li>Défaite : retour au dernier étage de la tranche d'en dessous, qu'il faut repasser ;
 *         dans la première tranche, −5 étages.</li>
 * </ul>
 * Les pas sont réglables dans la config serveur ({@code arena.*}).
 */
public final class ArenaRules {
    public static final int NEN_FLOOR = 200;
    public static final int TOP_FLOOR = 251;
    public static final int FAST_WIN_TICKS = 40 * 20;
    public static final int FIGHT_TIME_LIMIT_TICKS = 3 * 60 * 20;

    private ArenaRules() {}

    public static boolean isFloorMaster(int floor) {
        return floor >= TOP_FLOOR;
    }

    public static int floorAfterWin(int floor, boolean fast) {
        if (floor < NEN_FLOOR) {
            int step = HxHConfig.ARENA_WIN_STEP.get() + (fast ? HxHConfig.ARENA_FAST_WIN_BONUS.get() : 0);
            return Math.min(NEN_FLOOR, floor + step);
        }
        return Math.min(TOP_FLOOR, floor + HxHConfig.ARENA_WIN_STEP_ABOVE_200.get());
    }

    public static int floorAfterLoss(int floor) {
        ArenaBracket bracket = ArenaBrackets.forFloor(floor);
        if (bracket.index() == 0) return Math.max(1, floor - HxHConfig.ARENA_LOSS_DROP_FIRST_BRACKET.get());
        return Math.max(1, ArenaBrackets.get(bracket.index() - 1).to());
    }

    /** Jenny gagnés : rien au-dessus du 200e (on s'y bat pour l'honneur). */
    public static int jennyForWin(int floor) {
        return floor < NEN_FLOOR ? 50 + floor * 10 : 0;
    }

    public static int opponentStatLevel(int floor) {
        if (floor < NEN_FLOOR) return (int) Math.round(floor * 0.3);
        return Math.min(85, 60 + (floor - NEN_FLOOR) / 2);
    }

    public static int opponentNenLevel(int floor) {
        return floor < NEN_FLOOR ? 0 : Math.min(80, 20 + (floor - NEN_FLOOR));
    }
}
