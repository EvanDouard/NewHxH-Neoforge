package com.evandouard.hxh.arena;

/**
 * Règles chiffrées de la Tour Céleste (pures, testables).
 * <ul>
 *     <li>Étages 1 à 199 : adversaires sans Nen ; victoire = +10 étages (+20 si victoire rapide), défaite = −5.</li>
 *     <li>Arrivée au 200e : on ne peut pas le dépasser par un saut, il faut y arriver (190–199 → 200).</li>
 *     <li>200 et plus : adversaires avec Nen et Hatsu ; victoire = +10 étages jusqu'au 250e, défaite sans recul.</li>
 * </ul>
 */
public final class ArenaRules {
    public static final int NEN_FLOOR = 200;
    public static final int TOP_FLOOR = 250;
    public static final int FAST_WIN_TICKS = 40 * 20;
    public static final int FIGHT_TIME_LIMIT_TICKS = 3 * 60 * 20;

    private ArenaRules() {}

    public static int floorAfterWin(int floor, boolean fast) {
        if (floor < NEN_FLOOR) return Math.min(NEN_FLOOR, floor + (fast ? 20 : 10));
        return Math.min(TOP_FLOOR, floor + 10);
    }

    public static int floorAfterLoss(int floor) {
        return floor < NEN_FLOOR ? Math.max(1, floor - 5) : floor;
    }

    /** Jenny gagnés : rien au-dessus du 200e (on s'y bat pour l'honneur). */
    public static int jennyForWin(int floor) {
        return floor < NEN_FLOOR ? 50 + floor * 10 : 0;
    }

    /** Niveau des stats d'un adversaire selon l'étage : faible en bas, ~60 au 199e, jusqu'à 85 en haut. */
    public static int opponentStatLevel(int floor) {
        if (floor < NEN_FLOOR) return (int) Math.round(floor * 0.3);
        return Math.min(85, 60 + (floor - NEN_FLOOR) / 2);
    }

    /** Niveau de Nen d'un adversaire (0 = pas de Nen). */
    public static int opponentNenLevel(int floor) {
        return floor < NEN_FLOOR ? 0 : Math.min(80, 20 + (floor - NEN_FLOOR));
    }
}
