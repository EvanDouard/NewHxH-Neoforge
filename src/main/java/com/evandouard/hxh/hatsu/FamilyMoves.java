package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.nen.Targeting;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Coups secrets de famille : une technique sans Nen par famille (champ {@code secret_move} du fichier de famille).
 * Elle se place dans le menu juste avant les coups de base, se règle sur les stats (pas sur l'affinité du Nen) et ne
 * peut être ni copiée par Ging ni volée par Chrollo. Comme toutes les techniques, n'importe quelle entité peut la lancer.
 */
public final class FamilyMoves {
    private FamilyMoves() {}

    /** Couleurs : acier Zoldyck, écarlate Kurta, vert Freecss, blanc médical Paladiknight. */
    static final int ZOLDYCK = 0xB9C6D8, KURTA = 0xE0263A, FREECSS = 0x5BC85B, PALADIKNIGHT = 0xF2F6FF;

    private static int stat(LivingEntity e, Stat s) {
        return e.hasData(com.evandouard.hxh.data.ModAttachments.HUNTER) ? HunterStats.get(e).level(s) : 0;
    }

    /** Base commune : pas de Nen, pas d'aura, la catégorie ne sert qu'à l'affichage. */
    abstract static class Move extends Technique {
        Move(String id, int cooldown) {
            super(id, NenType.ENHANCER, 0, cooldown);
        }

        @Override
        public boolean isFamily() {
            return true;
        }
    }

    /**
     * Zoldyck — Main-griffe : la main se raidit en griffe, l'assassin fond sur la cible (8 blocs devant) et lui arrache
     * le cœur… version Minecraft : gros dégâts et saignement court. Dégâts 6 + Force × 0,08 + Vitesse × 0,06.
     */
    public static final Technique ZOLDYCK_CLAW = Techniques.register(new Move("family_zoldyck_claw", 300) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.target", "8.0");
        }

        @Override
        public double aiRange() {
            return 8;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity target = Targeting.inFront(caster, 8, 0.9);
            if (target == null) {
                NenFx.slash(caster, caster.getEyePosition().add(caster.getLookAngle().scale(2)), 1.2f, ZOLDYCK);
                return false;
            }
            // Bond silencieux juste devant la cible
            Vec3 dir = target.position().subtract(caster.position()).multiply(1, 0, 1);
            if (dir.lengthSqr() > 4) {
                Vec3 stop = target.position().subtract(dir.normalize().scale(1.2));
                caster.teleportTo(stop.x, target.getY(), stop.z);
                caster.hurtMarked = true;
            }
            float dmg = 6f + stat(caster, Stat.STRENGTH) * 0.08f + stat(caster, Stat.SPEED) * 0.06f;
            target.invulnerableTime = 0;
            target.hurt(Techniques.damageFrom(caster), dmg);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, false, true), caster);
            NenFx.slash(caster, target.getBoundingBox().getCenter(), 1.6f, ZOLDYCK);
            NenFx.impactOn(caster, target, 1.2f, KURTA);
            Techniques.sound(target, SoundEvents.PLAYER_ATTACK_SWEEP);
            return true;
        }
    });

    /**
     * Kurta — Colère écarlate : les yeux virent à l'écarlate pendant 15 s : Force II et Résistance I (Force III sous
     * 40 % de vie). Le bonus passif des yeux écarlates sur les techniques reste à part.
     */
    public static final Technique KURTA_SCARLET = Techniques.register(new Move("family_kurta_scarlet", 1800) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            boolean low = caster.getHealth() <= caster.getMaxHealth() * 0.4f;
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, low ? 2 : 1, false, false, true), caster);
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0, false, false, true), caster);
            NenFx.flare(caster, 1.4f, KURTA);
            NenFx.crackle(caster, KURTA, 300);
            Techniques.sound(caster, SoundEvents.WARDEN_HEARTBEAT);
            return true;
        }
    });

    /**
     * Freecss — Instinct du chasseur : odorat et ouïe de Gon. Toutes les créatures à 32 blocs sont repérées 12 s (vues
     * à travers les murs, pour le seul lanceur) et il gagne Vitesse I. Rate s'il n'y a personne autour.
     */
    public static final Technique FREECSS_INSTINCT = Techniques.register(new Move("family_freecss_instinct", 800) {
        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.area", "32");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            var around = Targeting.around(caster, 32);
            for (LivingEntity e : around) NenFx.mark(caster, e, FREECSS, 240);
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0, false, false, true), caster);
            NenFx.shockwave(caster, 3f, FREECSS);
            Techniques.sound(caster, SoundEvents.FOX_SNIFF);
            return !around.isEmpty();
        }
    });

    /**
     * Paladiknight — Premiers soins : soigne l'allié devant (4 blocs) ou soi-même : 4 PV + Vitalité × 0,08, et retire
     * poison et faiblesse.
     */
    public static final Technique PALADIKNIGHT_FIRST_AID = Techniques.register(new Move("family_paladiknight_first_aid", 900) {
        @Override
        public boolean isHeal() {
            return true;
        }

        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.target", "4.0");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity patient = Targeting.inFront(caster, 4, 0.85);
            if (patient == null || patient instanceof net.minecraft.world.entity.monster.Enemy) patient = caster;
            patient.heal(4f + stat(caster, Stat.VITALITY) * 0.08f);
            patient.removeEffect(MobEffects.POISON);
            patient.removeEffect(MobEffects.WEAKNESS);
            NenFx.flare(patient, 1f, PALADIKNIGHT);
            Techniques.sound(patient, SoundEvents.BREWING_STAND_BREW);
            return true;
        }
    });

    /** Charge la classe (les techniques s'enregistrent). */
    public static void init() {}

    /** Coup secret enregistré pour cet identifiant, ou null. */
    public static Technique get(String id) {
        Technique t = Techniques.get(id);
        return t != null && t.isFamily() ? t : null;
    }

    /** Donne le coup secret de sa famille à une entité (vide = aucun). */
    public static void grant(LivingEntity entity, String moveId) {
        HunterData d = HunterStats.get(entity);
        d.setFamilyMove(moveId != null && get(moveId) != null ? moveId : "");
    }
}
