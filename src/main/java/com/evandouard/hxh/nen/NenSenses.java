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
 *     <li>Gyo (interrupteur) : voit niveaux, auras cachées par l'In, invisibles (niv. 5), utilisateurs en Zetsu (niv. 10).</li>
 *     <li>En (interrupteur) : sphère d'aura ; tout ce qui est dedans brille. Portée selon la maîtrise de l'En.</li>
 *     <li>Shu (automatique) : avec l'aura active, l'outil tenu frappe plus fort, mine plus vite et s'use moins.</li>
 * </ul>
 */
public final class NenSenses {
    public static final int GYO_SEE_INVISIBLE = 5;
    public static final int GYO_SEE_ZETSU = 10;

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
        if (on) {
            String refusal = senseRefusal(data);
            if (refusal != null) return refusal;
        }
        data.setGyo(on);
        return null;
    }

    public static String setEn(LivingEntity entity, boolean on) {
        HunterData data = HunterStats.get(entity);
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
