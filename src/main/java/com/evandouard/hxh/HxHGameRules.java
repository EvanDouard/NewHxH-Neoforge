package com.evandouard.hxh;

import net.minecraft.world.level.GameRules;

/**
 * Règles de jeu du mod (/gamerule).
 */
public final class HxHGameRules {
    /** Si vrai, chaque nouvel Hunter reçoit un type de Nen aléatoire (caché jusqu'à la divination). */
    public static final GameRules.Key<GameRules.BooleanValue> RANDOM_NEN_TYPE =
            GameRules.register("randomNenType", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

    /**
     * Si vrai, les techniques puissantes (Jajanken : Pierre…) détruisent les blocs sur leur passage et à l'impact.
     * Faux : mode passif, rien n'est cassé. Jamais dans la Tour Céleste.
     */
    public static final GameRules.Key<GameRules.BooleanValue> NEN_DESTRUCTION =
            GameRules.register("hxhNenDestruction", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));

    private HxHGameRules() {}

    /** Force le chargement de la classe (et donc l'enregistrement des règles) au démarrage du mod. */
    public static void init() {}
}
