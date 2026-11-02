package com.evandouard.hxh.nen;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Outils de visée partagés par les joueurs et les mobs.
 */
public final class Targeting {
    private Targeting() {}

    /** Entité vivante la plus proche devant le regard, dans un cône (dot = cos de l'angle max). */
    @Nullable
    public static LivingEntity inFront(LivingEntity source, double range, double minDot) {
        Vec3 eye = source.getEyePosition();
        Vec3 look = source.getLookAngle();
        AABB box = source.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : source.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != source && e.isAlive() && !e.isSpectator())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range + e.getBbWidth() || dist < 1e-4) continue;
            if (to.normalize().dot(look) < minDot) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    /** Toutes les entités vivantes dans un rayon, sauf la source. */
    public static List<LivingEntity> around(LivingEntity source, double radius) {
        return source.level().getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(radius),
                e -> e != source && e.isAlive() && !e.isSpectator() && e.distanceTo(source) <= radius);
    }
}
