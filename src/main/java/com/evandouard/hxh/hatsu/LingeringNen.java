package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.fx.NenFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Techniques qui durent après le lancer : Bungee Gum (lien élastique) et Docteur Blythe (soin par la poupée).
 * Tout est côté serveur et marche pour n'importe quelle LivingEntity.
 */
public final class LingeringNen {
    /** Au-delà de cette distance, le Bungee Gum tire la cible vers Hisoka comme un élastique. */
    public static final double GUM_SLACK = 5;
    /** Le lien casse au-delà de cette distance. */
    public static final double GUM_BREAK = 32;

    private record Gum(LivingEntity owner, LivingEntity target, long until) {}

    /** {@code drain} > 0 : poupée maintenue (aura par tick, recharge de {@code technique} à la fin). */
    private record Doll(LivingEntity caster, LivingEntity patient, long until, float perPulse, float drain,
                        @org.jetbrains.annotations.Nullable Technique technique) {}

    private static final List<Gum> GUMS = new ArrayList<>();
    private static final List<Doll> DOLLS = new ArrayList<>();

    private LingeringNen() {}

    // ---------------------------------------------------------------- Bungee Gum

    public static LivingEntity gumTarget(LivingEntity owner) {
        for (Gum g : GUMS) if (g.owner == owner) return g.target;
        return null;
    }

    public static void attachGum(LivingEntity owner, LivingEntity target, int ticks) {
        cutGum(owner);
        GUMS.add(new Gum(owner, target, owner.level().getGameTime() + ticks));
        NenFx.gumLink(owner, target, ticks);
    }

    /** Coupe le lien (sans effet sur la cible). */
    public static void cutGum(LivingEntity owner) {
        if (GUMS.removeIf(g -> g.owner == owner)) NenFx.clear(owner);
    }

    /** « Il a les propriétés du caoutchouc » : le lien se contracte d'un coup et ramène la cible contre Hisoka. */
    public static boolean snapGum(LivingEntity owner, float power) {
        LivingEntity t = gumTarget(owner);
        if (t == null) return false;
        cutGum(owner);
        Vec3 to = owner.position().add(0, 0.4, 0).subtract(t.position());
        double d = to.length();
        if (d > 1e-3) {
            Vec3 v = to.normalize().scale(Math.min(2.6, 0.5 + d * 0.22) * power);
            t.setDeltaMovement(v.x, v.y + 0.35, v.z);
            t.hurtMarked = true;
        }
        if (owner.level() instanceof ServerLevel level) NenFx.impactAt(level, t.getBoundingBox().getCenter(), 1.2f, NenFx.HISOKA);
        return true;
    }

    // ---------------------------------------------------------------- Docteur Blythe

    public static boolean isOperating(LivingEntity caster) {
        for (Doll d : DOLLS) if (d.caster == caster) return true;
        return false;
    }

    public static void startDoll(LivingEntity caster, LivingEntity patient, int ticks, float total) {
        DOLLS.removeIf(d -> d.caster == caster);
        DOLLS.add(new Doll(caster, patient, caster.level().getGameTime() + ticks, total / Math.max(1, ticks / 10), 0f, null));
        NenFx.doll(caster, patient, ticks);
    }

    /** Maintien maximal de Docteur Blythe (joueur), et durée pour un mob qui « relâche » tout seul. */
    public static final int DOLL_MAX_HOLD = 600, DOLL_MOB_HOLD = 80;

    /**
     * Docteur Blythe maintenu : la poupée opère tant que la touche est tenue (ou {@link #DOLL_MOB_HOLD} ticks pour un
     * mob), en consommant {@code drain} d'aura par tick ; {@code perPulse} PV rendus toutes les 10 ticks.
     */
    public static void startHeldDoll(LivingEntity caster, LivingEntity patient, Technique technique, float perPulse, float drain) {
        DOLLS.removeIf(d -> d.caster == caster);
        int ticks = caster instanceof net.minecraft.world.entity.player.Player ? DOLL_MAX_HOLD : DOLL_MOB_HOLD;
        DOLLS.add(new Doll(caster, patient, caster.level().getGameTime() + ticks, perPulse, drain, technique));
        NenFx.doll(caster, patient, ticks);
    }

    public static boolean isHoldingDoll(LivingEntity caster) {
        for (Doll d : DOLLS) if (d.caster == caster && d.drain > 0) return true;
        return false;
    }

    /** Touche relâchée (ou plus d'aura) : la poupée disparaît, la recharge part. */
    public static void releaseDoll(LivingEntity caster) {
        Iterator<Doll> it = DOLLS.iterator();
        while (it.hasNext()) {
            Doll d = it.next();
            if (d.caster == caster && d.drain > 0) {
                it.remove();
                endHeld(d);
            }
        }
    }

    private static void endHeld(Doll d) {
        NenFx.clear(d.caster);
        d.caster.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        if (d.technique != null) {
            com.evandouard.hxh.data.HunterData data = com.evandouard.hxh.stats.HunterStats.get(d.caster);
            data.setReadyAt(d.technique.id(), d.caster.level().getGameTime() + d.technique.cooldownFor(data));
        }
    }

    // ---------------------------------------------------------------- tick

    public static void tickAll() {
        Iterator<Gum> gi = GUMS.iterator();
        while (gi.hasNext()) {
            Gum g = gi.next();
            long now = g.owner.level().getGameTime();
            double dist = g.owner.distanceTo(g.target);
            if (!g.owner.isAlive() || !g.target.isAlive() || g.owner.level() != g.target.level()
                    || now >= g.until || dist > GUM_BREAK) {
                gi.remove();
                NenFx.clear(g.owner);
                continue;
            }
            if (dist > GUM_SLACK) {
                Vec3 dir = g.owner.position().subtract(g.target.position()).normalize();
                double pull = Math.min(0.5, 0.06 * (dist - GUM_SLACK));
                g.target.setDeltaMovement(g.target.getDeltaMovement().add(dir.scale(pull)));
                g.target.hurtMarked = true;
            }
        }
        Iterator<Doll> di = DOLLS.iterator();
        while (di.hasNext()) {
            Doll d = di.next();
            long now = d.caster.level().getGameTime();
            boolean dry = false;
            if (d.drain > 0) {
                com.evandouard.hxh.data.HunterData data = com.evandouard.hxh.stats.HunterStats.get(d.caster);
                if (data.aura() < d.drain) dry = true;
                else data.setAura(data.aura() - d.drain);
            }
            if (!d.caster.isAlive() || !d.patient.isAlive() || now >= d.until || dry) {
                di.remove();
                if (d.drain > 0) endHeld(d);
                continue;
            }
            // Pitou ne bouge pas tant que la poupée opère
            d.caster.setDeltaMovement(0, Math.min(0, d.caster.getDeltaMovement().y), 0);
            d.caster.hurtMarked = true;
            if (now % 10 == 0) d.patient.heal(d.perPulse);
        }
    }
}
