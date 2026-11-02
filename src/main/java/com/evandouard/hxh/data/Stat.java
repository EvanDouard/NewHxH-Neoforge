package com.evandouard.hxh.data;

import java.util.Locale;

/**
 * Stats physiques entraînées par l'usage. Indépendantes de tout personnage incarné.
 */
public enum Stat {
    /** Dégâts au corps à corps. Monte en infligeant des dégâts. */
    STRENGTH,
    /** Vitesse de déplacement. Monte en sprintant. */
    SPEED,
    /** Réduction des dégâts subis. Monte en encaissant des dégâts. */
    ENDURANCE,
    /** Points de vie max. Monte en se soignant. */
    VITALITY,
    /** Volonté : servira à l'aura (J2). */
    WILLPOWER;

    public static final int MAX_LEVEL = 100;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "stat.hxh." + id();
    }

    public static Stat byId(String id) {
        for (Stat s : values()) if (s.id().equals(id)) return s;
        return null;
    }

    /** XP nécessaire pour passer du niveau {@code level} au suivant. */
    public static int xpToNext(int level) {
        return (int) Math.round(10 + 4 * Math.pow(level, 1.6));
    }
}
