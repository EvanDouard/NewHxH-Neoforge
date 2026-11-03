package com.evandouard.hxh.combat;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.effect.ModEffects;
import com.evandouard.hxh.nen.Targeting;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Combat au corps à corps, pour joueurs et mobs :
 * <ul>
 *     <li>combos : enchaîner les coups sur la même cible (+5 % par coup, 5e coup = coup final qui projette) ;</li>
 *     <li>coup de pied (appui court sur la touche Frappe) : gros recul, petite recharge ;</li>
 *     <li>frappe chargée (maintien) : une barre monte et descend, relâcher au sommet = frappe parfaite,
 *         trop bas = frappe ratée ;</li>
 *     <li>peur : face à un ennemi bien plus puissant qui déploie son aura (Ren, Ken), on est ralenti et affaibli.</li>
 * </ul>
 */
public final class Combat {
    public static final int COMBO_WINDOW_TICKS = 24;
    public static final int COMBO_MAX = 5;
    public static final int KICK_COOLDOWN_TICKS = 30;
    /** Maintien plus court que ça : c'est un coup de pied. */
    public static final int CHARGE_MIN_TICKS = 5;
    /** Durée d'un aller-retour de la barre de frappe chargée. */
    public static final int CHARGE_PERIOD_TICKS = 30;
    public static final float CHARGE_FAIL_BELOW = 0.3f;
    public static final float CHARGE_PERFECT_FROM = 0.85f;
    /** Écart de puissance (niveau Hunter + niveau de Nen) qui déclenche la peur. */
    public static final int FEAR_POWER_GAP = 25;

    private record Combo(int targetId, int count, long lastTick) {}

    private static final Map<LivingEntity, Combo> COMBOS = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> KICK_READY = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> CHARGE_START = new WeakHashMap<>();

    private Combat() {}

    // ---------- Combos ----------

    /**
     * Un coup au corps à corps vient de toucher : met à jour le combo et renvoie le multiplicateur de dégâts.
     * Le 5e coup est un coup final : recul supplémentaire, puis le combo repart de zéro.
     */
    public static float onMeleeHit(LivingEntity attacker, LivingEntity victim) {
        long now = attacker.level().getGameTime();
        Combo previous = COMBOS.get(attacker);
        int count = previous != null && previous.targetId() == victim.getId() && now - previous.lastTick() <= COMBO_WINDOW_TICKS
                ? Math.min(previous.count() + 1, COMBO_MAX) : 1;
        float factor = 1f + 0.05f * (count - 1);
        if (count >= COMBO_MAX) {
            victim.knockback(1.2, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
            if (attacker instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.combo_finisher"), true);
            COMBOS.remove(attacker);
        } else {
            COMBOS.put(attacker, new Combo(victim.getId(), count, now));
            if (count >= 2 && attacker instanceof Player p) {
                p.displayClientMessage(Component.translatable("message.hxh.combo", count), true);
            }
        }
        return factor;
    }

    public static int comboCount(LivingEntity attacker) {
        Combo c = COMBOS.get(attacker);
        return c == null ? 0 : c.count();
    }

    // ---------- Coup de pied et frappe chargée ----------

    private static float baseAttack(LivingEntity entity) {
        AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        return attack != null ? (float) attack.getValue() : 1f;
    }

    private static DamageSource meleeSource(LivingEntity attacker) {
        return attacker instanceof Player p ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker);
    }

    /** @return null si le coup de pied part, sinon la clé du refus */
    public static String kick(LivingEntity attacker) {
        long now = attacker.level().getGameTime();
        Long ready = KICK_READY.get(attacker);
        if (ready != null && now < ready) return "message.hxh.kick_cooldown";
        KICK_READY.put(attacker, now + KICK_COOLDOWN_TICKS);
        attacker.swing(InteractionHand.OFF_HAND, true);
        LivingEntity target = Targeting.inFront(attacker, 3.0, 0.8);
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.8f);
        if (target == null) return null; // coup dans le vide : il part quand même
        float strength = attacker.hasData(ModAttachments.HUNTER) ? HunterStats.get(attacker).level(Stat.STRENGTH) : 0;
        target.invulnerableTime = 0;
        target.hurt(meleeSource(attacker), baseAttack(attacker) * 0.7f + 1f);
        double push = 1.6 + strength * 0.01;
        target.knockback(push, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        target.setDeltaMovement(target.getDeltaMovement().add(0, 0.25, 0));
        if (attacker.level() instanceof ServerLevel server) {
            Vec3 c = target.getBoundingBox().getCenter();
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        }
        return null;
    }

    /** Valeur de la barre (0 à 1) après {@code heldTicks} de maintien : elle monte puis redescend en boucle. */
    public static float chargeValue(long heldTicks) {
        float phase = (heldTicks % CHARGE_PERIOD_TICKS) / (float) CHARGE_PERIOD_TICKS;
        return phase < 0.5f ? phase * 2f : (1f - phase) * 2f;
    }

    public static void startStrike(LivingEntity attacker) {
        CHARGE_START.put(attacker, attacker.level().getGameTime());
    }

    /** Touche relâchée : coup de pied si l'appui était court, sinon frappe chargée selon la barre. */
    public static void releaseStrike(LivingEntity attacker) {
        Long start = CHARGE_START.remove(attacker);
        if (start == null) return;
        long held = attacker.level().getGameTime() - start;
        if (held < CHARGE_MIN_TICKS) {
            String refusal = kick(attacker);
            if (refusal != null && attacker instanceof Player p) p.displayClientMessage(Component.translatable(refusal), true);
            return;
        }
        chargedStrike(attacker, chargeValue(held));
    }

    /**
     * Frappe chargée. Sous {@link #CHARGE_FAIL_BELOW} : ratée (on trébuche). Sinon dégâts × (1 + 2 × barre),
     * et à partir de {@link #CHARGE_PERFECT_FROM} : frappe parfaite qui projette.
     * @return les dégâts infligés (0 si ratée ou sans cible)
     */
    public static float chargedStrike(LivingEntity attacker, float value) {
        attacker.swing(InteractionHand.MAIN_HAND, true);
        if (value < CHARGE_FAIL_BELOW) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
            if (attacker instanceof Player p) p.displayClientMessage(Component.translatable("message.hxh.strike_failed"), true);
            return 0f;
        }
        boolean perfect = value >= CHARGE_PERFECT_FROM;
        if (attacker instanceof Player p) {
            p.displayClientMessage(Component.translatable(perfect ? "message.hxh.strike_perfect" : "message.hxh.strike_good"), true);
        }
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                perfect ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1f);
        LivingEntity target = Targeting.inFront(attacker, 3.5, 0.85);
        if (target == null) return 0f;
        float damage = baseAttack(attacker) * (1f + 2f * value);
        target.invulnerableTime = 0;
        target.hurt(meleeSource(attacker), damage);
        if (perfect) {
            target.knockback(1.5, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            if (attacker.level() instanceof ServerLevel server) {
                Vec3 c = target.getBoundingBox().getCenter();
                server.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 25, 0.3, 0.4, 0.3, 0.3);
            }
        }
        return damage;
    }

    // ---------- Peur ----------

    /** Puissance globale d'une entité : niveau Hunter + niveau de Nen. */
    public static int power(LivingEntity e) {
        if (!e.hasData(ModAttachments.HUNTER)) return 0;
        HunterData d = HunterStats.get(e);
        return d.hunterLevel() + (d.nenUnlocked() ? d.nenLevel() : 0);
    }

    /** Chaque seconde : un ennemi bien plus puissant qui déploie son aura (Ren, Ken) à 12 blocs fait peur. */
    public static void tickFear(LivingEntity entity) {
        if (entity.tickCount % 20 == 0) applyFearIfThreatened(entity);
    }

    /** @return true si l'entité a peur (un ennemi bien plus puissant déploie son aura à moins de 12 blocs) */
    public static boolean applyFearIfThreatened(LivingEntity entity) {
        int mine = power(entity);
        for (LivingEntity other : Targeting.around(entity, 12)) {
            if (!other.hasData(ModAttachments.HUNTER)) continue;
            HunterData d = HunterStats.get(other);
            boolean flaring = d.nenUnlocked() && (d.mode() == NenMode.REN || d.mode() == NenMode.KEN);
            if (flaring && power(other) - mine >= FEAR_POWER_GAP) {
                entity.addEffect(new MobEffectInstance(ModEffects.FEAR, 60, 0, false, false, true));
                return true;
            }
        }
        return false;
    }
}
