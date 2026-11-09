package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.nen.Targeting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Passifs jouables des personnages du premier lot.
 */
public final class Passives {
    private Passives() {}

    /** Gon — Instinct sauvage : +10 % de dégâts au corps à corps. */
    public static final Passive GON_WILD_INSTINCT = new Passive("gon_wild_instinct", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            return amount * 1.10f;
        }
    };

    /** Killua — Corps Zoldyck : immunisé à la foudre et au poison. */
    public static final Passive ZOLDYCK_BODY = new Passive("zoldyck_body", true) {
        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            return source.is(DamageTypes.LIGHTNING_BOLT) ? 0f : amount;
        }

        @Override
        public void tick(LivingEntity owner) {
            if (owner.hasEffect(MobEffects.POISON)) owner.removeEffect(MobEffects.POISON);
        }
    };

    /** Illumi — Entraînement Zoldyck : immunisé au poison. */
    public static final Passive POISON_TRAINING = new Passive("poison_training", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.hasEffect(MobEffects.POISON)) owner.removeEffect(MobEffects.POISON);
        }
    };

    /** Kurapika — Chaîne radiesthésique : les monstres à 12 blocs brillent (vus à travers les murs). */
    public static final Passive DOWSING_CHAIN = new Passive("dowsing_chain", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 40 != 0) return;
            for (LivingEntity e : Targeting.around(owner, 12)) {
                if (e instanceof Enemy) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            }
        }
    };

    /** Neferpitou — Agilité féline : aucun dégât de chute. */
    public static final Passive FELINE_AGILITY = new Passive("feline_agility", true) {
        @Override
        public float modifyIncoming(LivingEntity owner, DamageSource source, float amount) {
            return source.is(DamageTypeTags.IS_FALL) ? 0f : amount;
        }
    };

    /** Leorio — Médecin : récupère 1 PV toutes les 5 secondes. */
    public static final Passive DOCTOR = new Passive("doctor", true) {
        @Override
        public void tick(LivingEntity owner) {
            if (owner.tickCount % 100 == 0 && owner.getHealth() < owner.getMaxHealth()) owner.heal(1f);
        }
    };

    /** Hisoka — Frisson du combat : plus l'adversaire a de vie max, plus ses coups font mal (+1 % par PV au-delà de 20, max +30 %). */
    public static final Passive HISOKA_THRILL = new Passive("hisoka_thrill", true) {
        @Override
        public float modifyOutgoingMelee(LivingEntity owner, LivingEntity target, float amount) {
            float bonus = Math.min(0.30f, Math.max(0f, target.getMaxHealth() - 20f) * 0.01f);
            return amount * (1f + bonus);
        }
    };
}
