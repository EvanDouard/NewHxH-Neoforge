package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.hatsu.Technique;

/**
 * Débutant : chaque technique d'aura coûte beaucoup plus tant qu'elle n'est pas maîtrisée. Le surcoût part de
 * {@code beginnerCostMultiplier} (×2,5) à la maîtrise 0 et redescend en ligne droite jusqu'à ×1 au niveau
 * {@code beginnerCostUntil} (20). Les postures et sens suivent leur maîtrise, les Hatsu le niveau de Nen.
 * Vaut pour toute entité (un mob débutant paie autant qu'un joueur débutant).
 */
public final class NenCost {
    private NenCost() {}

    /**
     * L'aura est comptée en interne par petits nombres (un débutant : 100) ; tout ce qui s'affiche (barre, coûts,
     * régénération) est multiplié par cette unité, pour des nombres plus parlants : 1 000 d'aura au départ.
     */
    public static final float DISPLAY_UNIT = 10f;

    /** Valeur d'aura telle qu'on l'affiche. */
    public static int shown(double internal) {
        return (int) Math.round(internal * DISPLAY_UNIT);
    }

    /** Idem, en texte sans décimales inutiles (coûts par seconde, par PV…). */
    public static String shownText(double internal) {
        double v = internal * DISPLAY_UNIT;
        String s = Math.abs(v) >= 100 ? String.format(java.util.Locale.ROOT, "%.0f", v) : String.format(java.util.Locale.ROOT, "%.1f", v);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }

    /** Régénération d'aura : nulle avant ce niveau de la maîtrise « Régénération d'aura ». */
    public static final int REGEN_FROM = 5;

    /**
     * Part de la régénération de base obtenue à ce niveau de régénération : rien jusqu'au niveau 4, puis ça monte
     * jusqu'à ×1 au niveau 20, ×3 au niveau 100.
     */
    public static double regenFactor(int level) {
        if (level < REGEN_FROM) return 0;
        if (level < 20) return (level - REGEN_FROM + 1) / 16.0;
        return 1.0 + (level - 20) / 40.0;
    }

    /** Fin de la phase brutale : jusqu'à ce niveau, le surcoût s'effondre. */
    public static final int BRUTAL_UNTIL = 5;

    /**
     * Multiplicateur de coût pour un niveau de maîtrise : très élevé à 0, il s'effondre brutalement jusqu'au niveau 5
     * ({@code beginnerCostAtLevel5}), puis redescend doucement jusqu'à ×1 au niveau {@code beginnerCostUntil}.
     */
    public static double factor(int level) {
        int until = until();
        if (level >= until) return 1.0;
        double start = start();
        int l = Math.max(0, level);
        if (until <= BRUTAL_UNTIL) return 1.0 + (start - 1.0) * (1.0 - l / (double) until);
        double mid = Math.min(start, Math.max(1.0, atLevel5()));
        if (l < BRUTAL_UNTIL) return start + (mid - start) * (l / (double) BRUTAL_UNTIL);
        return 1.0 + (mid - 1.0) * (1.0 - (l - BRUTAL_UNTIL) / (double) (until - BRUTAL_UNTIL));
    }

    /** Aura par seconde que coûte le simple maintien du Ten ou de l'In à ce niveau de maîtrise (0 dès le niveau 5). */
    public static double novicePostureDrain(int level) {
        if (level >= BRUTAL_UNTIL) return 0;
        double base;
        try {
            base = HxHConfig.NOVICE_POSTURE_DRAIN.get();
        } catch (IllegalStateException e) {
            base = 4.0;
        }
        return base * (1.0 - Math.max(0, level) / (double) BRUTAL_UNTIL);
    }

    public static double atLevel5() {
        try {
            return HxHConfig.BEGINNER_COST_AT_LEVEL_5.get();
        } catch (IllegalStateException e) {
            return 2.0;
        }
    }

    public static double factor(HunterData data, Mastery mastery) {
        return factor(data.masteryLevel(mastery));
    }

    /** Multiplicateur des Hatsu : selon le niveau de Nen. */
    public static double hatsu(HunterData data) {
        return factor(data.nenLevel());
    }

    /** Coût réel d'une technique pour ce lanceur. */
    public static int cost(HunterData data, Technique technique) {
        return (int) Math.ceil(technique.auraCost() * hatsu(data));
    }

    private static double start() {
        try {
            return HxHConfig.BEGINNER_COST_MULTIPLIER.get();
        } catch (IllegalStateException e) {
            return 5.0; // configuration pas encore chargée (côté client, écrans)
        }
    }

    private static int until() {
        try {
            return HxHConfig.BEGINNER_COST_UNTIL.get();
        } catch (IllegalStateException e) {
            return 20;
        }
    }
}
