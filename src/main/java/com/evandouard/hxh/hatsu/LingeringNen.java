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

    private record Doll(LivingEntity caster, LivingEntity patient, long until, float perPulse) {}

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
        DOLLS.add(new Doll(caster, patient, caster.level().getGameTime() + ticks, total / Math.max(1, ticks / 10)));
        NenFx.doll(caster, patient, ticks);
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
            if (!d.caster.isAlive() || !d.patient.isAlive() || now >= d.until) {
                di.remove();
                continue;
            }
            // Pitou ne bouge pas tant que la poupée opère
            d.caster.setDeltaMovement(0, Math.min(0, d.caster.getDeltaMovement().y), 0);
            d.caster.hurtMarked = true;
            if (now % 10 == 0) d.patient.heal(d.perPulse);
        }
    }
}
