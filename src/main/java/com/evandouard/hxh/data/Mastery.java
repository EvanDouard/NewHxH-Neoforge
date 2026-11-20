package com.evandouard.hxh.data;

import java.util.Locale;

/**
 * Maîtrise de chaque technique de base du Nen. Chacune a son niveau et son XP, gagnés en la pratiquant,
 * et ne peut pas dépasser le niveau de Nen (plafond de verre).
 */
public enum Mastery {
    TEN, ZETSU, IN, REN, KEN, KO, RYU, GYO, EN, SHU,
    /** Régénération d'aura : nulle avant le niveau 5 ; monte en dormant (sommeil = aura pleine) et en méditant. */
    REGEN;

    public static final int MAX_LEVEL = 100;
    /** Plafond minimal : même un débutant peut monter ses maîtrises jusqu'à ce niveau. */
    public static final int MIN_CAP = 5;

    /**
     * Maîtrise où sont rangés le niveau et l'XP : l'In et le Zetsu partagent la même (la dissimulation),
     * le Zetsu n'étant que le palier supérieur de l'In.
     */
    public Mastery storage() {
        return this == ZETSU ? IN : this;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "mastery.hxh." + id();
    }

    public static Mastery byId(String id) {
        for (Mastery m : values()) if (m.id().equals(id)) return m;
        return null;
    }

    /** XP pour passer au niveau suivant d'une maîtrise. */
    public static int xpToNext(int level) {
        return 2 * Stat.xpToNext(level);
    }

    /** XP de Nen pour passer au niveau de Nen suivant. */
    public static int nenXpToNext(int level) {
        return 3 * Stat.xpToNext(level) + 10;
    }
}
