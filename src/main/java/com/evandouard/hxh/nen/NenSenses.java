package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.effect.ModEffects;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Gyo, En et Shu.
 * <ul>
 *     <li>Gyo (interrupteur), selon sa maîtrise : niveaux (0), pièges de Nen (5), invisibles de Minecraft (10),
 *     Ryu adverse (20), In (25, net à 50), type de Nen (35), invisibilité par le Nen et trajectoires (50),
 *     utilisateurs en Zetsu (maîtrise maximale).</li>
 *     <li>En (interrupteur) : sphère d'aura ; tout ce qui est dedans brille. Portée selon la maîtrise de l'En.</li>
 *     <li>Shu (automatique) : avec l'aura active, l'outil tenu frappe plus fort, mine plus vite et s'use moins.</li>
 * </ul>
 */
public final class NenSenses {
    /** Marques et pièges de Nen : portes de Knov, fil du Bungee Gum. */
    public static final int GYO_SEE_TRAPS = 5;
    /** Invisibilité de Minecraft (potion). */
    public static final int GYO_SEE_INVISIBLE = 10;
    /** Le poing qui porte le Ryu de l'adversaire brille. */
    public static final int GYO_SEE_RYU = 20;
    /** L'In commence à se voir (pâle), et se voit nettement à {@link #GYO_SEE_IN_CLEAR}. */
    public static final int GYO_SEE_IN = 25;
    public static final int GYO_SEE_IN_CLEAR = 50;
    /** Type de Nen affiché à côté du niveau. */
    public static final int GYO_SEE_TYPE = 35;
    /** Invisibilité par le Nen (Texture Surprise…) et trajectoire des tirs qui arrivent. */
    public static final int GYO_SEE_NEN_INVISIBLE = 50;
    public static final int GYO_SEE_TRAJECTORY = 50;
    /** Utilisateurs en Zetsu : maîtrise maximale seulement. */
    public static final int GYO_SEE_ZETSU = com.evandouard.hxh.data.Mastery.MAX_LEVEL;

    /** Maîtrise du Gyo actif de l'entité, ou -1 si le Gyo est coupé. */
    public static int gyoLevel(LivingEntity entity) {
        if (!entity.hasData(com.evandouard.hxh.data.ModAttachments.HUNTER)) return -1;
        var d = entity.getData(com.evandouard.hxh.data.ModAttachments.HUNTER);
        return d.gyo() ? d.masteryLevel(com.evandouard.hxh.data.Mastery.GYO) : -1;
    }

    /** Invisibilité par le Nen : invisible pour tous, sauf un Gyo de maîtrise {@link #GYO_SEE_NEN_INVISIBLE}. */
    public static void nenHide(LivingEntity entity, int ticks) {
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,
                ticks, 0, false, false, false));
        if (entity.hasData(com.evandouard.hxh.data.ModAttachments.HUNTER)) {
            entity.getData(com.evandouard.hxh.data.ModAttachments.HUNTER).setNenHiddenUntil(entity.level().getGameTime() + ticks);
        }
    }

    public static boolean nenHidden(LivingEntity entity) {
        return entity.isInvisible() && entity.hasData(com.evandouard.hxh.data.ModAttachments.HUNTER)
                && entity.getData(com.evandouard.hxh.data.ModAttachments.HUNTER).nenHiddenUntil() > entity.level().getGameTime();
    }

    /** Ce qu'un Gyo de cette maîtrise perce : invisibles (selon leur sorte) et Zetsu. */
    public static boolean gyoReveals(int gyo, LivingEntity target, boolean zetsu) {
        if (gyo < 0) return false;
        if (target.isInvisible()) return gyo >= (nenHidden(target) ? GYO_SEE_NEN_INVISIBLE : GYO_SEE_INVISIBLE);
        return zetsu && gyo >= GYO_SEE_ZETSU;
    }

    private NenSenses() {}

    /** Rayon de l'En : 2 blocs + 0,25 par niveau de maîtrise (elle-même plafonnée par le niveau de Nen). */
    public static double enRadius(HunterData data) {
        return 2 + data.masteryLevel(Mastery.EN) * 0.25;
    }

    private static String senseRefusal(HunterData data) {
        if (!data.nenUnlocked()) return "message.hxh.nen_locked";
        if (data.mode() == NenMode.ZETSU || data.mode() == NenMode.IN) return "message.hxh.sense_concealed";
        if (data.aura() < 1) return "message.hxh.no_aura";
        return null;
    }

    public static String setGyo(LivingEntity entity, boolean on) {
        HunterData data = HunterStats.get(entity);
        if (on && NenLearning.refusal(entity, NenLearning.Ability.GYO) != null) return NenLearning.refusal(entity, NenLearning.Ability.GYO);
        if (on) {
            String refusal = senseRefusal(data);
            if (refusal != null) return refusal;
        }
        data.setGyo(on);
        return null;
    }

    public static String setEn(LivingEntity entity, boolean on) {
        HunterData data = HunterStats.get(entity);
        if (on && NenLearning.refusal(entity, NenLearning.Ability.EN) != null) return NenLearning.refusal(entity, NenLearning.Ability.EN);
        if (on) {
            String refusal = senseRefusal(data);
            if (refusal != null) return refusal;
        }
        data.setEn(on);
        return null;
    }

    public static void toggleGyo(LivingEntity entity) {
        String refusal = setGyo(entity, !HunterStats.get(entity).gyo());
        if (refusal != null && entity instanceof Player p) p.displayClientMessage(Component.translatable(refusal), true);
    }

    public static void toggleEn(LivingEntity entity) {
        String refusal = setEn(entity, !HunterStats.get(entity).en());
        if (refusal != null && entity instanceof Player p) p.displayClientMessage(Component.translatable(refusal), true);
    }

    /** Chaque tick serveur : coût, entraînement, coupure si plus d'aura, effets témoins. */
    public static void tick(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = HunterStats.get(entity);
        // Courir disperse l'En (il se rétracte d'un coup) ; à l'arrêt il est parfait, en marchant il se réduit
        if (data.en() && entity instanceof Player p && p.isSprinting()) {
            data.setEn(false);
            p.displayClientMessage(Component.translatable("message.hxh.en_run"), true);
        }
        if (data.gyo() || data.en()) {
            if (!data.nenUnlocked() || data.mode() == NenMode.ZETSU || data.mode() == NenMode.IN) {
                data.setGyo(false);
                data.setEn(false);
            } else {
                float drain = 0;
                if (data.gyo()) drain += (float) Math.max(0.2, HxHConfig.GYO_DRAIN_PER_SECOND.get() - data.masteryLevel(Mastery.GYO) * 0.008);
                if (data.en()) drain += (float) Math.max(0.5, HxHConfig.EN_DRAIN_PER_SECOND.get() - data.masteryLevel(Mastery.EN) * 0.01);
                data.setAura(data.aura() - drain / 20f);
                if (data.aura() <= 0) {
                    data.setGyo(false);
                    data.setEn(false);
                } else if (entity.tickCount % 20 == 0) {
                    if (data.gyo()) NenProgress.train(entity, Mastery.GYO, 1);
                    if (data.en()) NenProgress.train(entity, Mastery.EN, 1);
                }
            }
        }
        syncEffect(entity, ModEffects.GYO, data.gyo());
        syncEffect(entity, ModEffects.EN, data.en());
    }

    private static void syncEffect(LivingEntity entity, Holder<MobEffect> effect, boolean on) {
        boolean has = entity.hasEffect(effect);
        if (on && !has) {
            entity.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        } else if (!on && has) {
            entity.removeEffect(effect);
        }
    }

    // ---------- Shu ----------

    /** Le Shu prolonge le Ten dans l'outil : il faut une aura active (Ten, Ren ou Ken). */
    public static boolean shuActive(LivingEntity entity, ItemStack held) {
        if (held.isEmpty() || !held.isDamageableItem() || !entity.hasData(ModAttachments.HUNTER)) return false;
        HunterData data = HunterStats.get(entity);
        if (!NenLearning.knows(entity, NenLearning.Ability.SHU)) return false;
        return data.nenUnlocked() && (data.mode() == NenMode.TEN || data.mode() == NenMode.REN || data.mode() == NenMode.KEN);
    }

    /** Bonus de dégâts d'un coup porté avec un outil sous Shu (+10 % à +50 %). */
    public static float shuDamageFactor(LivingEntity entity) {
        if (!shuActive(entity, entity.getMainHandItem())) return 1f;
        return (float) (1.1 + HunterStats.get(entity).masteryLevel(Mastery.SHU) * 0.004);
    }

    /** Bonus de vitesse de minage sous Shu (+10 % à +50 %). */
    public static float shuMiningFactor(LivingEntity entity) {
        if (!shuActive(entity, entity.getMainHandItem())) return 1f;
        return (float) (1.1 + HunterStats.get(entity).masteryLevel(Mastery.SHU) * 0.004);
    }

    /** Chance (20 % à 80 %) que l'outil ne s'use pas sur cette action : on lui rend le point d'usure. */
    public static void shuProtectTool(LivingEntity entity) {
        ItemStack held = entity.getMainHandItem();
        if (!shuActive(entity, held)) return;
        HunterData data = HunterStats.get(entity);
        double chance = 0.2 + data.masteryLevel(Mastery.SHU) * 0.006;
        if (held.getDamageValue() > 0 && entity.getRandom().nextDouble() < chance) {
            held.setDamageValue(held.getDamageValue() - 1);
        }
        data.setAura(data.aura() - 0.5f);
        NenProgress.train(entity, Mastery.SHU, 1);
    }
}
