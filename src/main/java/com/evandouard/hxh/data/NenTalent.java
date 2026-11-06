package com.evandouard.hxh.data;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Talent pour le Nen, tiré au hasard en début de partie et définitif : multiplie tout gain d'XP de Nen
 * (niveau de Nen et maîtrises).
 */
public enum NenTalent {
    ORDINARY("ordinary", 0.8, 400),
    PROMISING("promising", 1.0, 300),
    GIFTED("gifted", 1.25, 180),
    EXCEPTIONAL("exceptional", 1.5, 80),
    PRODIGY("prodigy", 2.0, 30),
    GENIUS("genius", 3.0, 9),
    MONSTER("monster", 4.0, 1);

    private final String id;
    private final double multiplier;
    /** Chance sur 1000. */
    private final int weight;

    NenTalent(String id, double multiplier, int weight) {
        this.id = id;
        this.multiplier = multiplier;
        this.weight = weight;
    }

    public String id() {
        return id;
    }

    public double multiplier() {
        return multiplier;
    }

    public int weightPerMille() {
        return weight;
    }

    public String translationKey() {
        return "talent.hxh." + id;
    }

    public static NenTalent roll(RandomSource random) {
        int r = random.nextInt(1000);
        for (NenTalent t : values()) {
            if (r < t.weight) return t;
            r -= t.weight;
        }
        return ORDINARY;
    }

    @Nullable
    public static NenTalent byId(String id) {
        for (NenTalent t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
