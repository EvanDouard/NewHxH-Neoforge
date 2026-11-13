package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.nen.Targeting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Types de techniques réutilisables ajoutés avec les personnages de la fiche {@code docs/kits-personnages.md} :
 * zone autour de soi avec effets, soutien des alliés, rafales, pluie d'impacts, balayage en arc, rayon, tempête qui
 * dure, bombe à retardement. Comme le reste, ils marchent pour n'importe quelle LivingEntity.
 */
public final class Kinds {
    private Kinds() {}

    // ---------- Aides ----------

    /** Allié : pour un joueur, les autres joueurs et ses bêtes apprivoisées ; pour un mob, ceux de son espèce. */
    public static boolean isAlly(LivingEntity caster, LivingEntity e) {
        if (e == caster) return true;
        if (caster instanceof Player cp) {
            // joueurs : toujours alliés, sauf membres d'organisations ennemies (Brigade Fantôme contre Association…)
            if (e instanceof Player ep) return !com.evandouard.hxh.org.Organizations.enemies(cp, ep);
            return e instanceof TamableAnimal t && t.isOwnedBy(caster);
        }
        return e.getType() == caster.getType();
    }

    /** Ennemis autour d'un point (les alliés du lanceur sont épargnés). */
    public static List<LivingEntity> enemiesAround(LivingEntity caster, Vec3 at, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : caster.level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(at, at).inflate(radius), e -> e.isAlive() && !e.isSpectator())) {
            if (isAlly(caster, e)) continue;
            if (e.getBoundingBox().getCenter().distanceTo(at) <= radius + e.getBbWidth() / 2) out.add(e);
        }
        return out;
    }

    /** Point visé : le premier bloc dans le regard, au plus à {@code distance}. */
    public static Vec3 aimPoint(LivingEntity caster, double distance) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(distance));
        var hit = caster.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return hit.getLocation();
    }

    static void hurt(LivingEntity caster, LivingEntity t, float amount) {
        t.invulnerableTime = 0;
        t.hurt(Techniques.damageFrom(caster), amount);
    }

    static void sound(LivingEntity at, SoundEvent sound) {
        if (sound != null) at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, 0.8f, 1f);
    }

    // ---------- Zone autour de soi, avec effets ----------

    /** Onde autour du lanceur : dégâts, recul et effets sur chaque ennemi touché. */
    public static class Pulse extends Technique {
        final double radius;
        final float damage, knockback;
        final List<Techniques.Effect> effects;
        final Techniques.Look look;
        final SoundEvent sound;

        Pulse(String id, NenType category, int cost, int cooldown, double radius, float damage, float knockback,
              List<Techniques.Effect> effects, Techniques.Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.effects = effects;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.area",
                    String.format(java.util.Locale.ROOT, "%.0f", radius));
        }

        @Override
        public double aiRange() {
            return radius;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            List<LivingEntity> hit = enemiesAround(caster, caster.position().add(0, 1, 0), radius);
            for (LivingEntity t : hit) {
                if (damage > 0) hurt(caster, t, damage * power);
                if (knockback > 0) t.knockback(knockback * power, caster.getX() - t.getX(), caster.getZ() - t.getZ());
                for (Techniques.Effect e : effects) t.addEffect(e.create(power), caster);
                Techniques.showStrike(look, caster, t, t.getBoundingBox().getCenter(), 40);
            }
            NenFx.shockwave(caster, (float) radius, look.color());
            sound(caster, sound);
            return true;
        }
    }

    // ---------- Soutien des alliés ----------

    /** Le lanceur et ses alliés proches sont soignés et reçoivent des effets. */
    public static class AllyBuff extends Technique {
        final double radius;
        final float heal;
        final List<Techniques.Effect> effects;
        final Techniques.Look look;
        final SoundEvent sound;

        AllyBuff(String id, NenType category, int cost, int cooldown, double radius, float heal,
                 List<Techniques.Effect> effects, Techniques.Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.radius = radius;
            this.heal = heal;
            this.effects = effects;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public boolean isHeal() {
            return heal > 0;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            List<LivingEntity> allies = new ArrayList<>();
            allies.add(caster);
            for (LivingEntity e : Targeting.around(caster, radius)) if (isAlly(caster, e)) allies.add(e);
            for (LivingEntity a : allies) {
                if (heal > 0) a.heal(heal * power);
                for (Techniques.Effect e : effects) a.addEffect(e.create(power), caster);
                Techniques.showSelf(look, a, 40);
            }
            NenFx.shockwave(caster, (float) radius, look.color());
            sound(caster, sound);
            return true;
        }
    }

    // ---------- Rafales ----------

    /** Plusieurs tirs d'aura de suite vers la cible devant soi (doigts-mitrailleuses, orbes…). */
    public static class Burst extends Technique {
        final double range;
        final int shots, interval;
        final float damagePer;
        final List<Techniques.Effect> effects;
        final Techniques.Look look;
        final SoundEvent sound;

        Burst(String id, NenType category, int cost, int cooldown, double range, int shots, int interval, float damagePer,
              List<Techniques.Effect> effects, Techniques.Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.range = range;
            this.shots = shots;
            this.interval = interval;
            this.damagePer = damagePer;
            this.effects = effects;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.target",
                    String.format(java.util.Locale.ROOT, "%.1f", range));
        }

        @Override
        public double aiRange() {
            return range;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            boolean any = Targeting.inFront(caster, range, 0.9) != null;
            for (int i = 0; i < shots; i++) {
                NenTasks.later(i * interval, () -> {
                    if (!caster.isAlive()) return;
                    LivingEntity t = Targeting.inFront(caster, range, 0.9);
                    Vec3 end = t != null ? t.getBoundingBox().getCenter() : caster.getEyePosition().add(caster.getLookAngle().scale(range));
                    Techniques.showStrike(look, caster, t, end, 20);
                    if (t != null) {
                        hurt(caster, t, damagePer * power);
                        for (Techniques.Effect e : effects) t.addEffect(e.create(power), caster);
                    }
                    sound(caster, sound);
                });
            }
            return any;
        }
    }

    // ---------- Pluie d'impacts ----------

    /** Pluie d'impacts autour du point visé (dragons de Zeno, démons de Razor…). */
    public static class Rain extends Technique {
        final double distance, radius;
        final int drops, duration;
        final float damagePer;
        final int color;
        final SoundEvent sound;
        /** Image qui tombe à chaque impact, et images posées en cercle autour du centre au départ (ou null). */
        NenFx.Sprite dropSprite, ringSprite;

        Rain withSprites(NenFx.Sprite drop, NenFx.Sprite ring) {
            this.dropSprite = drop;
            this.ringSprite = ring;
            return this;
        }

        Rain(String id, NenType category, int cost, int cooldown, double distance, double radius, int drops,
             int duration, float damagePer, int color, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.distance = distance;
            this.radius = radius;
            this.drops = drops;
            this.duration = duration;
            this.damagePer = damagePer;
            this.color = color;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.rain",
                    String.format(java.util.Locale.ROOT, "%.0f", distance), String.format(java.util.Locale.ROOT, "%.0f", radius));
        }

        @Override
        public double aiRange() {
            return distance;
        }

        /** Centre de la pluie : la cible devant, sinon le point visé. */
        Vec3 center(LivingEntity caster) {
            LivingEntity t = Targeting.inFront(caster, distance, 0.85);
            return t != null ? t.position() : aimPoint(caster, distance);
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            Vec3 c = center(caster);
            var rnd = caster.getRandom();
            if (ringSprite != null) for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3;
                NenFx.sprite(level, ringSprite, null, c.add(Math.cos(a) * (radius + 1), 1.2, Math.sin(a) * (radius + 1)), 2.2f, color, duration + 20);
            }
            int fall = dropSprite != null ? 6 : 0;
            for (int i = 0; i < drops; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * radius;
                Vec3 at = c.add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                int when = i * Math.max(1, duration / Math.max(1, drops));
                if (dropSprite != null) NenTasks.later(when, () -> NenFx.sprite(level, dropSprite, null, at.add(0, 0.8, 0), 1.8f, color, 10));
                NenTasks.later(when + fall, () -> {
                    NenFx.impactAt(level, at.add(0, 0.6, 0), 1.3f, color);
                    NenFx.slashAt(level, at.add(0, 1.6, 0), 1.1f, color);
                    for (LivingEntity t : enemiesAround(caster, at.add(0, 0.8, 0), 1.6)) hurt(caster, t, damagePer * power);
                    level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 0.5f, 1.2f);
                });
            }
            return true;
        }
    }

    // ---------- Balayage en arc ----------

    /** Coupe en arc devant soi : toutes les cibles du demi-cercle avant. */
    public static class Sweep extends Technique {
        final double reach, halfAngle;
        final float damage, knockback;
        final int color;
        final SoundEvent sound;

        Sweep(String id, NenType category, int cost, int cooldown, double reach, double halfAngleDeg, float damage,
              float knockback, int color, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.reach = reach;
            this.halfAngle = halfAngleDeg;
            this.damage = damage;
            this.knockback = knockback;
            this.color = color;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.cone",
                    String.format(java.util.Locale.ROOT, "%.0f", reach), String.format(java.util.Locale.ROOT, "%.0f", halfAngle));
        }

        @Override
        public double aiRange() {
            return reach;
        }

        static List<LivingEntity> inArc(LivingEntity caster, double reach, double halfAngleDeg) {
            Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle();
            double cos = Math.cos(Math.toRadians(halfAngleDeg));
            List<LivingEntity> out = new ArrayList<>();
            for (LivingEntity e : caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(reach),
                    e -> e != caster && e.isAlive() && !e.isSpectator())) {
                Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                double d = to.length();
                if (d > reach + e.getBbWidth() || d < 1e-4) continue;
                if (to.normalize().dot(look) < cos && d > 1.5) continue;
                out.add(e);
            }
            return out;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            List<LivingEntity> hit = inArc(caster, reach, halfAngle);
            for (LivingEntity t : hit) {
                hurt(caster, t, damage * power);
                if (knockback > 0) t.knockback(knockback * power, caster.getX() - t.getX(), caster.getZ() - t.getZ());
                NenFx.slash(caster, t.getBoundingBox().getCenter(), 1.2f, color);
            }
            NenFx.slash(caster, caster.getEyePosition().add(caster.getLookAngle().scale(reach * 0.6)), (float) reach * 0.6f, color);
            sound(caster, sound);
            return !hit.isEmpty();
        }
    }

    // ---------- Rayon ----------

    /** Rayon d'aura droit devant : tout ce qui est sur la ligne (Photon de Meruem…). */
    public static class Beam extends Technique {
        final double length, width;
        final float damage;
        final int color;
        final SoundEvent sound;

        Beam(String id, NenType category, int cost, int cooldown, double length, double width, float damage, int color,
             SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.length = length;
            this.width = width;
            this.damage = damage;
            this.color = color;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.beam", (int) length);
        }

        @Override
        public double aiRange() {
            return length * 0.8;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            Vec3 eye = caster.getEyePosition(), look = caster.getLookAngle();
            List<LivingEntity> hit = Netero.beamTargets(caster, eye, look, length, width);
            for (LivingEntity t : hit) {
                hurt(caster, t, damage * power);
                NenFx.impactOn(caster, t, 1.5f, color);
            }
            for (int i = 1; i <= length; i += 2) NenFx.impactAt(level, eye.add(look.scale(i)), 0.9f, color);
            sound(caster, sound);
            return !hit.isEmpty();
        }
    }

    // ---------- Tempête qui dure ----------

    /** Plusieurs vagues de coupures devant soi pendant un moment (papiers de Kalluto, fumée de Morel…). */
    public static class Storm extends Technique {
        final double reach, halfAngle;
        final int waves, interval;
        final float damagePer;
        final List<Techniques.Effect> effects;
        final int color;
        final SoundEvent sound;
        /** Image posée sur chaque cible à chaque vague (soldats de fumée de Morel…), ou null. */
        NenFx.Sprite sprite;

        Storm withSprite(NenFx.Sprite s) {
            this.sprite = s;
            return this;
        }

        Storm(String id, NenType category, int cost, int cooldown, double reach, double halfAngleDeg, int waves,
              int interval, float damagePer, List<Techniques.Effect> effects, int color, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.reach = reach;
            this.halfAngle = halfAngleDeg;
            this.waves = waves;
            this.interval = interval;
            this.damagePer = damagePer;
            this.effects = effects;
            this.color = color;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.cone",
                    String.format(java.util.Locale.ROOT, "%.0f", reach), String.format(java.util.Locale.ROOT, "%.0f", halfAngle));
        }

        @Override
        public double aiRange() {
            return reach;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            for (int w = 0; w < waves; w++) {
                NenTasks.later(w * interval, () -> {
                    if (!caster.isAlive() || !(caster.level() instanceof ServerLevel level)) return;
                    for (LivingEntity t : Sweep.inArc(caster, reach, halfAngle)) {
                        if (isAlly(caster, t)) continue;
                        hurt(caster, t, damagePer * power);
                        for (Techniques.Effect e : effects) t.addEffect(e.create(power), caster);
                        NenFx.slashAt(level, t.getBoundingBox().getCenter(), 0.9f, color);
                        if (sprite != null && sprite != NenFx.Sprite.PAPER) NenFx.sprite(level, sprite, null,
                                t.position().add(caster.getLookAngle().scale(-1.2)).add(0, 1, 0), 2f, color, interval + 6);
                    }
                    var rnd = caster.getRandom();
                    Vec3 at = caster.getEyePosition().add(caster.getLookAngle().scale(1.5 + rnd.nextDouble() * reach * 0.7))
                            .add(rnd.nextGaussian() * 0.8, rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.8);
                    NenFx.slashAt(level, at, 1.2f, color);
                    // papiers de Kalluto : une nuée de feuilles dans le cône, cible ou pas
                    if (sprite == NenFx.Sprite.PAPER) for (int i = 0; i < 4; i++) {
                        Vec3 p = caster.getEyePosition().add(caster.getLookAngle().scale(1.5 + rnd.nextDouble() * reach * 0.8))
                                .add(rnd.nextGaussian() * 1.2, rnd.nextGaussian() * 0.6, rnd.nextGaussian() * 1.2);
                        NenFx.sprite(level, sprite, null, p, 0.9f, color, interval * 2 + 10);
                    }
                    sound(caster, sound);
                });
            }
            return true;
        }
    }

    // ---------- Bombe à retardement ----------

    /** Marque la cible devant soi ; après {@code delay} ticks, elle explose (compte à rebours de Genthru). */
    public static class TimeBomb extends Technique {
        final double range, radius;
        final int delay;
        final float damage;
        final int color;

        TimeBomb(String id, NenType category, int cost, int cooldown, double range, int delay, float damage, double radius, int color) {
            super(id, category, cost, cooldown);
            this.range = range;
            this.delay = delay;
            this.damage = damage;
            this.radius = radius;
            this.color = color;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.target",
                    String.format(java.util.Locale.ROOT, "%.1f", range));
        }

        @Override
        public double aiRange() {
            return range;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity target = Targeting.inFront(caster, range, 0.92);
            if (target == null) return false;
            NenFx.impactOn(caster, target, 0.8f, color);
            // bombe dessinée collée à la cible, chiffres du compte à rebours
            if (caster.level() instanceof ServerLevel lv) NenFx.sprite(lv, NenFx.Sprite.BOMB, target, target.position(), 1.5f, color, delay);
            // marque de la bombe : un exorciste peut la retirer avant l'explosion
            target.getPersistentData().putLong(com.evandouard.hxh.nen.Exorcism.BOMB, caster.level().getGameTime() + delay);
            int seconds = Math.max(1, delay / 20);
            for (int s = seconds; s >= 1; s--) {
                final int left = s;
                NenTasks.later((seconds - s) * 20, () -> {
                    if (!target.isAlive() || !target.getPersistentData().contains(com.evandouard.hxh.nen.Exorcism.BOMB)) return;
                    NenFx.flare(target, 0.6f + 0.3f * (seconds - left), color);
                    if (caster instanceof Player p) p.displayClientMessage(net.minecraft.network.chat.Component.literal(left + "…")
                            .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD), true);
                });
            }
            NenTasks.later(delay, () -> {
                if (!target.isAlive() || !(target.level() instanceof ServerLevel level)) return;
                if (!target.getPersistentData().contains(com.evandouard.hxh.nen.Exorcism.BOMB)) return; // désamorcée
                target.getPersistentData().remove(com.evandouard.hxh.nen.Exorcism.BOMB);
                Vec3 at = target.getBoundingBox().getCenter();
                NenFx.impactAt(level, at, 2.4f, color);
                NenFx.shockwaveAt(level, target.position().add(0, 0.2, 0), (float) radius, color);
                for (LivingEntity t : enemiesAround(caster, at, radius)) hurt(caster, t, damage * power);
                level.playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.PLAYERS, 1f, 1.1f);
            });
            return true;
        }
    }

    /** Effet sans particules, durée fixe (passifs). */
    static MobEffectInstance quiet(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amp) {
        return new MobEffectInstance(effect, ticks, amp, false, false, true);
    }
}
