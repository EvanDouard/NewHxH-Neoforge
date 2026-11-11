package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Coup qu'on charge en maintenant la touche (Big Bang Impact d'Uvogin…) : la charge monte de 0 à 1 en
 * {@link #chargeTicks} ticks, le coup part au relâchement (ou tout seul au maximum), d'autant plus fort et destructeur
 * que la charge était pleine. Un mob relâche de lui-même. La recharge part au relâchement.
 */
public abstract class HeldStrike extends Technique {
    private record Hold(HeldStrike technique, float power, long start) {}

    private static final Map<LivingEntity, Hold> HOLDING = new WeakHashMap<>();
    final int chargeTicks;
    final int color;

    protected HeldStrike(String id, NenType category, int cost, int cooldown, int chargeTicks, int color) {
        super(id, category, cost, cooldown);
        this.chargeTicks = chargeTicks;
        this.color = color;
    }

    @Override
    public boolean isCharged() {
        return true;
    }

    /** Le coup part, avec une charge de 0 (relâché tout de suite) à 1 (pleine). */
    protected abstract void strike(LivingEntity caster, float power, float charge);

    @Override
    public boolean perform(LivingEntity caster, float power) {
        if (HOLDING.containsKey(caster)) return true;
        HOLDING.put(caster, new Hold(this, power, caster.level().getGameTime()));
        NenFx.flare(caster, 1f, color);
        return true;
    }

    public static boolean isHolding(LivingEntity e) {
        return HOLDING.containsKey(e);
    }

    /** Charge en cours (0 à 1), ou -1 sans charge. */
    public static float charge(LivingEntity e) {
        Hold h = HOLDING.get(e);
        if (h == null) return -1;
        return Math.min(1f, (e.level().getGameTime() - h.start()) / (float) h.technique().chargeTicks);
    }

    public static void tickAll() {
        if (HOLDING.isEmpty()) return;
        List<LivingEntity> done = new ArrayList<>();
        for (Map.Entry<LivingEntity, Hold> e : HOLDING.entrySet()) {
            LivingEntity caster = e.getKey();
            Hold h = e.getValue();
            long held = caster.level().getGameTime() - h.start();
            if (!caster.isAlive()) {
                done.add(caster);
                continue;
            }
            // l'aura monte dans le poing : une lueur à chaque palier
            if (held % 10 == 0) NenFx.flare(caster, 0.6f + 0.8f * Math.min(1f, held / (float) h.technique().chargeTicks), h.technique().color);
            boolean mobDone = !(caster instanceof Player) && held >= h.technique().chargeTicks * 0.7f;
            if (held >= h.technique().chargeTicks + 10 || mobDone) done.add(caster);
        }
        for (LivingEntity c : done) release(c);
    }

    /** Touche relâchée : le coup part. */
    public static void release(LivingEntity caster) {
        float charge = charge(caster);
        Hold h = HOLDING.remove(caster);
        if (h == null || !caster.isAlive()) return;
        HunterStats.get(caster).setReadyAt(h.technique().id(), caster.level().getGameTime() + h.technique().cooldownTicks());
        h.technique().strike(caster, h.power(), Math.max(0f, charge));
    }
}
