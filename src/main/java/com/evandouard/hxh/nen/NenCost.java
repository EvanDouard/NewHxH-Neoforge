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
            base = 5.0;
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
