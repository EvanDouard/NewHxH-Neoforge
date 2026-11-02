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
            .comment("Vitesse de déplacement ajoutée par niveau de Vitesse (0.004 = +0,4 %)")
            .defineInRange("stats.speedPercentPerLevel", 0.004, 0.0, 0.1);

    public static final ModConfigSpec.DoubleValue ENDURANCE_REDUCTION_PER_LEVEL = BUILDER
            .comment("Réduction des dégâts subis par niveau d'Endurance (0.003 = 0,3 %)")
            .defineInRange("stats.enduranceReductionPerLevel", 0.003, 0.0, 0.01);

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

    public static final ModConfigSpec.DoubleValue TEN_DAMAGE_REDUCTION = BUILDER
            .comment("Réduction des dégâts subis en Ten (0.15 = -15 %)")
            .defineInRange("aura.tenDamageReduction", 0.15, 0.0, 0.9);

    public static final ModConfigSpec.DoubleValue ZETSU_DAMAGE_TAKEN_BONUS = BUILDER
            .comment("Dégâts subis en plus en Zetsu (0.5 = +50 %)")
            .defineInRange("aura.zetsuDamageTakenBonus", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.IntValue EXHAUSTION_TICKS = BUILDER
            .comment("Durée de l'épuisement (lenteur + faiblesse) quand l'aura tombe à zéro en Ren, en ticks")
            .defineInRange("aura.exhaustionTicks", 200, 0, 12000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private HxHConfig() {}
}
