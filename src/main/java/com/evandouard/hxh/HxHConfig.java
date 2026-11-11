package com.evandouard.hxh;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration serveur (fichier serverconfig/hxh-server.toml de chaque monde).
 */
public final class HxHConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue XP_MULTIPLIER = BUILDER
            .comment("Multiplicateur de l'XP gagnée par toutes les stats")
            .defineInRange("stats.xpMultiplier", 1.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue STRENGTH_DAMAGE_PER_LEVEL = BUILDER
            .comment("Dégâts de corps à corps ajoutés par niveau de Force (1.0 = un demi-cœur)")
            .defineInRange("stats.strengthDamagePerLevel", 0.1, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue SPEED_PERCENT_PER_LEVEL = BUILDER
            .comment("Vitesse de déplacement ajoutée par niveau de Vitesse (0.015 = +1,5 %, soit +150 % au niveau 100)")
            .defineInRange("stats.speedPercentPerLevel", 0.015, 0.0, 0.1);

    public static final ModConfigSpec.DoubleValue STEP_UP_FROM_SPEED_BONUS = BUILDER
            .comment("Bonus de vitesse à partir duquel on monte les blocs d'un coup, comme un cheval (0.35 = +35 %)")
            .defineInRange("stats.stepUpFromSpeedBonus", 0.35, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue ENDURANCE_FLAT_PER_LEVEL = BUILDER
            .comment("Résistance plate aux attaques par niveau d'Endurance, en PV (0.05 = 5 PV au niveau 100)")
            .defineInRange("stats.enduranceFlatPerLevel", 0.05, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue VITALITY_HEALTH_PER_LEVEL = BUILDER
            .comment("Points de vie max ajoutés par niveau de Vitalité (2.0 = un cœur)")
            .defineInRange("stats.vitalityHealthPerLevel", 0.2, 0.0, 10.0);

    public static final ModConfigSpec.IntValue AURA_BASE = BUILDER
            .comment("Aura max de départ d'un utilisateur de Nen")
            .defineInRange("aura.base", 100, 1, 100000);

    public static final ModConfigSpec.IntValue AURA_PER_WILLPOWER = BUILDER
            .comment("Aura max ajoutée par niveau de Volonté")
            .defineInRange("aura.perWillpower", 4, 0, 1000);

    public static final ModConfigSpec.DoubleValue AURA_REGEN_PER_SECOND = BUILDER
            .comment("Aura régénérée par seconde (hors Ren)")
            .defineInRange("aura.regenPerSecond", 2.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue ZETSU_REGEN_MULTIPLIER = BUILDER
            .comment("Multiplicateur de régénération d'aura en Zetsu")
            .defineInRange("aura.zetsuRegenMultiplier", 3.0, 1.0, 100.0);

    public static final ModConfigSpec.DoubleValue REN_DRAIN_PER_SECOND = BUILDER
            .comment("Aura consommée par seconde en Ren")
            .defineInRange("aura.renDrainPerSecond", 3.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue REN_DAMAGE_BONUS = BUILDER
            .comment("Bonus de dégâts infligés en Ren (0.3 = +30 %)")
            .defineInRange("aura.renDamageBonus", 0.3, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue TEN_RESISTANCE_BASE = BUILDER
            .comment("Résistance de Nen en Ten au niveau de maîtrise 0, en PV retirés à chaque attaque")
            .defineInRange("defense.tenBase", 1.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue TEN_RESISTANCE_PER_LEVEL = BUILDER
            .comment("Résistance de Nen en Ten ajoutée par niveau de maîtrise du Ten")
            .defineInRange("defense.tenPerLevel", 0.03, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue KEN_RESISTANCE_BASE = BUILDER
            .comment("Résistance de Nen en Ken au niveau de maîtrise 0")
            .defineInRange("defense.kenBase", 3.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue KEN_RESISTANCE_PER_LEVEL = BUILDER
            .comment("Résistance de Nen en Ken ajoutée par niveau de maîtrise du Ken")
            .defineInRange("defense.kenPerLevel", 0.07, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue KEN_DRAIN_PER_SECOND = BUILDER
            .comment("Aura consommée par seconde en Ken au niveau de maîtrise 0")
            .defineInRange("defense.kenDrainPerSecond", 4.0, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue KO_AURA_COST = BUILDER
            .comment("Aura consommée d'un coup par le Ko au niveau de maîtrise 0")
            .defineInRange("defense.koCost", 30, 0, 100000);

    public static final ModConfigSpec.DoubleValue KO_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplicateur de la prochaine attaque au corps à corps sous Ko, au niveau de maîtrise 0")
            .defineInRange("defense.koMultiplier", 2.0, 1.0, 100.0);

    public static final ModConfigSpec.IntValue EXHAUSTION_TICKS = BUILDER
            .comment("Durée de l'épuisement (lenteur + faiblesse) quand l'aura tombe à zéro en Ren, en ticks")
            .defineInRange("aura.exhaustionTicks", 200, 0, 12000);

    public static final ModConfigSpec.DoubleValue HEAL_BASE_PER_SECOND = BUILDER
            .comment("Soin par le Nen : PV rendus par seconde au niveau 0")
            .defineInRange("heal.basePerSecond", 1.0, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue HEAL_PER_LEVEL = BUILDER
            .comment("Soin par le Nen : PV/s ajoutés par niveau de maîtrise")
            .defineInRange("heal.perLevel", 0.09, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue HEAL_BASE_COST = BUILDER
            .comment("Soin par le Nen : aura consommée par PV rendu au niveau 0")
            .defineInRange("heal.baseCost", 5.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue HEAL_COST_REDUCTION_PER_LEVEL = BUILDER
            .comment("Soin par le Nen : réduction du coût par PV par niveau de maîtrise")
            .defineInRange("heal.costReductionPerLevel", 0.04, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue HEAL_MIN_COST = BUILDER
            .comment("Soin par le Nen : coût minimal par PV, quel que soit le niveau")
            .defineInRange("heal.minCost", 1.0, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue MOB_HEAL_MIN_LEVEL = BUILDER
            .comment("Niveau Hunter (moyenne des stats) à partir duquel un mob à Nen sait se soigner avec le Nen")
            .defineInRange("heal.mobMinLevel", 20, 0, 100);

    public static final ModConfigSpec.IntValue MEDITATION_SECONDS_TO_AWAKEN = BUILDER
            .comment("Éveil par la méditation : secondes de méditation cumulées nécessaires")
            .defineInRange("awakening.meditationSeconds", 1200, 1, 1_000_000);

    public static final ModConfigSpec.DoubleValue ACCIDENTAL_AWAKENING_CHANCE = BUILDER
            .comment("Éveil par accident : chance qu'un coup d'un utilisateur de Nen ouvre les pores de la victime")
            .defineInRange("awakening.accidentalChance", 0.03, 0.0, 1.0);

    public static final ModConfigSpec.IntValue OPEN_PORES_AURA_COST = BUILDER
            .comment("Ouverture des pores par un maître : aura consommée par le maître")
            .defineInRange("awakening.openPoresCost", 30, 0, 100000);

    public static final ModConfigSpec.IntValue OPEN_PORES_COOLDOWN_TICKS = BUILDER
            .comment("Ouverture des pores par un maître : temps de recharge en ticks")
            .defineInRange("awakening.openPoresCooldown", 200, 0, 72000);

    public static final ModConfigSpec.DoubleValue OPEN_PORES_RANGE = BUILDER
            .comment("Ouverture des pores par un maître : portée en blocs")
            .defineInRange("awakening.openPoresRange", 4.0, 1.0, 32.0);

    public static final ModConfigSpec.IntValue ZETSU_UNLOCK_IN_LEVEL = BUILDER
            .comment("Niveau de maîtrise de l'In à atteindre pour débloquer le Zetsu complet")
            .defineInRange("senses.zetsuUnlockInLevel", 10, 0, 100);

    public static final ModConfigSpec.DoubleValue GYO_DRAIN_PER_SECOND = BUILDER
            .comment("Aura consommée par seconde quand le Gyo est actif")
            .defineInRange("senses.gyoDrainPerSecond", 1.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue EN_DRAIN_PER_SECOND = BUILDER
            .comment("Aura consommée par seconde quand l'En est actif")
            .defineInRange("senses.enDrainPerSecond", 2.0, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue ARENA_WIN_STEP = BUILDER
            .comment("Tour Céleste : étages gagnés par victoire sous le 200e")
            .defineInRange("arena.winStep", 5, 1, 100);

    public static final ModConfigSpec.IntValue ARENA_FAST_WIN_BONUS = BUILDER
            .comment("Tour Céleste : étages en plus pour une victoire rapide sous le 200e")
            .defineInRange("arena.fastWinBonus", 3, 0, 100);

    public static final ModConfigSpec.IntValue ARENA_WIN_STEP_ABOVE_200 = BUILDER
            .comment("Tour Céleste : étages gagnés par victoire à partir du 240e (avant : +10 jusqu'au 220e, puis +5)")
            .defineInRange("arena.winStepAbove200", 1, 1, 50);

    public static final ModConfigSpec.IntValue ARENA_LOSS_DROP_FIRST_BRACKET = BUILDER
            .comment("Tour Céleste : étages perdus par défaite dans la première tranche (ailleurs, on redescend dans la tranche d'en dessous)")
            .defineInRange("arena.lossDropFirstBracket", 5, 0, 100);

    public static final ModConfigSpec.IntValue COMBAT_STRONG_FROM = BUILDER
            .comment("Combat : niveau de Force à partir duquel on est « fort » (coup de pied et frappe chargée projettent)")
            .defineInRange("combat.strongFromStrength", 30, 0, 100);

    public static final ModConfigSpec.DoubleValue EXAM_MARATHON_SPEED = BUILDER
            .comment("Examen Hunter : vitesse de Satotz au marathon (joueur qui marche = 0.10, qui sprinte = 0.13,"
                    + " qui sprinte en sautant ≈ 0.165)")
            .defineInRange("exam.satotzSpeed", 0.165, 0.05, 0.4);

    public static final ModConfigSpec.IntValue EXAM_LOST_SECONDS = BUILDER
            .comment("Examen Hunter : secondes passées trop loin de Satotz avant d'échouer")
            .defineInRange("exam.lostSeconds", 5, 1, 120);

    public static final ModConfigSpec.IntValue EXAM_RETRY_TICKS = BUILDER
            .comment("Examen Hunter : attente avant de retenter après un échec (24000 = un jour Minecraft)")
            .defineInRange("exam.retryTicks", 24000, 0, 2_400_000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private HxHConfig() {}
}
