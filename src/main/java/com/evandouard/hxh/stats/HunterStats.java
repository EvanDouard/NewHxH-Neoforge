package com.evandouard.hxh.stats;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.Stat;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Logique serveur des stats : XP, niveaux et effets sur les attributs.
 * Fonctionne pour toute LivingEntity (joueur ou mob).
 */
public final class HunterStats {
    private static final ResourceLocation STRENGTH_ID = HxHMod.id("stat_strength");
    private static final ResourceLocation SPEED_ID = HxHMod.id("stat_speed");
    private static final ResourceLocation STEP_ID = HxHMod.id("stat_speed_step");
    private static final ResourceLocation VITALITY_ID = HxHMod.id("stat_vitality");
    private static final ResourceLocation SWIM_ID = HxHMod.id("stat_swim");
    private static final ResourceLocation WATER_ID = HxHMod.id("stat_water_walk");

    /** Bonus de vitesse (part de la vitesse de base) au niveau de Vitesse donné : linéaire + courbe. */
    public static double speedBonus(int level) {
        return level * HxHConfig.SPEED_PERCENT_PER_LEVEL.get() + level * (double) level * HxHConfig.SPEED_CURVE.get();
    }

    private HunterStats() {}

    public static HunterData get(LivingEntity entity) {
        return entity.getData(ModAttachments.HUNTER);
    }

    /** Ajoute de l'XP brute (avant multiplicateur de config) et gère les montées de niveau. */
    public static void addXp(LivingEntity entity, Stat stat, double rawAmount) {
        // Zushi (passif « Assidu ») : +25 % d'expérience à l'entraînement
        if ("zushi".equals(get(entity).kit())) rawAmount *= 1.25;
        // héritage de la famille (Freecss, Zoldyck…)
        rawAmount *= com.evandouard.hxh.family.Families.xpMultiplier(entity, stat);
        int amount = (int) Math.round(rawAmount * HxHConfig.XP_MULTIPLIER.get());
        if (amount <= 0) return;
        HunterData data = get(entity);
        // une part de l'entraînement nourrit le sang du clan
        com.evandouard.hxh.family.Families.addClanXp(entity, amount * com.evandouard.hxh.family.Families.CLAN_XP_FROM_STATS);
        int gained = data.addXp(stat, amount);
        if (gained > 0) {
            applyAttributes(entity);
            if (entity instanceof Player player) {
                com.evandouard.hxh.network.Hud.toast(player, Component.translatable(stat.translationKey()),
                        Component.translatable("hud.hxh.toast.level", data.level(stat)), "+" + gained,
                        com.evandouard.hxh.network.Hud.statColor(stat));
            }
        }
    }

    public static void setLevel(LivingEntity entity, Stat stat, int level) {
        get(entity).setLevel(stat, level);
        applyAttributes(entity);
    }

    /** Recalcule les modificateurs d'attributs à partir des niveaux de stats. */
    public static void applyAttributes(LivingEntity entity) {
        HunterData data = get(entity);
        setModifier(entity, Attributes.ATTACK_DAMAGE, STRENGTH_ID,
                data.level(Stat.STRENGTH) * HxHConfig.STRENGTH_DAMAGE_PER_LEVEL.get(),
                AttributeModifier.Operation.ADD_VALUE);
        double speedBonus = speedBonus(data.level(Stat.SPEED)) * data.speedCap();
        setModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_ID, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        // Très rapide : on monte les blocs d'un coup, comme un cheval
        setModifier(entity, Attributes.STEP_HEIGHT, STEP_ID,
                speedBonus >= HxHConfig.STEP_UP_FROM_SPEED_BONUS.get() ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);
        // Nage : vitesse et force font aussi avancer dans l'eau (sinon, rapide sur terre mais lent en nageant)
        double swim = (data.level(Stat.SPEED) * 0.6 + data.level(Stat.STRENGTH) * 0.4) * HxHConfig.SWIM_PER_LEVEL.get()
                * Math.max(0.2, data.speedCap());
        setModifier(entity, net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED, SWIM_ID, swim, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(entity, Attributes.WATER_MOVEMENT_EFFICIENCY, WATER_ID, Math.min(0.6, swim / 3), AttributeModifier.Operation.ADD_VALUE);
        setModifier(entity, Attributes.MAX_HEALTH, VITALITY_ID,
                data.level(Stat.VITALITY) * HxHConfig.VITALITY_HEALTH_PER_LEVEL.get(),
                AttributeModifier.Operation.ADD_VALUE);
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id,
                                    double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, amount, op));
    }
}
