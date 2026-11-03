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
    private static final ResourceLocation VITALITY_ID = HxHMod.id("stat_vitality");

    private HunterStats() {}

    public static HunterData get(LivingEntity entity) {
        return entity.getData(ModAttachments.HUNTER);
    }

    /** Ajoute de l'XP brute (avant multiplicateur de config) et gère les montées de niveau. */
    public static void addXp(LivingEntity entity, Stat stat, double rawAmount) {
        int amount = (int) Math.round(rawAmount * HxHConfig.XP_MULTIPLIER.get());
        if (amount <= 0) return;
        HunterData data = get(entity);
        int gained = data.addXp(stat, amount);
        if (gained > 0) {
            applyAttributes(entity);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.hxh.stat_level_up",
                        Component.translatable(stat.translationKey()), data.level(stat)), true);
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
        setModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_ID,
                data.level(Stat.SPEED) * HxHConfig.SPEED_PERCENT_PER_LEVEL.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
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
