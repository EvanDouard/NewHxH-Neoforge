package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.nen.Targeting;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Isaac Netero (Renforcement). Sa force vient de la Prière : une jauge propre (0 à 100) qui se remplit en priant —
 * pour un Netero, « Méditer » devient « Prière » — et que dépense chaque technique de Kannon.
 * <ul>
 *   <li>Poings de Kannon : Kannon lui prête ses poings, ses coups sont renforcés un temps (mains dorées) ;</li>
 *   <li>Bodhisattva aux mille mains : la statue surgit derrière lui et frappe d'une paume géante (cône) ;</li>
 *   <li>Main zéro : prière pleine, tout est dépensé : rayon d'aura pure, image d'impact.</li>
 * </ul>
 */
public final class Netero {
    private Netero() {}

    public static final String KIT = "netero";
    /** Prière gagnée par seconde de prière (pleine en 20 s) ; pour un mob, regain passif par seconde. */
    public static final float PRAYER_PER_SECOND = 5f, MOB_PRAYER_PER_SECOND = 2f;

    public static boolean isNetero(LivingEntity e) {
        return KIT.equals(HunterStats.get(e).kit());
    }

    /** Une seconde de prière (méditation d'un Netero) : la jauge monte, halo doré, onde à chaque quart. */
    public static void prayTick(LivingEntity entity, int meditationTick) {
        HunterData data = HunterStats.get(entity);
        if (meditationTick % 10 != 0) return;
        float before = data.kitValue(KitBars.PRAYER);
        data.addKitValue(KitBars.PRAYER, PRAYER_PER_SECOND / 2, KitBars.PRAYER_MAX);
        float after = data.kitValue(KitBars.PRAYER);
        if (meditationTick % 40 == 0) NenFx.flare(entity, 1.3f, NenFx.GOLD);
        if ((int) (before / 25) != (int) (after / 25) || (after >= KitBars.PRAYER_MAX && before < KitBars.PRAYER_MAX)) {
            // claquement des paumes : éclat aux mains et onde dorée
            NenFx.impact(entity, entity.getEyePosition().add(entity.getLookAngle().scale(0.5)).add(0, -0.4, 0), 0.9f, NenFx.WHITE);
            NenFx.shockwave(entity, 2.5f, NenFx.GOLD);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BELL_BLOCK,
                    SoundSource.PLAYERS, 0.7f, 1.2f);
            if (after >= KitBars.PRAYER_MAX && entity instanceof Player p)
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hxh.prayer_full"), true);
        }
    }

    /** Passif « Prière » : regain passif pour les mobs. */
    public static final Passive PRAYER = new Passive("netero_prayer", true) {
        @Override
        public void tick(LivingEntity owner) {
            HunterData data = HunterStats.get(owner);
            if (!(owner instanceof Player) && owner.tickCount % 20 == 0)
                data.addKitValue(KitBars.PRAYER, MOB_PRAYER_PER_SECOND, KitBars.PRAYER_MAX);
        }
    };

    /** Paume de la statue : un cône large et long (12 blocs, 40°). */
    private static final Techniques.PalmWave KANNON_PALM = new Techniques.PalmWave("netero_kannon_palm", NenType.ENHANCER,
            0, 0, 12.0, 40.0, 14f, 3.0f, NenFx.GOLD);

    /** Bodhisattva aux mille mains : la statue surgit derrière Netero et frappe d'une paume géante. */
    public static final Technique KANNON = Techniques.register(new Technique("netero_kannon", NenType.ENHANCER, 30, 200) {
        @Override
        public double aiRange() {
            return 10;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return KANNON_PALM.conditions();
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (caster.level() instanceof ServerLevel level)
                kannon(level, caster, 9f, 0xF0FFFFFF, 40, "strike");
            KANNON_PALM.perform(caster, power);
            return true;
        }
    }).costs(KitBars.PRAYER, 10).requiresNenLevel(10);

    /** Main zéro : prière pleine, tout part. Rayon de 24 blocs, 2,5 de large : 40 dégâts, image d'impact. */
    public static final Technique ZERO_HAND = Techniques.register(new Technique("netero_zero_hand", NenType.ENHANCER, 60, 1200) {
        @Override
        public double aiRange() {
            return 18;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.beam", 24);
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            kannon(level, caster, 12f, 0xE8FFFFFF, 60, "zero");
            Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle();
            // Toute l'aura qui reste part dans le rayon : de 15 dégâts (rien) à 60 (aura pleine)
            HunterData data = HunterStats.get(caster);
            float spent = data.auraMax() <= 0 ? 0f : Math.min(1f, data.aura() / data.auraMax());
            data.setAura(0f);
            float damage = (15f + 45f * spent) * power;
            List<LivingEntity> hit = beamTargets(caster, eye, look, 24, 2.5);
            if (com.evandouard.hxh.HxHGameRules.canDestroy(level))
                for (int i = 3; i <= 24; i += 3)
                    com.evandouard.hxh.combat.Impacts.crater(level, eye.add(look.scale(i)), 1.8f, caster);
            for (LivingEntity t : hit) {
                t.invulnerableTime = 0;
                t.hurt(Techniques.damageFrom(caster), damage);
                t.setDeltaMovement(t.getDeltaMovement().add(look.scale(2.5)));
                t.hurtMarked = true;
                NenFx.impactFrame(caster, t, 16);
            }
            for (int i = 2; i <= 24; i += 2) NenFx.impactAt(level, eye.add(look.scale(i)), 1.6f, i % 4 == 0 ? NenFx.WHITE : NenFx.GOLD);
            NenFx.quake(level, eye.add(look.scale(24)), 4f);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.2f, 0.5f);
            return true;
        }
    }).costs(KitBars.PRAYER, KitBars.PRAYER_MAX).requiresNenLevel(40);

    /** Entités dans un cylindre de rayon {@code width} le long du regard. */
    static List<LivingEntity> beamTargets(LivingEntity caster, Vec3 from, Vec3 dir, double length, double width) {
        AABB box = new AABB(from, from.add(dir.scale(length))).inflate(width);
        return caster.level().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == caster || !e.isAlive() || e.isSpectator()) return false;
            Vec3 to = e.getBoundingBox().getCenter().subtract(from);
            double along = to.dot(dir);
            if (along < 0 || along > length) return false;
            return to.subtract(dir.scale(along)).length() <= width + e.getBbWidth() / 2;
        });
    }

    /**
     * Le Bodhisattva aux mille mains surgit derrière Netero (modèle GeckoLib, voir tools/models/summon_kannon.py) :
     * {@code height} blocs de haut, puis il joue le geste ({@code strike} : paume ; {@code zero} : Main zéro).
     */
    public static void kannon(net.minecraft.server.level.ServerLevel level, LivingEntity caster, float height, int color,
                              int life, String action) {
        com.evandouard.hxh.summon.NenSummons.dismiss(level, com.evandouard.hxh.summon.NenSummons.KANNON, caster);
        com.evandouard.hxh.summon.NenSummonEntity k = com.evandouard.hxh.summon.NenSummons.spawn(level, caster,
                com.evandouard.hxh.summon.NenSummons.KANNON, caster, com.evandouard.hxh.summon.NenSummonEntity.Anchor.BEHIND,
                height / 4f, 1.4f + height * 0.12f, color, life);
        if (k != null) k.actLater(action, 8);
    }
}
