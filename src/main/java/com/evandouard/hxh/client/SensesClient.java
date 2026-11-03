package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.nen.NenSenses;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Ce que le Gyo et l'En font voir, côté client uniquement (chaque joueur voit selon ses propres sens).
 * Les entités repérées brillent (contour visible à travers les murs).
 */
public final class SensesClient {
    private static final Set<Entity> GLOWING = new HashSet<>();

    private SensesClient() {}

    /** Le joueur local a-t-il le Gyo actif ? */
    public static boolean localGyo() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getData(ModAttachments.HUNTER).gyo();
    }

    public static void tick(Minecraft mc) {
        HunterData me = mc.player.getData(ModAttachments.HUNTER);
        Set<Entity> wanted = new HashSet<>();
        double enRadius = me.en() ? NenSenses.enRadius(me) : 0;
        int gyo = me.gyo() ? me.masteryLevel(Mastery.GYO) : -1;

        if (gyo >= 0 || enRadius > 0) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == mc.player || !(e instanceof LivingEntity living)) continue;
                NenMode mode = living.hasData(ModAttachments.HUNTER) ? living.getData(ModAttachments.HUNTER).mode() : NenMode.NONE;
                boolean zetsu = mode == NenMode.ZETSU;
                if (gyo >= NenSenses.GYO_SEE_INVISIBLE && living.isInvisible()) wanted.add(e);
                if (gyo >= NenSenses.GYO_SEE_ZETSU && zetsu) wanted.add(e);
                // L'En sent tout ce qui a une aura dans la sphère (le Zetsu y échappe)
                if (enRadius > 0 && !zetsu && living.distanceTo(mc.player) <= enRadius) wanted.add(e);
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

        // Sphère d'En visible autour de chaque joueur qui l'utilise (comme Pitou autour du palais)
        if (mc.level.getGameTime() % 5 == 0) {
            for (Player p : mc.level.players()) {
                if (!p.hasData(ModAttachments.HUNTER)) continue;
                HunterData d = p.getData(ModAttachments.HUNTER);
                if (!d.en()) continue;
                double r = NenSenses.enRadius(d);
                int rgb = d.auraColor();
                DustParticleOptions dust = new DustParticleOptions(new Vector3f(
                        ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), 1.0f);
                int count = (int) Math.min(120, 12 + r * 6);
                for (int i = 0; i < count; i++) {
                    double u = p.getRandom().nextDouble() * 2 - 1;
                    double theta = p.getRandom().nextDouble() * Math.PI * 2;
                    double s = Math.sqrt(1 - u * u);
                    double y = p.getY() + p.getBbHeight() * 0.5 + r * u;
                    if (y < p.getY() - 0.5) continue; // dôme : rien sous les pieds
                    mc.level.addParticle(dust, p.getX() + r * s * Math.cos(theta), y, p.getZ() + r * s * Math.sin(theta), 0, 0, 0);
                }
            }
        }
    }
}
