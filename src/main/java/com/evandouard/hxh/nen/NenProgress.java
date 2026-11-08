package com.evandouard.hxh.nen;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Progression du Nen : toute utilisation du Nen rapporte de l'XP de Nen ; pratiquer une technique
 * de base rapporte en plus de l'XP à sa maîtrise (plafonnée par le niveau de Nen). Les deux sont multipliées
 * par le talent pour le Nen du joueur.
 */
public final class NenProgress {
    private NenProgress() {}

    /**
     * Applique le talent pour le Nen à un gain d'XP. L'arrondi est aléatoire pour que les petits gains
     * (1 XP) restent justes en moyenne : ×1,25 donne 1 ou 2, en moyenne 1,25.
     */
    public static int withTalent(LivingEntity entity, int amount) {
        double scaled = amount * HunterStats.get(entity).talentMultiplier();
        int whole = (int) Math.floor(scaled);
        return whole + (entity.getRandom().nextDouble() < scaled - whole ? 1 : 0);
    }

    public static void addNenXp(LivingEntity entity, int amount) {
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked() || amount <= 0) return;
        amount = withTalent(entity, amount);
        if (amount <= 0) return;
        if (data.addNenXp(amount) > 0 && entity instanceof Player player) {
            com.evandouard.hxh.network.Hud.toast(player, Component.translatable("hud.hxh.toast.nen"),
                    Component.translatable("hud.hxh.toast.level", data.nenLevel()), "+1", com.evandouard.hxh.network.Hud.BLUE);
        }
    }

    /** Pratique d'une technique de base : XP de maîtrise + XP de Nen. */
    public static void train(LivingEntity entity, Mastery mastery, int amount) {
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked() || amount <= 0) return;
        int masteryXp = withTalent(entity, amount);
        if (masteryXp > 0 && data.addMasteryXp(mastery, masteryXp) > 0 && entity instanceof Player player) {
            com.evandouard.hxh.network.Hud.toast(player, Component.translatable(mastery.translationKey()),
                    Component.translatable("hud.hxh.toast.level", data.masteryLevel(mastery)), "+1", com.evandouard.hxh.network.Hud.PURPLE);
        }
        addNenXp(entity, amount);
    }
}
