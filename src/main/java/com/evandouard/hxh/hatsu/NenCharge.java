package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Techniques qu'on charge en maintenant la touche (Jajanken : Pierre). Pendant la charge on est figé sur place et
 * l'aura part en continu ; ce qu'on y a mis fait la force du coup. On relâche pour lancer ; à court d'aura ou au
 * maximum, le coup part tout seul. Un mob relâche de lui-même au bout d'un moment.
 */
public final class NenCharge {
    private static final ResourceLocation ROOT_SPEED = HxHMod.id("charge_root_speed");
    private static final ResourceLocation ROOT_JUMP = HxHMod.id("charge_root_jump");
    /** Un mob relâche au bout de ce temps. */
    static final int MOB_RELEASE_TICKS = 35;

    private record Charge(Techniques.ChargedOrb technique, float power, long start, float[] spent) {}

    private static final Map<LivingEntity, Charge> CHARGING = new WeakHashMap<>();

    private NenCharge() {}

    public static boolean isCharging(LivingEntity entity) {
        return CHARGING.containsKey(entity);
    }

    /** Aura mise dans la charge en cours (0 sans charge). */
    public static float spent(LivingEntity entity) {
        Charge c = CHARGING.get(entity);
        return c == null ? 0 : c.spent()[0];
    }

    static void start(LivingEntity entity, Techniques.ChargedOrb technique, float power) {
        if (CHARGING.containsKey(entity)) return;
        CHARGING.put(entity, new Charge(technique, power, entity.level().getGameTime(), new float[]{0}));
        root(entity, true);
        NenFx.chargeOrb(entity, technique.color, technique.maxTicks + 2, technique.epic);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    /** Fige sur place : vitesse et saut à zéro (côté client aussi, par les attributs). */
    private static void root(LivingEntity entity, boolean on) {
        modifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), ROOT_SPEED, on);
        modifier(entity.getAttribute(Attributes.JUMP_STRENGTH), ROOT_JUMP, on);
    }

    private static void modifier(AttributeInstance a, ResourceLocation id, boolean on) {
        if (a == null) return;
        a.removeModifier(id);
        if (on) a.addTransientModifier(new AttributeModifier(id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Chaque tick (serveur) : l'aura part, et le coup part tout seul à court d'aura ou au maximum. */
    public static void tickAll() {
        if (CHARGING.isEmpty()) return;
        List<LivingEntity> release = new ArrayList<>();
        for (Map.Entry<LivingEntity, Charge> e : CHARGING.entrySet()) {
            LivingEntity entity = e.getKey();
            Charge c = e.getValue();
            if (!entity.isAlive()) {
                release.add(entity);
                continue;
            }
            HunterData data = HunterStats.get(entity);
            long held = entity.level().getGameTime() - c.start();
            float drain = c.technique().drainPerTick;
            if (data.aura() >= drain) {
                data.setAura(data.aura() - drain);
                c.spent()[0] += drain;
            }
            entity.setDeltaMovement(0, Math.min(0, entity.getDeltaMovement().y), 0);
            boolean mobDone = !(entity instanceof Player) && held >= MOB_RELEASE_TICKS;
            if (data.aura() < drain || held >= c.technique().maxTicks || mobDone) release.add(entity);
        }
        for (LivingEntity entity : release) release(entity);
    }

    /** Relâche : la boule part, d'autant plus forte qu'on y a mis d'aura. */
    public static void release(LivingEntity entity) {
        Charge c = CHARGING.remove(entity);
        if (c == null) return;
        root(entity, false);
        NenFx.clear(entity);
        if (!entity.isAlive()) return;
        Techniques.ChargedOrb t = c.technique();
        float spent = c.spent()[0];
        HunterData data = HunterStats.get(entity);
        data.setReadyAt(t.id(), entity.level().getGameTime() + t.cooldownTicks());
        com.evandouard.hxh.nen.NenProgress.addNenXp(entity, Math.max(1, Math.round(spent / 5)));
        if (spent < t.minSpent && entity instanceof Player) {
            // Trop court : le poing se relâche sans rien lancer
            if (entity instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.charge_too_short"), true);
            return;
        }
        // Ryu poussé vers l'attaque : la boule est plus grosse et frappe plus fort (×1,5 à 100 %)
        float ryu = com.evandouard.hxh.nen.NenDefense.ryuSpecialFactor(entity);
        float damage = (t.baseDamage + spent * t.damagePerAura) * c.power() * ryu;
        NenProjectile.launch(entity, NenProjectile.ORB, spent * c.power() * t.blastScale * ryu, damage,
                t.epic ? 0xFFFFFFFF : t.color);
        Vec3 at = entity.getEyePosition().add(entity.getLookAngle());
        entity.level().playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
        com.evandouard.hxh.combat.CombatAnimations.play(entity, com.evandouard.hxh.combat.CombatAnim.CHARGED_PUNCH);
    }
}
