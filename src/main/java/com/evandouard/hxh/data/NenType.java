package com.evandouard.hxh.data;

import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * Les six catégories de Nen, dans l'ordre de l'hexagone.
 */
public enum NenType {
    ENHANCER, TRANSMUTER, CONJURER, SPECIALIST, MANIPULATOR, EMITTER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "nen.hxh." + id();
    }

    public static NenType byId(String id) {
        for (NenType t : values()) if (t.id().equals(id)) return t;
        return null;
    }

    /**
     * Efficacité d'une technique de catégorie {@code other} pour un utilisateur de ce type,
     * selon la distance sur l'hexagone : 100 %, 80 % (voisins), 60 %, 40 % (opposé).
     */
    public float affinity(NenType other) {
        int n = values().length;
        int d = Math.abs(ordinal() - other.ordinal());
        int distance = Math.min(d, n - d);
        return 1.0f - 0.2f * distance;
    }

    /** Tirage aléatoire : les Spécialistes sont rares (5 %), les autres types se partagent le reste. */
    public static NenType random(RandomSource random) {
        if (random.nextFloat() < 0.05f) return SPECIALIST;
        NenType[] common = {ENHANCER, TRANSMUTER, CONJURER, MANIPULATOR, EMITTER};
        return common[random.nextInt(common.length)];
    }
}
