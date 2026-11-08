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
        // Le dôme d'En lui-même est dessiné par EnDomeClient
    }
}
