package com.evandouard.hxh.training;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

/**
 * Gamme d'un objet d'entraînement : I fabricable (bas de gamme), II amélioré à partir du I,
 * III vendu par les marchands, IV rare (enchères).
 */
public enum TrainingTier {
    I(1, 1.0, Rarity.COMMON, ChatFormatting.GRAY),
    II(2, 1.6, Rarity.COMMON, ChatFormatting.WHITE),
    III(3, 2.5, Rarity.UNCOMMON, ChatFormatting.YELLOW),
    IV(4, 4.0, Rarity.RARE, ChatFormatting.AQUA);

    private final int number;
    private final double multiplier;
    private final Rarity rarity;
    private final ChatFormatting color;

    TrainingTier(int number, double multiplier, Rarity rarity, ChatFormatting color) {
        this.number = number;
        this.multiplier = multiplier;
        this.rarity = rarity;
        this.color = color;
    }

    public int number() {
        return number;
    }

    /** Multiplicateur d'XP par rapport au niveau I. */
    public double multiplier() {
        return multiplier;
    }

    public Rarity rarity() {
        return rarity;
    }

    public ChatFormatting color() {
        return color;
    }

    /** Suffixe des identifiants : dumbbell_1 … dumbbell_4. */
    public String suffix() {
        return "_" + number;
    }

    /** Clé de la ligne « où l'obtenir » de l'infobulle. */
    public String sourceKey() {
        return "tooltip.hxh.training.source_" + number;
    }
}
