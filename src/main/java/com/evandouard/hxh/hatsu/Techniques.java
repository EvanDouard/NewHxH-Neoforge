package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.nen.Targeting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registre des techniques. Les effets sont volontairement simples pour l'instant :
 * les visuels détaillés (aura qui grossit dans le poing, souffle au sol…) viendront ensuite.
 */
public final class Techniques {
    private static final Map<String, Technique> ALL = new LinkedHashMap<>();

    private Techniques() {}

    @Nullable
    public static Technique get(String id) {
        return ALL.get(id);
    }

    public static Map<String, Technique> all() {
        return ALL;
    }

    private static <T extends Technique> T register(T technique) {
        ALL.put(technique.id(), technique);
        return technique;
    }

    // ---------- Gon : Jajanken ----------
    public static final Technique GON_ROCK = register(new Strike("gon_rock", NenType.ENHANCER, 40, 160,
            3.5, 0.85, 9f, 1.6f, List.of(), ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE.value()));
    public static final Technique GON_SCISSORS = register(new Strike("gon_scissors", NenType.TRANSMUTER, 25, 100,
            5, 0.85, 6f, 0.4f, List.of(), ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP));
    public static final Technique GON_PAPER = register(new Strike("gon_paper", NenType.EMITTER, 30, 120,
            16, 0.97, 5f, 0.8f, List.of(), ParticleTypes.END_ROD, SoundEvents.FIREWORK_ROCKET_BLAST));

    // ---------- Killua : électricité ----------
    public static final Technique KILLUA_THUNDERBOLT = register(new Strike("killua_thunderbolt", NenType.TRANSMUTER, 30, 120,
            4, 0.85, 6f, 0.2f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 4)),
            ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_IMPACT));
    public static final Technique KILLUA_GODSPEED = register(new SelfBuff("killua_godspeed", NenType.TRANSMUTER, 50, 600,
            List.of(new Effect(MobEffects.MOVEMENT_SPEED, 200, 2), new Effect(MobEffects.DIG_SPEED, 200, 1)),
            ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER));

    // ---------- Kurapika : chaînes ----------
    public static final Technique KURAPIKA_CHAIN_JAIL = register(new Strike("kurapika_chain_jail", NenType.CONJURER, 45, 400,
            8, 0.95, 1f, 0f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 100, 6), new Effect(MobEffects.WEAKNESS, 100, 1)),
            ParticleTypes.CRIT, SoundEvents.CHAIN_PLACE));
    public static final Technique KURAPIKA_HOLY_CHAIN = register(new SelfHeal("kurapika_holy_chain", NenType.CONJURER, 35, 300,
            8f, ParticleTypes.HAPPY_VILLAGER, SoundEvents.CHAIN_PLACE));

    // ---------- Leorio : émission ----------
    public static final Technique LEORIO_REMOTE_PUNCH = register(new Strike("leorio_remote_punch", NenType.EMITTER, 30, 120,
            12, 0.97, 7f, 1.0f, List.of(), ParticleTypes.CLOUD, SoundEvents.PLAYER_ATTACK_KNOCKBACK));
    public static final Technique LEORIO_SHOCKWAVE = register(new Blast("leorio_shockwave", NenType.EMITTER, 40, 200,
            4, 4f, 1.2f, ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE.value()));

    // ---------- Illumi : aiguilles ----------
    public static final Technique ILLUMI_NEEDLE = register(new Strike("illumi_needle", NenType.MANIPULATOR, 25, 100,
            12, 0.97, 3f, 0f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), new Effect(MobEffects.CONFUSION, 100, 0)),
            ParticleTypes.CRIT, SoundEvents.ARROW_HIT));
    public static final Technique ILLUMI_TRANSFORM = register(new SelfBuff("illumi_transform", NenType.MANIPULATOR, 50, 600,
            List.of(new Effect(MobEffects.DAMAGE_RESISTANCE, 300, 0), new Effect(MobEffects.DAMAGE_BOOST, 300, 0)),
            ParticleTypes.SMOKE, SoundEvents.ZOMBIE_VILLAGER_CURE));

    // ---------- Neferpitou : spécialiste ----------
    public static final Technique PITOU_TERPSICHORA = register(new SelfBuff("pitou_terpsichora", NenType.SPECIALIST, 60, 600,
            List.of(new Effect(MobEffects.DAMAGE_BOOST, 200, 1), new Effect(MobEffects.MOVEMENT_SPEED, 200, 1),
                    new Effect(MobEffects.JUMP, 200, 1)),
            ParticleTypes.SOUL_FIRE_FLAME, SoundEvents.WARDEN_ROAR));
    public static final Technique PITOU_DOCTOR_BLYTHE = register(new SelfHeal("pitou_doctor_blythe", NenType.SPECIALIST, 50, 400,
            12f, ParticleTypes.HEART, SoundEvents.AMETHYST_BLOCK_CHIME));

    // =====================================================================
    // Types de techniques réutilisables
    // =====================================================================

    public record Effect(Holder<MobEffect> effect, int ticks, int amplifier) {
        MobEffectInstance create(float power) {
            return new MobEffectInstance(effect, Math.round(ticks * Math.max(0.5f, power)), amplifier);
        }
    }

    static DamageSource damageFrom(LivingEntity caster) {
        return caster instanceof Player player
                ? caster.damageSources().playerAttack(player)
                : caster.damageSources().mobAttack(caster);
    }

    static void fx(LivingEntity at, ParticleOptions particle, SoundEvent sound, int count) {
        if (at.level() instanceof ServerLevel server) {
            server.sendParticles(particle, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(),
                    count, at.getBbWidth() * 0.4, at.getBbHeight() * 0.3, at.getBbWidth() * 0.4, 0.05);
        }
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, 0.8f, 1f);
    }

    /** Traînée de particules du lanceur à la cible (techniques à distance). */
    static void trail(LivingEntity from, LivingEntity to, ParticleOptions particle) {
        if (!(from.level() instanceof ServerLevel server)) return;
        Vec3 a = from.getEyePosition();
        Vec3 b = to.getBoundingBox().getCenter();
        int steps = (int) Math.max(2, a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            server.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** Technique décrite mais pas encore jouable (personnages en réflexion). */
    public static final class Idea extends Technique {
        public Idea(String id, NenType category) {
            super(id, category, 0, 0);
        }

        @Override
        public boolean implemented() {
            return false;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.idea");
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            return false;
        }
    }

    /** Enregistre une technique-idée (appelé par HatsuKits pour les personnages à venir). */
    static Technique idea(String id, NenType category) {
        return register(new Idea(id, category));
    }

    /** Frappe une cible devant soi (au corps à corps ou à distance). */
    static final class Strike extends Technique {
        private final double range, minDot;
        private final float damage, knockback;
        private final List<Effect> effects;
        private final ParticleOptions particle;
        private final SoundEvent sound;

        Strike(String id, NenType category, int cost, int cooldown, double range, double minDot, float damage,
               float knockback, List<Effect> effects, ParticleOptions particle, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.range = range;
            this.minDot = minDot;
            this.damage = damage;
            this.knockback = knockback;
            this.effects = effects;
            this.particle = particle;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.target",
                    String.format(java.util.Locale.ROOT, "%.1f", range));
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity target = Targeting.inFront(caster, range, minDot);
            if (target == null) return false;
            if (range > 5) trail(caster, target, particle);
            target.invulnerableTime = 0;
            target.hurt(damageFrom(caster), damage * power);
            if (knockback > 0) target.knockback(knockback * power, caster.getX() - target.getX(), caster.getZ() - target.getZ());
            for (Effect e : effects) target.addEffect(e.create(power), caster);
            fx(target, particle, sound, 20);
            return true;
        }
    }

    /** Onde de choc autour du lanceur. */
    static final class Blast extends Technique {
        private final double radius;
        private final float damage, knockback;
        private final ParticleOptions particle;
        private final SoundEvent sound;

        Blast(String id, NenType category, int cost, int cooldown, double radius, float damage, float knockback,
              ParticleOptions particle, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.particle = particle;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.area",
                    String.format(java.util.Locale.ROOT, "%.0f", radius));
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            List<LivingEntity> targets = Targeting.around(caster, radius);
            for (LivingEntity t : targets) {
                t.invulnerableTime = 0;
                t.hurt(damageFrom(caster), damage * power);
                t.knockback(knockback * power, caster.getX() - t.getX(), caster.getZ() - t.getZ());
            }
            fx(caster, particle, sound, 12);
            return true; // l'onde part même sans cible
        }
    }

    /** Renforcement sur soi. */
    static final class SelfBuff extends Technique {
        private final List<Effect> effects;
        private final ParticleOptions particle;
        private final SoundEvent sound;

        SelfBuff(String id, NenType category, int cost, int cooldown, List<Effect> effects,
                 ParticleOptions particle, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.effects = effects;
            this.particle = particle;
            this.sound = sound;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            for (Effect e : effects) caster.addEffect(e.create(power), caster);
            fx(caster, particle, sound, 25);
            return true;
        }
    }

    /** Soin instantané sur soi. */
    static final class SelfHeal extends Technique {
        private final float amount;
        private final ParticleOptions particle;
        private final SoundEvent sound;

        SelfHeal(String id, NenType category, int cost, int cooldown, float amount,
                 ParticleOptions particle, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.amount = amount;
            this.particle = particle;
            this.sound = sound;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            caster.heal(amount * power);
            fx(caster, particle, sound, 10);
            return true;
        }
    }
}
