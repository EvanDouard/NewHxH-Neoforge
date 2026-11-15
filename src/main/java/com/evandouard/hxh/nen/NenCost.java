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

    /** Multiplicateur de coût pour un niveau de maîtrise. */
    public static double factor(int level) {
        int until = until();
        if (level >= until) return 1.0;
        double start = start();
        return 1.0 + (start - 1.0) * (1.0 - Math.max(0, level) / (double) until);
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
            return 2.5; // configuration pas encore chargée (côté client, écrans)
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
