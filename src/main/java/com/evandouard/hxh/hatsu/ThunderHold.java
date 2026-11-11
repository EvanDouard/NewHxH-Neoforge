package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Thunderbolt de Killua, maintenu : tant que la touche est tenue, l'électricité frappe ce qui est devant lui toutes les
 * 5 ticks (dégâts, paralysie). Le coût monte avec la durée : quelques points au début, beaucoup au bout de 3 s.
 * Relâché (ou à court d'aura), la recharge part.
 */
public final class ThunderHold {
    private ThunderHold() {}

    /** Portée, tick d'intervalle entre deux décharges, maintien maximal, relâche d'un mob. */
    static final double RANGE = 5;
    static final int PULSE = 5, MAX_TICKS = 100, MOB_RELEASE = 30;

    private record Hold(Technique technique, float power, long start) {}

    private static final Map<LivingEntity, Hold> HOLDING = new WeakHashMap<>();

    public static boolean isHolding(LivingEntity e) {
        return HOLDING.containsKey(e);
    }

    /** Coût par tick après {@code held} ticks de maintien : 0,2 au départ, puis de plus en plus cher. */
    public static float drain(long held) {
        return 0.2f + held * 0.012f;
    }

    static void start(LivingEntity caster, Technique technique, float power) {
        if (HOLDING.containsKey(caster)) return;
        HOLDING.put(caster, new Hold(technique, power, caster.level().getGameTime()));
        NenFx.crackle(caster, NenFx.TRANSMUTER, MAX_TICKS + 4);
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    public static void tickAll() {
        if (HOLDING.isEmpty()) return;
        List<LivingEntity> done = new ArrayList<>();
        for (Map.Entry<LivingEntity, Hold> e : HOLDING.entrySet()) {
            LivingEntity caster = e.getKey();
            Hold h = e.getValue();
            if (!caster.isAlive()) {
                done.add(caster);
                continue;
            }
            long held = caster.level().getGameTime() - h.start();
            HunterData data = HunterStats.get(caster);
            float cost = drain(held);
            if (data.aura() < cost || held >= MAX_TICKS || !(caster instanceof Player) && held >= MOB_RELEASE) {
                done.add(caster);
                continue;
            }
            data.setAura(data.aura() - cost);
            if (held % PULSE == 0) pulse(caster, h.power(), held);
        }
        for (LivingEntity c : done) release(c);
    }

    private static void pulse(LivingEntity caster, float power, long held) {
        LivingEntity target = Targeting.inFront(caster, RANGE, 0.8);
        Vec3 end = target != null ? target.getBoundingBox().getCenter() : caster.getEyePosition().add(caster.getLookAngle().scale(RANGE));
        NenFx.bolt(caster, target, end, NenFx.TRANSMUTER);
        if (target == null) return;
        // de plus en plus fort à mesure qu'on tient
        float dmg = (2f + held * 0.03f) * power;
        Kinds.hurt(caster, target, dmg);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4), caster);
        NenFx.crackle(target, NenFx.TRANSMUTER, 20);
        caster.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    /** Touche relâchée : l'électricité retombe, la recharge part. */
    public static void release(LivingEntity caster) {
        Hold h = HOLDING.remove(caster);
        if (h == null) return;
        NenFx.clear(caster);
        HunterData data = HunterStats.get(caster);
        data.setReadyAt(h.technique().id(), caster.level().getGameTime() + h.technique().cooldownTicks());
        if (caster instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.thunder_released"), true);
    }

    /** Technique « Thunderbolt » maintenu. */
    static final class Technique_ extends Technique {
        Technique_(String id, NenType category, int cost, int cooldown) {
            super(id, category, cost, cooldown);
        }

        @Override
        public boolean isCharged() {
            return true;
        }

        @Override
        public double aiRange() {
            return RANGE;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.hold_bolt", String.format(java.util.Locale.ROOT, "%.0f", RANGE));
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            start(caster, this, power);
            return true;
        }
    }
}
