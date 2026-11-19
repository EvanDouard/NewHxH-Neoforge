package com.evandouard.hxh.combat;

import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Chute : les dégâts sont réduits et fondent avec la Volonté (plus aucun au niveau maximum) ; l'atterrissage
 * marque le sol, d'autant plus que la chute était haute (éclats, onde de choc, secousse, puis cratère).
 */
public final class FallImpact {
    /** Part des dégâts de chute vanilla subie par un chasseur sans Volonté. */
    public static final float BASE_FACTOR = 0.6f;
    /** Hauteurs (blocs) à partir desquelles le sol réagit. */
    public static final float DEBRIS_FROM = 4f, SHOCK_FROM = 8f, QUAKE_FROM = 14f, CRATER_FROM = 20f;

    private FallImpact() {}

    /** Part des dégâts de chute subie : {@code BASE × (1 − Volonté / max)} ; 0 à la Volonté maximale. */
    public static float damageFactor(int willpower) {
        float w = Math.max(0, Math.min(Stat.MAX_LEVEL, willpower)) / (float) Stat.MAX_LEVEL;
        return BASE_FACTOR * (1f - w);
    }

    public static void onFall(LivingFallEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) return;
        float distance = event.getDistance();
        if (distance < 3f) return;
        if (e.hasData(com.evandouard.hxh.data.ModAttachments.HUNTER)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * damageFactor(HunterStats.get(e).level(Stat.WILLPOWER)));
        }
        impact(level, e, distance);
    }

    /** Marque le sol sous {@code e} selon la hauteur de chute. */
    public static void impact(ServerLevel level, LivingEntity e, float distance) {
        if (distance < DEBRIS_FROM) return;
        BlockPos ground = BlockPos.containing(e.getX(), e.getY() - 0.2, e.getZ());
        BlockState s = level.getBlockState(ground);
        if (s.isAir()) return;
        Vec3 at = new Vec3(e.getX(), ground.getY() + 1.0, e.getZ());
        NenFx.debris(level, at, Math.min(24, 6 + (int) distance), s.getMapColor(level, ground).col);
        if (distance >= SHOCK_FROM) NenFx.shockwaveAt(level, at, Math.min(6f, distance / 3f), NenFx.WHITE);
        if (distance >= QUAKE_FROM) NenFx.quake(level, at, Math.min(10f, distance / 2f));
        if (distance >= CRATER_FROM && Impacts.destructive(level)) {
            Impacts.bowl(level, at, Math.min(Impacts.BOWL_RADIUS, 1f + (distance - CRATER_FROM) / 10f), e);
        }
    }
}
