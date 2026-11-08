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
 *     <li>coup de pied (appui court sur la touche Frappe) : petite recharge ;</li>
 *     <li>frappe chargée (maintien) : une barre monte et descend, relâcher au sommet = frappe parfaite,
 *         trop bas = frappe ratée ;</li>
 *     <li>peur : rien qu'en regardant une entité bien plus puissante (le loup chimère devant le Roi), ou quand elle
 *         déploie son aura (Ren, Ken) même sans la voir, on est ralenti et la vue se dézoome (panique) ; un mob
 *         terrifié recule.</li>
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
    /** Écart de puissance (niveau Hunter + niveau de Nen) qui fait peur quand le plus fort déploie son aura (Ren, Ken). */
    public static final int FEAR_POWER_GAP = 25;
    /** Écart qui fait peur rien qu'à la vue (présence écrasante, aura au repos). */
    public static final int FEAR_PRESENCE_GAP = 40;
    /** Portée de la peur par l'aura déployée (pas besoin de voir), et par la seule présence (il faut la regarder). */
    public static final double FEAR_AURA_RANGE = 16, FEAR_PRESENCE_RANGE = 10;
    /** Écarts à partir desquels la peur s'aggrave (niveau II : on recule ; niveau III : quasi paralysé). */
    public static final int FEAR_STRONG_GAP = 60, FEAR_TERROR_GAP = 100;

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
        // Le coup de Minecraft (clic main vide) n'a plus d'animation : l'attaque basique est une capacité du menu.
        // Les mobs, eux, gardent leurs poings animés.
        if (!(attacker instanceof Player)) CombatAnimations.punch(attacker);
        long now = attacker.level().getGameTime();
        Combo previous = COMBOS.get(attacker);
        int count = previous != null && previous.targetId() == victim.getId() && now - previous.lastTick() <= COMBO_WINDOW_TICKS
                ? Math.min(previous.count() + 1, COMBO_MAX) : 1;
        float factor = 1f + 0.05f * (count - 1);
        if (count >= COMBO_MAX) {
            victim.knockback(1.2, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
            com.evandouard.hxh.network.Hud.callout(attacker, Component.translatable("message.hxh.combo_finisher"),
                    com.evandouard.hxh.network.Hud.RED, true);
            COMBOS.remove(attacker);
        } else {
            COMBOS.put(attacker, new Combo(victim.getId(), count, now));
            if (count >= 2) {
                com.evandouard.hxh.network.Hud.callout(attacker, Component.translatable("message.hxh.combo", count),
                        0xFFFFFFFF, false);
            }
        }
        return factor;
    }

    public static int comboCount(LivingEntity attacker) {
        Combo c = COMBOS.get(attacker);
        return c == null ? 0 : c.count();
    }

    // ---------- Coup de pied et frappe chargée ----------

    /** Projection d'un coup de pied ou d'une frappe chargée. */
    public enum Projection { NONE, LIGHT, HEAVY }

    /** Charge du coup : pas chargé (coup de pied), chargé hors de la meilleure zone, chargé dans la meilleure zone. */
    public enum Charge { NONE, GOOD, PERFECT }

    /** « Fort » : Force au moins égale au seuil de la config ({@code combat.strongFromStrength}). */
    public static boolean isStrong(LivingEntity attacker) {
        return attacker.hasData(ModAttachments.HUNTER)
                && HunterStats.get(attacker).level(Stat.STRENGTH) >= com.evandouard.hxh.HxHConfig.COMBAT_STRONG_FROM.get();
    }

    /**
     * Tableau des projections :
     * <pre>
     *                     pas fort   fort
     * pas chargé          aucune     légère
     * chargé (bon)        aucune     légère
     * chargé (parfait)    légère     grosse
     * </pre>
     */
    public static Projection projection(boolean strong, Charge charge) {
        if (charge == Charge.PERFECT) return strong ? Projection.HEAVY : Projection.LIGHT;
        return strong ? Projection.LIGHT : Projection.NONE;
    }

    /** Applique la projection : légère = petit recul, grosse = envol (un peu plus fort avec la Force). */
    private static void project(LivingEntity attacker, LivingEntity target, Projection projection) {
        if (projection == Projection.NONE) return;
        double strength = attacker.hasData(ModAttachments.HUNTER) ? HunterStats.get(attacker).level(Stat.STRENGTH) : 0;
        double push = projection == Projection.HEAVY ? 2.0 + strength * 0.015 : 0.8;
        target.knockback(push, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        if (projection == Projection.HEAVY) {
            double lift = 0.35 * (1.0 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
            if (lift > 0) target.setDeltaMovement(target.getDeltaMovement().add(0, lift, 0));
        }
        target.hurtMarked = true;
    }

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
        kickNow(attacker);
        return null;
    }

    /** Le coup de pied lui-même (la recharge est gérée par la technique « coup de pied »). @return la cible ou null */
    public static LivingEntity kickNow(LivingEntity attacker) {
        CombatAnimations.play(attacker, CombatAnim.KICK);
        LivingEntity target = Targeting.inFront(attacker, 3.0, 0.8);
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.8f);
        if (target == null) return null; // coup dans le vide : il part quand même
        target.invulnerableTime = 0;
        target.hurt(meleeSource(attacker), baseAttack(attacker) * 0.7f + 1f);
        project(attacker, target, projection(isStrong(attacker), Charge.NONE));
        com.evandouard.hxh.fx.NenFx.impactOn(attacker, target, 1.2f, com.evandouard.hxh.fx.NenFx.WHITE);
        return target;
    }

    /** Attaque basique : un poing (droit puis gauche) sur ce qui est devant. @return la cible ou null */
    public static LivingEntity basicPunch(LivingEntity attacker) {
        CombatAnim anim = CombatAnimations.punchAnyway(attacker);
        LivingEntity target = Targeting.inFront(attacker, 3.0, 0.8);
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                target != null ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.9f, 1.1f);
        if (target == null) return null;
        target.invulnerableTime = 0;
        target.hurt(meleeSource(attacker), baseAttack(attacker));
        target.knockback(0.3, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        com.evandouard.hxh.fx.NenFx.impactOn(attacker, target, 0.8f, com.evandouard.hxh.fx.NenFx.WHITE);
        return target;
    }

    /** Relâche une frappe chargée par la technique (la barre décide : ratée, chargée ou parfaite). */
    public static boolean releaseChargedTechnique(LivingEntity attacker) {
        Long start = CHARGE_START.remove(attacker);
        if (start == null) return false;
        chargedStrike(attacker, chargeValue(attacker.level().getGameTime() - start));
        return true;
    }

    /** Valeur de la barre (0 à 1) après {@code heldTicks} de maintien : elle monte puis redescend en boucle. */
    public static float chargeValue(long heldTicks) {
        float phase = (heldTicks % CHARGE_PERIOD_TICKS) / (float) CHARGE_PERIOD_TICKS;
        return phase < 0.5f ? phase * 2f : (1f - phase) * 2f;
    }

    public static void startStrike(LivingEntity attacker) {
        CHARGE_START.put(attacker, attacker.level().getGameTime());
        CombatAnimations.play(attacker, CombatAnim.CHARGE); // le bras ne recule qu'après un appui « long »
    }

    /** Touche Frappe maintenue (frappe en cours d'armement). */
    public static boolean isCharging(LivingEntity attacker) {
        return CHARGE_START.containsKey(attacker);
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
     * Frappe chargée. Sous {@link #CHARGE_FAIL_BELOW} : ratée (on trébuche). Sinon dégâts × (1 + 2 × barre).
     * La projection suit {@link #projection} : à partir de {@link #CHARGE_PERFECT_FROM}, la frappe est parfaite.
     * @return les dégâts infligés (0 si ratée ou sans cible)
     */
    public static float chargedStrike(LivingEntity attacker, float value) {
        if (value < CHARGE_FAIL_BELOW) {
            CombatAnimations.play(attacker, CombatAnim.STOP);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false, true));
            com.evandouard.hxh.network.Hud.callout(attacker, Component.translatable("message.hxh.strike_failed"), 0xFF9A9A9A, false);
            return 0f;
        }
        boolean perfect = value >= CHARGE_PERFECT_FROM;
        CombatAnimations.play(attacker, CombatAnim.CHARGED_PUNCH);
        if (attacker instanceof Player p) {
            com.evandouard.hxh.network.Hud.callout(p, Component.translatable(perfect ? "message.hxh.strike_perfect" : "message.hxh.strike_good"),
                    perfect ? com.evandouard.hxh.network.Hud.GOLD : 0xFFFFFFFF, perfect);
        }
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                perfect ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1f);
        LivingEntity target = Targeting.inFront(attacker, 3.5, 0.85);
        if (target == null) return 0f;
        float damage = baseAttack(attacker) * (1f + 2f * value);
        target.invulnerableTime = 0;
        target.hurt(meleeSource(attacker), damage);
        project(attacker, target, projection(isStrong(attacker), perfect ? Charge.PERFECT : Charge.GOOD));
        // Étoile d'impact : dorée et grosse pour une frappe parfaite, avec onde au sol
        com.evandouard.hxh.fx.NenFx.impactOn(attacker, target, perfect ? 2.4f : 1.6f,
                perfect ? com.evandouard.hxh.fx.NenFx.GOLD : com.evandouard.hxh.fx.NenFx.WHITE);
        if (perfect) com.evandouard.hxh.fx.NenFx.shockwave(target, 2.5f, com.evandouard.hxh.fx.NenFx.GOLD);
        return damage;
    }

    // ---------- Peur ----------

    /** Puissance globale d'une entité : niveau Hunter + niveau de Nen. */
    public static int power(LivingEntity e) {
        if (!e.hasData(ModAttachments.HUNTER)) return 0;
        HunterData d = HunterStats.get(e);
        return d.hunterLevel() + (d.nenUnlocked() ? d.nenLevel() : 0);
    }

    /** Chaque seconde (décalé d'une entité à l'autre) : vérifie si quelque chose de bien plus puissant fait peur. */
    public static void tickFear(LivingEntity entity) {
        if ((entity.tickCount + entity.getId()) % 20 == 0) applyFearIfThreatened(entity);
    }

    /**
     * Source de peur la plus écrasante autour de l'entité, et l'écart de puissance, ou null.
     * <ul>
     *     <li>présence : elle est à moins de 10 blocs, en vue, et on la regarde ; écart ≥ 40 ;</li>
     *     <li>aura déployée (Ren, Ken) : à moins de 16 blocs, même sans la voir ; écart ≥ 25.</li>
     * </ul>
     * En Zetsu ou en In, la présence est masquée : elle ne fait pas peur.
     */
    @org.jetbrains.annotations.Nullable
    public static Map.Entry<LivingEntity, Integer> fearSource(LivingEntity entity) {
        int mine = power(entity);
        LivingEntity best = null;
        int bestGap = 0;
        for (LivingEntity other : Targeting.around(entity, FEAR_AURA_RANGE)) {
            if (!other.hasData(ModAttachments.HUNTER)) continue;
            if (other instanceof Player p && p.isSpectator()) continue;
            HunterData d = HunterStats.get(other);
            NenMode mode = d.nenUnlocked() ? d.mode() : NenMode.NONE;
            if (mode == NenMode.ZETSU || mode == NenMode.IN) continue;
            int gap = power(other) - mine;
            boolean flaring = mode == NenMode.REN || mode == NenMode.KEN;
            boolean scary = flaring && gap >= FEAR_POWER_GAP
                    || gap >= FEAR_PRESENCE_GAP && entity.distanceTo(other) <= FEAR_PRESENCE_RANGE && isLookingAt(entity, other);
            if (scary && gap > bestGap) {
                best = other;
                bestGap = gap;
            }
        }
        return best == null ? null : Map.entry(best, bestGap);
    }

    /** L'entité regarde l'autre (dans un cône devant elle) et la voit (rien entre les deux). */
    public static boolean isLookingAt(LivingEntity viewer, LivingEntity target) {
        Vec3 look = viewer.getViewVector(1f).normalize();
        Vec3 to = target.getEyePosition().subtract(viewer.getEyePosition());
        double dist = to.length();
        if (dist < 1e-3) return true;
        return look.dot(to.scale(1 / dist)) > 0.6 && viewer.hasLineOfSight(target);
    }

    /** @return true si l'entité a peur (voir {@link #fearSource}) ; applique l'effet Peur, plus fort si l'écart est énorme */
    public static boolean applyFearIfThreatened(LivingEntity entity) {
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (entity instanceof com.evandouard.hxh.npc.NpcEntity) return false; // PNJ de quête : imperturbables
        Map.Entry<LivingEntity, Integer> source = fearSource(entity);
        if (source == null) return false;
        int gap = source.getValue();
        int amplifier = gap >= FEAR_TERROR_GAP ? 2 : gap >= FEAR_STRONG_GAP ? 1 : 0;
        boolean onset = !entity.hasEffect(ModEffects.FEAR);
        entity.addEffect(new MobEffectInstance(ModEffects.FEAR, 60, amplifier, false, false, true));
        LivingEntity scary = source.getKey();
        if (onset && entity instanceof net.minecraft.server.level.ServerPlayer player) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.hxh.fear." + amplifier, scary.getDisplayName()).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
        }
        // Un mob terrifié n'ose plus attaquer et recule
        if (amplifier >= 1 && entity instanceof net.minecraft.world.entity.PathfinderMob mob) {
            if (mob.getTarget() == scary) mob.setTarget(null);
            Vec3 away = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(mob, 10, 5, scary.position());
            if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
        }
        return true;
    }
}
