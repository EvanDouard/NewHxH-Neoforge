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

    /** Si vrai, chaque capacité du Nen doit être apprise auprès d'un maître (quêtes de Wing) avant d'être utilisée. */
    public static final GameRules.Key<GameRules.BooleanValue> NEN_LEARNING =
            GameRules.register("hxhNenLearning", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

    /**
     * Pourcentage appliqué aux chances des mobs naturels au Nen (champ « natural » des mobs d'exemple) :
     * 100 = chances du fichier, 0 = aucun.
     */
    public static final GameRules.Key<GameRules.IntegerValue> NEN_MOB_CHANCE =
            GameRules.register("hxhNenMobChance", GameRules.Category.SPAWNING, GameRules.IntegerValue.create(100));

    /** Si vrai, des événements aléatoires (primes de l'Association…) sont annoncés dans le chat. */
    public static final GameRules.Key<GameRules.BooleanValue> RANDOM_EVENTS =
            GameRules.register("hxhRandomEvents", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));

    /** Si vrai, chaque nouveau joueur naît dans une famille tirée au hasard (Zoldyck, Kurta…) ; faux : aucune famille. */
    public static final GameRules.Key<GameRules.BooleanValue> RANDOM_FAMILY =
            GameRules.register("randomFamily", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

    /** Tests : si vrai, aucune technique ni coup n'a de recharge. */
    public static final GameRules.Key<GameRules.BooleanValue> NO_COOLDOWNS =
            GameRules.register("hxhNoCooldowns", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    /** Tests : si vrai, les techniques et modes du Nen ne coûtent pas d'aura. */
    public static final GameRules.Key<GameRules.BooleanValue> NO_AURA_COST =
            GameRules.register("hxhNoAuraCost", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    private HxHGameRules() {}

    /** Les coups du Nen peuvent casser des blocs ici : règle active, et ni la Tour Céleste ni l'Examen. */
    public static boolean canDestroy(net.minecraft.server.level.ServerLevel level) {
        return level.getGameRules().getBoolean(NEN_DESTRUCTION)
                && !level.dimension().equals(com.evandouard.hxh.arena.ArenaWorld.DIMENSION)
                && !com.evandouard.hxh.exam.ExamWorld.isExamLevel(level);
    }

    /** Copie des deux règles de test, mise à jour à chaque tick du serveur (lue partout sans accès au monde). */
    public static volatile boolean noCooldownsActive, noAuraCostActive;

    public static void refresh(net.minecraft.server.MinecraftServer server) {
        GameRules rules = server.getGameRules();
        noCooldownsActive = rules.getBoolean(NO_COOLDOWNS);
        noAuraCostActive = rules.getBoolean(NO_AURA_COST);
    }

    public static boolean noCooldowns(net.minecraft.world.level.Level level) {
        return level.getServer() != null && level.getServer().getGameRules().getBoolean(NO_COOLDOWNS);
    }

    public static boolean noAuraCost(net.minecraft.world.level.Level level) {
        return level.getServer() != null && level.getServer().getGameRules().getBoolean(NO_AURA_COST);
    }

    /** Force le chargement de la classe (et donc l'enregistrement des règles) au démarrage du mod. */
    public static void init() {}
}
