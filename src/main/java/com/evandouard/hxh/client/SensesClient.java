package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.nen.NenSenses;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Ce que le Gyo et l'En font voir, côté client uniquement (chaque joueur voit selon ses propres sens).
 * Les entités repérées brillent (contour visible à travers les murs).
 */
public final class SensesClient {
    private static final Set<Entity> GLOWING = new HashSet<>();
    /** Couleur du contour des entités senties (couleur d'aura du joueur local), -1 si aucune. */
    private static volatile int senseColor = -1;

    private SensesClient() {}

    /** Le joueur local a-t-il le Gyo actif ? */
    public static boolean localGyo() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getData(ModAttachments.HUNTER).gyo();
    }

    /** Maîtrise du Gyo actif du joueur local, ou -1. */
    public static int localGyoLevel() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? -1 : NenSenses.gyoLevel(mc.player);
    }

    /** L'entité est dans l'En du joueur local (contour, nom et niveau affichés). */
    public static boolean inLocalEn(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || e == mc.player) return false;
        double r = EnDomeClient.effectiveRadius(mc.player);
        if (r <= 0 || !(e instanceof LivingEntity l)) return false;
        boolean zetsu = l.hasData(ModAttachments.HUNTER) && l.getData(ModAttachments.HUNTER).mode() == NenMode.ZETSU;
        return !zetsu && inSphere(l, mc.player, r);
    }

    /** Une partie du corps de l'entité est-elle dans la sphère d'En (et pas seulement son centre) ? */
    static boolean inSphere(LivingEntity l, Entity center, double r) {
        var box = l.getBoundingBox();
        var c = center.position().add(0, center.getBbHeight() / 2, 0);
        double dx = Math.max(box.minX - c.x, Math.max(0, c.x - box.maxX));
        double dy = Math.max(box.minY - c.y, Math.max(0, c.y - box.maxY));
        double dz = Math.max(box.minZ - c.z, Math.max(0, c.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    /** Maîtrise de l'En du joueur local (-1 si l'En est coupé). */
    public static int localEnLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;
        HunterData d = mc.player.getData(ModAttachments.HUNTER);
        return d.en() ? d.masteryLevel(Mastery.EN) : -1;
    }

    /** Couleur de contour d'une entité sentie par l'En ou le Gyo, ou -1 pour laisser Minecraft décider. */
    public static int outlineColor(Entity e) {
        int c = senseColor;
        return c != -1 && GLOWING.contains(e) ? c : -1;
    }

    public static void tick(Minecraft mc) {
        HunterData me = mc.player.getData(ModAttachments.HUNTER);
        senseColor = me.auraColor() & 0xFFFFFF;
        Set<Entity> wanted = new HashSet<>();
        EnDomeClient.tick(mc);
        double enRadius = EnDomeClient.effectiveRadius(mc.player);
        int gyo = me.gyo() ? me.masteryLevel(Mastery.GYO) : -1;

        if (gyo >= 0 || enRadius > 0) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == mc.player || !(e instanceof LivingEntity living)) continue;
                NenMode mode = living.hasData(ModAttachments.HUNTER) ? living.getData(ModAttachments.HUNTER).mode() : NenMode.NONE;
                boolean zetsu = mode == NenMode.ZETSU;
                if (NenSenses.gyoReveals(gyo, living, zetsu)) wanted.add(e);
                // L'En sent tout ce qui vit dans la sphère, mobs ordinaires compris (le Zetsu y échappe)
                if (enRadius > 0 && !zetsu && inSphere(living, mc.player, enRadius)) wanted.add(e);
            }
        }
        for (Entity e : wanted) e.setGlowingTag(true);
        for (Entity e : GLOWING) {
            if (!wanted.contains(e) && !(e instanceof LivingEntity l && l.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING))) {
                e.setGlowingTag(false);
            }
        }
        GLOWING.clear();
        GLOWING.addAll(wanted);
        // Le dôme d'En lui-même est dessiné par EnDomeClient
    }
}
