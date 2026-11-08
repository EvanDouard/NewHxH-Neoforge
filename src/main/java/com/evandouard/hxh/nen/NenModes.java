package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Logique serveur de l'aura et des modes de base (Ten, Zetsu, Ren).
 * Marche pour toute LivingEntity : un mob peut passer en Ren exactement comme un joueur.
 */
public final class NenModes {
    private NenModes() {}

    public static int computeAuraMax(HunterData data) {
        return HxHConfig.AURA_BASE.get() + HxHConfig.AURA_PER_WILLPOWER.get() * data.level(Stat.WILLPOWER);
    }

    /** Débloque le Nen : aura pleine, mode neutre. */
    public static void unlock(LivingEntity entity) {
        HunterData data = HunterStats.get(entity);
        data.setNenUnlocked(true);
        if (entity instanceof net.minecraft.world.entity.Mob mob) NenMobAI.onAwakened(mob);
        data.setAuraMax(computeAuraMax(data));
        data.setAura(data.auraMax());
        data.setMode(NenMode.NONE);
    }

    /**
     * Essaie de passer dans un mode.
     * @return null si c'est accepté, sinon la clé de traduction du refus
     */
    public static String trySetMode(LivingEntity entity, NenMode mode) {
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked()) return "message.hxh.nen_locked";
        if ((mode == NenMode.REN || mode == NenMode.KEN) && data.aura() < 1) return "message.hxh.no_aura";
        data.setMode(mode);
        return null;
    }

    /** Le Zetsu complet se débloque en travaillant l'In. */
    public static boolean zetsuUnlocked(HunterData data) {
        return data.masteryLevel(com.evandouard.hxh.data.Mastery.IN) >= HxHConfig.ZETSU_UNLOCK_IN_LEVEL.get();
    }

    /** Touche pressée : même mode = retour au neutre, sinon on essaie le nouveau mode. */
    public static void toggle(LivingEntity entity, NenMode mode) {
        HunterData data = HunterStats.get(entity);
        NenMode target = data.mode() == mode ? NenMode.NONE : mode;
        // Touche de dissimulation : Neutre → In → Zetsu (si débloqué) → Neutre
        if (mode == NenMode.ZETSU || mode == NenMode.IN) {
            target = switch (data.mode()) {
                case IN -> zetsuUnlocked(data) ? NenMode.ZETSU : NenMode.NONE;
                case ZETSU -> NenMode.NONE;
                default -> NenMode.IN;
            };
        }
        String refusal = trySetMode(entity, target);
        if (refusal == null && (target == NenMode.ZETSU || target == NenMode.IN)) {
            if (target == NenMode.ZETSU) data.setHealing(false); // pas de soin sans aura
            data.setGyo(false); // dissimulé : Gyo et En coupés
            data.setEn(false);
        }
        if (entity instanceof Player player) {
            player.displayClientMessage(refusal != null
                    ? Component.translatable(refusal)
                    : Component.translatable("message.hxh.mode_changed", Component.translatable(target.translationKey())), true);
        }
    }

    /** Appelé à chaque tick serveur pour les entités ayant des données Hunter. */
    public static void tick(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked()) return;

        data.setAuraMax(computeAuraMax(data));
        float regen = (float) (HxHConfig.AURA_REGEN_PER_SECOND.get() / 20.0);
        if (data.meditating()) regen *= 2; // la méditation recharge l'aura plus vite

        switch (data.mode()) {
            case NONE, TEN, IN -> data.setAura(data.aura() + regen);
            case ZETSU -> data.setAura(data.aura() + regen * (float) (HxHConfig.ZETSU_REGEN_MULTIPLIER.get()
                    + data.masteryLevel(com.evandouard.hxh.data.Mastery.ZETSU) * 0.02));
            case REN -> {
                data.setAura(data.aura() - (float) (HxHConfig.REN_DRAIN_PER_SECOND.get() / 20.0));
                if (data.aura() <= 0) exhaust(entity, data);
            }
            case KEN -> {
                double drain = Math.max(1.5, HxHConfig.KEN_DRAIN_PER_SECOND.get()
                        - data.masteryLevel(com.evandouard.hxh.data.Mastery.KEN) * 0.02);
                data.setAura(data.aura() - (float) (drain / 20.0));
                if (data.aura() <= 0) exhaust(entity, data);
            }
        }

        // Entraînement : chaque seconde passée dans une posture fait progresser sa maîtrise et le Nen
        if (entity.tickCount % 20 == 0) {
            switch (data.mode()) {
                case TEN -> NenProgress.train(entity, com.evandouard.hxh.data.Mastery.TEN, 1);
                case ZETSU -> NenProgress.train(entity, com.evandouard.hxh.data.Mastery.ZETSU, 1);
                case IN -> NenProgress.train(entity, com.evandouard.hxh.data.Mastery.IN, 1);
                case REN -> {
                    NenProgress.train(entity, com.evandouard.hxh.data.Mastery.REN, 2);
                    HunterStats.addXp(entity, Stat.WILLPOWER, 1);
                }
                case KEN -> {
                    NenProgress.train(entity, com.evandouard.hxh.data.Mastery.KEN, 2);
                    HunterStats.addXp(entity, Stat.WILLPOWER, 1);
                }
                default -> { }
            }
        }
    }

    private static void exhaust(LivingEntity entity, HunterData data) {
        data.setMode(NenMode.NONE);
        int ticks = HxHConfig.EXHAUSTION_TICKS.get();
        if (ticks > 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, false, true));
        }
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.hxh.exhausted"), true);
        }
    }
}
