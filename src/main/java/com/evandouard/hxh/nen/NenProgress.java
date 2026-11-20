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
            levelUpFx(player, com.evandouard.hxh.fx.NenFx.ENHANCER);
            player.sendSystemMessage(Component.translatable("message.hxh.nen_level_up", data.nenLevel(), data.masteryCap())
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        }
    }

    /** Passage de niveau bien visible : son et onde d'aura autour des pieds. */
    private static void levelUpFx(Player player, int color) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);
        com.evandouard.hxh.fx.NenFx.shockwaveAt(level, player.position().add(0, 0.1, 0), 2.5f, color);
    }

    /** Pratique d'une technique de base : XP de maîtrise + XP de Nen. */
    public static void train(LivingEntity entity, Mastery mastery, int amount) {
        if (trainOnly(entity, mastery, amount)) addNenXp(entity, amount);
    }

    /** XP de maîtrise seule (sans XP de Nen) : le sommeil et la méditation entraînent la régénération d'aura. */
    public static boolean trainOnly(LivingEntity entity, Mastery mastery, int amount) {
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked() || amount <= 0) return false;
        int masteryXp = withTalent(entity, amount);
        if (masteryXp > 0 && data.addMasteryXp(mastery, masteryXp) > 0 && entity instanceof Player player) {
            com.evandouard.hxh.network.Hud.toast(player, Component.translatable(mastery.translationKey()),
                    Component.translatable("hud.hxh.toast.level", data.masteryLevel(mastery)), "+1", com.evandouard.hxh.network.Hud.PURPLE);
            levelUpFx(player, com.evandouard.hxh.fx.NenFx.GOLD);
            boolean atCap = data.masteryLevel(mastery) >= data.masteryCap();
            player.sendSystemMessage(Component.translatable(atCap ? "message.hxh.mastery_level_up_cap" : "message.hxh.mastery_level_up",
                    Component.translatable(mastery.translationKey()), data.masteryLevel(mastery), data.masteryCap())
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }
}
