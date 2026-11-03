package com.evandouard.hxh.nen;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Progression du Nen : toute utilisation du Nen rapporte de l'XP de Nen ; pratiquer une technique
 * de base rapporte en plus de l'XP à sa maîtrise (plafonnée par le niveau de Nen).
 */
public final class NenProgress {
    private NenProgress() {}

    public static void addNenXp(LivingEntity entity, int amount) {
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked() || amount <= 0) return;
        if (data.addNenXp(amount) > 0 && entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.hxh.nen_level_up", data.nenLevel()), true);
        }
    }

    /** Pratique d'une technique de base : XP de maîtrise + XP de Nen. */
    public static void train(LivingEntity entity, Mastery mastery, int amount) {
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked() || amount <= 0) return;
        if (data.addMasteryXp(mastery, amount) > 0 && entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.hxh.mastery_level_up",
                    Component.translatable(mastery.translationKey()), data.masteryLevel(mastery)), true);
        }
        addNenXp(entity, amount);
    }
}
