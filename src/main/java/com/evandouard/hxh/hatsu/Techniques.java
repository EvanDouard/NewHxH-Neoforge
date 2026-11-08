package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.nen.Targeting;
import net.minecraft.core.Holder;
import com.evandouard.hxh.fx.NenFx;
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
            3.5, 0.85, 9f, 1.6f, List.of(), new Look(Style.IMPACT, NenFx.ENHANCER, 2.2f), SoundEvents.GENERIC_EXPLODE.value()));
    public static final Technique GON_SCISSORS = register(new Strike("gon_scissors", NenType.TRANSMUTER, 25, 100,
            5, 0.85, 6f, 0.4f, List.of(), new Look(Style.SLASH, NenFx.TRANSMUTER, 1.6f), SoundEvents.PLAYER_ATTACK_SWEEP)).requiresNenLevel(5);
    public static final Technique GON_PAPER = register(new Strike("gon_paper", NenType.EMITTER, 30, 120,
            16, 0.97, 5f, 0.8f, List.of(), new Look(Style.ORB, NenFx.EMITTER, 1.4f), SoundEvents.FIREWORK_ROCKET_BLAST)).requiresNenLevel(10);

    // ---------- Killua : électricité ----------
    public static final Technique KILLUA_THUNDERBOLT = register(new Strike("killua_thunderbolt", NenType.TRANSMUTER, 30, 120,
            4, 0.85, 6f, 0.2f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 60, 4)),
            new Look(Style.BOLT, NenFx.TRANSMUTER, 1f), SoundEvents.LIGHTNING_BOLT_IMPACT));
    public static final Technique KILLUA_GODSPEED = register(new SelfBuff("killua_godspeed", NenType.TRANSMUTER, 50, 600,
            List.of(new Effect(MobEffects.MOVEMENT_SPEED, 200, 2), new Effect(MobEffects.DIG_SPEED, 200, 1)),
            new Look(Style.CRACKLE, NenFx.TRANSMUTER, 1f), SoundEvents.LIGHTNING_BOLT_THUNDER)).requiresNenLevel(10);

    // ---------- Kurapika : chaînes ----------
    public static final Technique KURAPIKA_CHAIN_JAIL = register(new Strike("kurapika_chain_jail", NenType.CONJURER, 45, 400,
            8, 0.95, 1f, 0f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 100, 6), new Effect(MobEffects.WEAKNESS, 100, 1)),
            new Look(Style.CHAIN, NenFx.CONJURER, 1f), SoundEvents.CHAIN_PLACE)).requiresNenLevel(5);
    public static final Technique KURAPIKA_HOLY_CHAIN = register(new SelfHeal("kurapika_holy_chain", NenType.CONJURER, 35, 300,
            8f, new Look(Style.HOLY_CHAIN, NenFx.CONJURER, 1f), SoundEvents.CHAIN_PLACE));

    // ---------- Leorio : émission ----------
    public static final Technique LEORIO_REMOTE_PUNCH = register(new Strike("leorio_remote_punch", NenType.EMITTER, 30, 120,
            12, 0.97, 7f, 1.0f, List.of(), new Look(Style.ORB, NenFx.EMITTER, 1.8f), SoundEvents.PLAYER_ATTACK_KNOCKBACK));
    public static final Technique LEORIO_SHOCKWAVE = register(new Blast("leorio_shockwave", NenType.EMITTER, 40, 200,
            4, 4f, 1.2f, new Look(Style.SHOCKWAVE, NenFx.EMITTER, 1f), SoundEvents.GENERIC_EXPLODE.value())).requiresNenLevel(5);

    // ---------- Illumi : aiguilles ----------
    public static final Technique ILLUMI_NEEDLE = register(new Strike("illumi_needle", NenType.MANIPULATOR, 25, 100,
            12, 0.97, 3f, 0f, List.of(new Effect(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), new Effect(MobEffects.CONFUSION, 100, 0)),
            new Look(Style.NEEDLE, NenFx.MANIPULATOR, 1f), SoundEvents.ARROW_HIT));
    public static final Technique ILLUMI_TRANSFORM = register(new SelfBuff("illumi_transform", NenType.MANIPULATOR, 50, 600,
            List.of(new Effect(MobEffects.DAMAGE_RESISTANCE, 300, 0), new Effect(MobEffects.DAMAGE_BOOST, 300, 0)),
            new Look(Style.FLARE, NenFx.DARK, 1f), SoundEvents.ZOMBIE_VILLAGER_CURE)).requiresNenLevel(10);

    // ---------- Neferpitou : spécialiste ----------
    public static final Technique PITOU_TERPSICHORA = register(new SelfBuff("pitou_terpsichora", NenType.SPECIALIST, 60, 600,
            List.of(new Effect(MobEffects.DAMAGE_BOOST, 200, 1), new Effect(MobEffects.MOVEMENT_SPEED, 200, 1),
                    new Effect(MobEffects.JUMP, 200, 1)),
            new Look(Style.FLARE, NenFx.SPECIALIST, 1.4f), SoundEvents.WARDEN_ROAR)).requiresNenLevel(10);
    public static final Technique PITOU_DOCTOR_BLYTHE = register(new SelfHeal("pitou_doctor_blythe", NenType.SPECIALIST, 50, 400,
            12f, new Look(Style.FLARE, NenFx.HEAL, 1.1f), SoundEvents.AMETHYST_BLOCK_CHIME));

    // =====================================================================
    // Types de techniques réutilisables
    // =====================================================================

    public record Effect(Holder<MobEffect> effect, int ticks, int amplifier) {
        MobEffectInstance create(float power) {
            // Sans les bulles d'effet de Minecraft (direction artistique : pas de particules vanilla)
            return new MobEffectInstance(effect, Math.round(ticks * Math.max(0.5f, power)), amplifier, false, false, true);
        }
    }

    static DamageSource damageFrom(LivingEntity caster) {
        return caster instanceof Player player
                ? caster.damageSources().playerAttack(player)
                : caster.damageSources().mobAttack(caster);
    }

    /** Allure d'une technique (direction artistique : formes dessinées, pas de particules de Minecraft). */
    public enum Style { IMPACT, SLASH, ORB, BOLT, CHAIN, NEEDLE, SHOCKWAVE, FLARE, CRACKLE, HOLY_CHAIN }

    /** Allure : style, couleur d'aura, taille. */
    public record Look(Style style, int color, float size) {}

    static void sound(LivingEntity at, SoundEvent sound) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, 0.8f, 1f);
    }

    /** Effet visuel d'une frappe : sur la cible touchée, ou vers le point visé si elle rate. */
    static void showStrike(Look look, LivingEntity caster, @Nullable LivingEntity target, Vec3 end, int holdTicks) {
        Vec3 at = target != null ? target.getBoundingBox().getCenter() : end;
        switch (look.style()) {
            case IMPACT -> {
                NenFx.impact(caster, at, target != null ? look.size() : look.size() * 0.6f, look.color());
                if (target != null && look.size() >= 2f) NenFx.shockwave(target, look.size() * 1.2f, look.color());
            }
            case SLASH -> NenFx.slash(caster, at, look.size(), look.color());
            case ORB -> NenFx.orb(caster, target, end, look.size(), look.color());
            case BOLT -> NenFx.bolt(caster, target, end, look.color());
            case CHAIN -> NenFx.chain(caster, target, end, look.color(), target != null ? holdTicks : 14);
            case NEEDLE -> NenFx.needle(caster, target, end, target != null ? holdTicks : 14);
            default -> NenFx.impact(caster, at, look.size(), look.color());
        }
    }

    /** Effet visuel sur soi (renforcements, soins). */
    static void showSelf(Look look, LivingEntity caster, int holdTicks) {
        switch (look.style()) {
            case CRACKLE -> {
                NenFx.flare(caster, look.size(), look.color());
                NenFx.crackle(caster, look.color(), holdTicks);
            }
            case HOLY_CHAIN -> NenFx.holyChain(caster, look.color(), 50);
            default -> NenFx.flare(caster, look.size(), look.color());
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
        private final Look look;
        private final SoundEvent sound;

        Strike(String id, NenType category, int cost, int cooldown, double range, double minDot, float damage,
               float knockback, List<Effect> effects, Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.range = range;
            this.minDot = minDot;
            this.damage = damage;
            this.knockback = knockback;
            this.effects = effects;
            this.look = look;
            this.sound = sound;
        }

        /** Durée de l'effet le plus long (une chaîne reste enroulée, une aiguille reste plantée, aussi longtemps). */
        private int holdTicks(float power) {
            int t = 20;
            for (Effect e : effects) t = Math.max(t, Math.round(e.ticks() * Math.max(0.5f, power)));
            return t;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.target",
                    String.format(java.util.Locale.ROOT, "%.1f", range));
        }

        @Override
        public double aiRange() {
            return range;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity target = Targeting.inFront(caster, range, minDot);
            if (target == null) {
                // Raté : la technique part quand même vers le point visé (plus tard : dégâts aux blocs à l'impact)
                Vec3 end = caster.getEyePosition().add(caster.getLookAngle().scale(range));
                showStrike(look, caster, null, end, 0);
                caster.level().playSound(null, end.x, end.y, end.z, sound, SoundSource.PLAYERS, 0.8f, 1f);
                return false;
            }
            showStrike(look, caster, target, target.getBoundingBox().getCenter(), holdTicks(power));
            target.invulnerableTime = 0;
            target.hurt(damageFrom(caster), damage * power);
            if (knockback > 0) target.knockback(knockback * power, caster.getX() - target.getX(), caster.getZ() - target.getZ());
            for (Effect e : effects) target.addEffect(e.create(power), caster);
            sound(target, sound);
            return true;
        }
    }

    /** Onde de choc autour du lanceur. */
    static final class Blast extends Technique {
        private final double radius;
        private final float damage, knockback;
        private final Look look;
        private final SoundEvent sound;

        Blast(String id, NenType category, int cost, int cooldown, double radius, float damage, float knockback,
              Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.radius = radius;
            this.damage = damage;
            this.knockback = knockback;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public net.minecraft.network.chat.Component conditions() {
            return net.minecraft.network.chat.Component.translatable("technique.hxh.cond.area",
                    String.format(java.util.Locale.ROOT, "%.0f", radius));
        }

        @Override
        public double aiRange() {
            return radius;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            List<LivingEntity> targets = Targeting.around(caster, radius);
            for (LivingEntity t : targets) {
                t.invulnerableTime = 0;
                t.hurt(damageFrom(caster), damage * power);
                t.knockback(knockback * power, caster.getX() - t.getX(), caster.getZ() - t.getZ());
            }
            NenFx.shockwave(caster, (float) radius, look.color());
            NenFx.impact(caster, caster.position().add(0, 0.3, 0), 1.6f, look.color());
            for (LivingEntity t : targets) NenFx.impactOn(caster, t, 1f, look.color());
            sound(caster, sound);
            return true; // l'onde part même sans cible
        }
    }

    /** Renforcement sur soi. */
    static final class SelfBuff extends Technique {
        private final List<Effect> effects;
        private final Look look;
        private final SoundEvent sound;

        SelfBuff(String id, NenType category, int cost, int cooldown, List<Effect> effects,
                 Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.effects = effects;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            int hold = 20;
            for (Effect e : effects) {
                MobEffectInstance inst = e.create(power);
                caster.addEffect(inst, caster);
                hold = Math.max(hold, inst.getDuration());
            }
            showSelf(look, caster, hold);
            sound(caster, sound);
            return true;
        }
    }

    /** Soin instantané sur soi. */
    static final class SelfHeal extends Technique {
        private final float amount;
        private final Look look;
        private final SoundEvent sound;

        SelfHeal(String id, NenType category, int cost, int cooldown, float amount,
                 Look look, SoundEvent sound) {
            super(id, category, cost, cooldown);
            this.amount = amount;
            this.look = look;
            this.sound = sound;
        }

        @Override
        public boolean isHeal() {
            return true;
        }

        @Override
        public boolean perform(LivingEntity caster, float power) {
            caster.heal(amount * power);
            showSelf(look, caster, 40);
            sound(caster, sound);
            return true;
        }
    }
}
