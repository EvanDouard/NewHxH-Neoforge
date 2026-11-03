package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Défense et attaque selon la posture. Les dégâts d'une attaque sont réduits de façon plate :
 * <pre>
 * dégâts subis = max(0, dégâts − (résistance d'Endurance + résistance de Nen de la posture))
 * </pre>
 * Zetsu et Ko : 0 de résistance de Nen. Neutre : la moitié du Ten. Ten et Ren : le Ten. Ken : élevée.
 */
public final class NenDefense {
    private NenDefense() {}

    public static float enduranceResistance(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return 0f;
        return (float) (HunterStats.get(entity).level(Stat.ENDURANCE) * HxHConfig.ENDURANCE_FLAT_PER_LEVEL.get());
    }

    public static float tenResistance(HunterData data) {
        return (float) (HxHConfig.TEN_RESISTANCE_BASE.get()
                + data.masteryLevel(Mastery.TEN) * HxHConfig.TEN_RESISTANCE_PER_LEVEL.get());
    }

    public static float kenResistance(HunterData data) {
        return (float) (HxHConfig.KEN_RESISTANCE_BASE.get()
                + data.masteryLevel(Mastery.KEN) * HxHConfig.KEN_RESISTANCE_PER_LEVEL.get());
    }

    public static boolean koActive(LivingEntity entity) {
        return entity.hasData(ModAttachments.HUNTER)
                && HunterStats.get(entity).koUntil() > entity.level().getGameTime();
    }

    /** Résistance de Nen actuelle (PV retirés à chaque attaque reçue). */
    public static float nenResistance(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return 0f;
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked() || koActive(entity)) return 0f; // Ko : tout est dans le poing
        float base = switch (data.mode()) {
            case ZETSU -> 0f;
            case NONE, IN -> tenResistance(data) * 0.5f;
            case TEN, REN -> tenResistance(data);
            case KEN -> kenResistance(data);
        };
        return base * ryuDefenseFactor(data);
    }

    /** Résistance totale, en PV retirés à chaque attaque reçue. */
    public static float totalResistance(LivingEntity entity) {
        return enduranceResistance(entity) + nenResistance(entity);
    }

    /**
     * Multiplicateur des dégâts au corps à corps de l'attaquant : Ren, Ken (moitié du bonus de Ren), Ko.
     * @param consumeKo true quand le coup part vraiment : le Ko est alors dépensé
     */
    public static float outgoingFactor(LivingEntity attacker, boolean consumeKo) {
        if (!attacker.hasData(ModAttachments.HUNTER)) return 1f;
        HunterData data = HunterStats.get(attacker);
        if (!data.nenUnlocked()) return 1f;
        double renBonus = HxHConfig.REN_DAMAGE_BONUS.get() + data.masteryLevel(Mastery.REN) * 0.004;
        float factor = switch (data.mode()) {
            case REN -> (float) (1 + renBonus);
            case KEN -> (float) (1 + renBonus * 0.5);
            default -> 1f;
        };
        factor *= ryuAttackFactor(data);
        if (koActive(attacker)) {
            factor *= (float) (HxHConfig.KO_DAMAGE_MULTIPLIER.get() + data.masteryLevel(Mastery.KO) * 0.02);
            if (consumeKo) {
                data.setKoUntil(0);
                NenProgress.train(attacker, Mastery.KO, 5);
            }
        }
        return factor;
    }

    // ---------- Ryu : répartition attaque / défense ----------

    /** Amplitude du Ryu : plus il est maîtrisé, plus la répartition a d'effet (±30 % à ±70 %). */
    private static double ryuSwing(HunterData data) {
        return (data.ryuAttack() - 50) / 100.0 * (0.6 + data.masteryLevel(Mastery.RYU) * 0.008);
    }

    /** Multiplicateur des dégâts au corps à corps selon la part d'aura mise dans l'attaque. */
    public static float ryuAttackFactor(HunterData data) {
        return (float) (1 + ryuSwing(data));
    }

    /** Multiplicateur de la résistance de Nen : ce qui va dans l'attaque manque à la défense. */
    public static float ryuDefenseFactor(HunterData data) {
        return (float) Math.max(0, 1 - ryuSwing(data));
    }

    public static int koCost(HunterData data) {
        return Math.max(10, (int) Math.round(HxHConfig.KO_AURA_COST.get() - data.masteryLevel(Mastery.KO) * 0.15));
    }

    /**
     * Ko : concentre toute l'aura dans le poing pour la prochaine attaque (5 s), le corps est découvert.
     * @return null si accepté, sinon la clé du refus
     */
    public static String ko(LivingEntity entity) {
        HunterData data = HunterStats.get(entity);
        if (!data.nenUnlocked()) return "message.hxh.nen_locked";
        if (data.mode() == NenMode.ZETSU) return "message.hxh.hatsu_zetsu";
        int cost = koCost(data);
        if (data.aura() < cost) return "message.hxh.no_aura";
        data.setAura(data.aura() - cost);
        data.setKoUntil(entity.level().getGameTime() + 100);
        NenProgress.train(entity, Mastery.KO, 2);
        return null;
    }

    public static void requestKo(LivingEntity entity) {
        String refusal = ko(entity);
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable(refusal != null ? refusal : "message.hxh.ko_ready"), true);
        }
    }
}
