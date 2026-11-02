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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private HxHConfig() {}
}
