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
        if (mode == NenMode.REN && data.aura() < 1) return "message.hxh.no_aura";
        data.setMode(mode);
        return null;
    }

    /** Touche pressée : même mode = retour au neutre, sinon on essaie le nouveau mode. */
    public static void toggle(LivingEntity entity, NenMode mode) {
        HunterData data = HunterStats.get(entity);
        NenMode target = data.mode() == mode ? NenMode.NONE : mode;
        String refusal = trySetMode(entity, target);
        if (refusal == null && target == NenMode.ZETSU) data.setHealing(false); // pas de soin sans aura
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
            case NONE, TEN -> data.setAura(data.aura() + regen);
            case ZETSU -> data.setAura(data.aura() + regen * HxHConfig.ZETSU_REGEN_MULTIPLIER.get().floatValue());
            case REN -> {
                data.setAura(data.aura() - (float) (HxHConfig.REN_DRAIN_PER_SECOND.get() / 20.0));
                if (data.aura() <= 0) exhaust(entity, data);
            }
        }

        // Volonté : s'entraîne en maintenant l'aura (Ren : 1 XP/s, Ten : 1 XP / 5 s)
        if (data.mode() == NenMode.REN && entity.tickCount % 20 == 0) HunterStats.addXp(entity, Stat.WILLPOWER, 1);
        if (data.mode() == NenMode.TEN && entity.tickCount % 100 == 0) HunterStats.addXp(entity, Stat.WILLPOWER, 1);
    }

    private static void exhaust(LivingEntity entity, HunterData data) {
        data.setMode(NenMode.NONE);
        int ticks = HxHConfig.EXHAUSTION_TICKS.get();
        if (ticks > 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        }
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.hxh.exhausted"), true);
        }
    }

    /** Multiplicateur des dégâts subis selon le mode. */
    public static float incomingFactor(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return 1f;
        return switch (HunterStats.get(entity).mode()) {
            case TEN -> (float) (1.0 - HxHConfig.TEN_DAMAGE_REDUCTION.get());
            case ZETSU -> (float) (1.0 + HxHConfig.ZETSU_DAMAGE_TAKEN_BONUS.get());
            default -> 1f;
        };
    }

    /** Multiplicateur des dégâts infligés au corps à corps selon le mode. */
    public static float outgoingFactor(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.HUNTER)) return 1f;
        return HunterStats.get(entity).mode() == NenMode.REN
                ? (float) (1.0 + HxHConfig.REN_DAMAGE_BONUS.get())
                : 1f;
    }
}
