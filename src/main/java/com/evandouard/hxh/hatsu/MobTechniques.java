package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.exam.ExamWorld;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.nen.Targeting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.evandouard.hxh.data.NenType.*;

/**
 * Hatsu propres aux mobs (un par espèce, passifs compris) : le mob qui s'éveille au Nen sans personnage reçoit celui
 * de son espèce (voir {@link MobKits}). Le principe reste le même que pour les personnages : une technique, une
 * recharge, un coût, et l'IA la lance quand la cible est à portée.
 */
public final class MobTechniques {
    private MobTechniques() {}

    /** Étiquette des zombies de Nen (créés par un zombie) : pas de butin, disparition au bout d'un moment. */
    public static final String NEN_CLONE = "hxh_nen_clone";

    private static LivingEntity target(LivingEntity caster, double range) {
        if (caster instanceof Mob m && m.getTarget() != null && m.getTarget().isAlive() && m.distanceTo(m.getTarget()) <= range)
            return m.getTarget();
        return Targeting.inFront(caster, range, 0.7);
    }

    private static void sound(LivingEntity at, net.minecraft.sounds.SoundEvent s, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), s, SoundSource.HOSTILE, 0.9f, pitch);
    }

    /** Base commune : portée et texte de conditions. */
    abstract static class MobTechnique extends Technique {
        final double range;

        MobTechnique(String id, NenType type, int cost, int cooldown, double range) {
            super(id, type, cost, cooldown);
            this.range = range;
        }

        @Override
        public double aiRange() {
            return range;
        }

        @Override
        public Component conditions() {
            return Component.translatable("technique.hxh.cond.target", String.format(java.util.Locale.ROOT, "%.0f", range));
        }
    }

    // ---------- Squelette : flèches de Nen ----------

    /** Trois flèches d'aura en éventail, sans gravité, qui filent droit et frappent fort. */
    public static final Technique NEN_ARROWS = Techniques.register(new MobTechnique("mob_nen_arrows", EMITTER, 20, 90, 22) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            LivingEntity t = target(caster, range);
            Vec3 aim = t != null ? t.getBoundingBox().getCenter().subtract(caster.getEyePosition()).normalize() : caster.getLookAngle();
            for (int i = -1; i <= 1; i++) {
                Vec3 d = aim.yRot(i * 0.12f);
                Arrow arrow = new Arrow(level, caster, new ItemStack(Items.ARROW), null);
                arrow.setPos(caster.getEyePosition().add(d.scale(0.8)));
                arrow.shoot(d.x, d.y, d.z, 2.8f, 0f);
                arrow.setNoGravity(true);
                arrow.setBaseDamage(3.5 * power);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                arrow.addTag("hxh_nen_arrow");
                level.addFreshEntity(arrow);
                NenFx.orb(caster, null, caster.getEyePosition().add(d.scale(16)), 0.35f, NenFx.EMITTER);
            }
            sound(caster, SoundEvents.ARROW_SHOOT, 1.4f);
            return true;
        }
    });

    // ---------- Creeper : mines d'aura (il n'explose plus lui-même) ----------

    /** Quatre mines d'aura autour de la cible ; elles éclatent 1,5 s plus tard. Le creeper, lui, reste entier. */
    public static final Technique AURA_MINES = Techniques.register(new MobTechnique("mob_aura_mines", EMITTER, 30, 120, 12) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            LivingEntity t = target(caster, range);
            if (t == null) return false;
            for (int i = 0; i < 4; i++) {
                double a = caster.getRandom().nextDouble() * Math.PI * 2, d = 1 + caster.getRandom().nextDouble() * 2.5;
                Vec3 at = ExamWorld.safeSpot(level, t.position(), a, d);
                NenFx.shockwaveAt(level, at.add(0, 0.1, 0), 1.6f, 0xFF7CFF5A);
                NenTasks.later(30 + i * 3, () -> {
                    NenFx.impactAt(level, at.add(0, 0.6, 0), 2.2f, 0xFF7CFF5A);
                    NenFx.quake(level, at, 2.8f);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.3f);
                    for (LivingEntity e : Kinds.enemiesAround(caster, at, 2.6)) Kinds.hurt(caster, e, 7f * power);
                });
            }
            sound(caster, SoundEvents.CREEPER_PRIMED, 0.7f);
            return true;
        }
    });

    // ---------- Zombie : zombies de Nen ----------

    /** Deux zombies d'aura sortent du sol et attaquent sa cible ; ils se dissipent au bout de 15 s. */
    public static final Technique NEN_HORDE = Techniques.register(new MobTechnique("mob_nen_horde", CONJURER, 40, 400, 14) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            if (caster.getTags().contains(NEN_CLONE)) return false;
            LivingEntity t = target(caster, range);
            for (int i = 0; i < 2; i++) {
                Zombie z = EntityType.ZOMBIE.create(level);
                if (z == null) continue;
                Vec3 at = ExamWorld.safeSpot(level, caster.position(), caster.getYRot() * Math.PI / 180 + (i == 0 ? 1.6 : -1.6), 2);
                z.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
                var hp = z.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(8 + 4 * power);
                z.setHealth(z.getMaxHealth());
                for (EquipmentSlot s : EquipmentSlot.values()) z.setDropChance(s, 0f);
                z.addTag(NEN_CLONE);
                z.setCustomName(Component.translatable("entity.hxh.nen_zombie"));
                z.setGlowingTag(true);
                if (t != null) z.setTarget(t);
                level.addFreshEntity(z);
                NenFx.flare(z, 1.2f, NenFx.CONJURER);
                NenFx.quake(level, at, 1.8f);
                NenTasks.later(300, () -> {
                    if (!z.isAlive()) return;
                    NenFx.impactAt(level, z.position().add(0, 1, 0), 1.2f, NenFx.CONJURER);
                    z.discard();
                });
            }
            sound(caster, SoundEvents.ZOMBIE_AMBIENT, 0.6f);
            return true;
        }
    });

    // ---------- Araignée : toile d'aura ----------

    public static final Technique AURA_WEB = Techniques.register(new MobTechnique("mob_aura_web", MANIPULATOR, 20, 140, 9) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity t = target(caster, range);
            if (t == null) return false;
            NenFx.chain(caster, t, t.getBoundingBox().getCenter(), 0xFFE8E8E8, 60);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (60 * Math.max(0.5f, power)), 4), caster);
            t.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 128, false, false, false), caster); // ne saute plus
            sound(caster, SoundEvents.SPIDER_AMBIENT, 0.6f);
            return true;
        }
    });

    // ---------- Enderman : pas éclair ----------

    public static final Technique BLINK_STRIKE = Techniques.register(new MobTechnique("mob_blink_strike", SPECIALIST, 25, 120, 18) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            LivingEntity t = target(caster, range);
            if (t == null) return false;
            NenFx.slashAt(level, caster.position().add(0, 1.4, 0), 1.6f, 0xFFB070FF);
            Vec3 back = ExamWorld.safeSpot(level, t.position(), Math.atan2(-t.getLookAngle().z, -t.getLookAngle().x), 1.6);
            caster.teleportTo(back.x, back.y, back.z);
            Kinds.hurt(caster, t, 7f * power);
            NenFx.impactOn(caster, t, 1.6f, 0xFFB070FF);
            sound(caster, SoundEvents.ENDERMAN_TELEPORT, 1.2f);
            return true;
        }
    });

    // ---------- Cochon : charge de sanglier ----------

    public static final Technique BOAR_CHARGE = Techniques.register(new MobTechnique("mob_boar_charge", ENHANCER, 15, 100, 9) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity t = target(caster, range);
            if (t == null) return false;
            Vec3 d = t.position().subtract(caster.position()).multiply(1, 0, 1).normalize();
            caster.setDeltaMovement(d.scale(1.4).add(0, 0.2, 0));
            caster.hurtMarked = true;
            NenTasks.later(6, () -> {
                if (!t.isAlive() || caster.distanceTo(t) > 2.8) return;
                Kinds.hurt(caster, t, 5f * power);
                t.knockback(1.4, caster.getX() - t.getX(), caster.getZ() - t.getZ());
                NenFx.impactOn(caster, t, 1.8f, NenFx.ENHANCER);
            });
            NenFx.flare(caster, 0.8f, NenFx.ENHANCER);
            sound(caster, SoundEvents.PIG_AMBIENT, 0.6f);
            return true;
        }
    });

    // ---------- Vache : lait d'aura (soin des alliés) ----------

    public static final Technique HEALING_MILK = Techniques.register(new Kinds.AllyBuff("mob_healing_milk", ENHANCER, 25, 300,
            6, 6f, List.of(new Techniques.Effect(MobEffects.REGENERATION, 100, 0)),
            new Techniques.Look(Techniques.Style.FLARE, 0xFFFFFFFF, 1f), SoundEvents.COW_MILK));

    // ---------- Mouton : laine-bouclier ----------

    public static final Technique WOOL_SHIELD = Techniques.register(new Kinds.AllyBuff("mob_wool_shield", CONJURER, 20, 300,
            5, 0f, List.of(new Techniques.Effect(MobEffects.ABSORPTION, 200, 1), new Techniques.Effect(MobEffects.DAMAGE_RESISTANCE, 100, 0)),
            new Techniques.Look(Techniques.Style.FLARE, 0xFFF4F4F4, 1.2f), SoundEvents.WOOL_PLACE) {
        @Override
        public double aiRange() {
            return 10;
        }
    });

    // ---------- Poule : œufs explosifs ----------

    public static final Technique EGG_BARRAGE = Techniques.register(new MobTechnique("mob_egg_barrage", EMITTER, 15, 120, 12) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            LivingEntity t = target(caster, range);
            if (t == null) return false;
            for (int i = 0; i < 5; i++) {
                NenTasks.later(i * 4, () -> {
                    if (!t.isAlive()) return;
                    Vec3 at = t.position().add((caster.getRandom().nextDouble() - 0.5) * 2, 0.5, (caster.getRandom().nextDouble() - 0.5) * 2);
                    NenFx.orb(caster, null, at, 0.3f, 0xFFFFF2B0);
                    NenTasks.later(5, () -> {
                        NenFx.impactAt(level, at, 1.2f, 0xFFFFE070);
                        for (LivingEntity e : Kinds.enemiesAround(caster, at, 1.6)) Kinds.hurt(caster, e, 2.5f * power);
                    });
                });
            }
            sound(caster, SoundEvents.CHICKEN_EGG, 0.8f);
            return true;
        }
    });

    // ---------- Loup : croc d'aura ----------

    public static final Technique FANG_LUNGE = Techniques.register(new MobTechnique("mob_fang_lunge", ENHANCER, 15, 90, 8) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            LivingEntity t = target(caster, range);
            if (t == null) return false;
            Vec3 d = t.position().subtract(caster.position()).normalize();
            caster.setDeltaMovement(d.scale(1.2).add(0, 0.35, 0));
            caster.hurtMarked = true;
            NenTasks.later(7, () -> {
                if (!t.isAlive() || caster.distanceTo(t) > 2.6) return;
                Kinds.hurt(caster, t, 6f * power);
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), caster); // saignement
                NenFx.slashAt((ServerLevel) caster.level(), t.getBoundingBox().getCenter(), 1.4f, 0xFFFF4040);
            });
            sound(caster, SoundEvents.WOLF_GROWL, 1.1f);
            return true;
        }
    });

    // ---------- Slime : rebond écrasant ----------

    public static final Technique BOUNCE_SLAM = Techniques.register(new MobTechnique("mob_bounce_slam", ENHANCER, 20, 120, 7) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            caster.setDeltaMovement(caster.getDeltaMovement().add(0, 1.1, 0));
            caster.hurtMarked = true;
            NenTasks.later(18, () -> {
                if (!caster.isAlive()) return;
                caster.setDeltaMovement(0, -1.6, 0);
                caster.hurtMarked = true;
                NenTasks.later(6, () -> {
                    NenFx.quake(level, caster.position(), 4f);
                    NenFx.shockwaveAt(level, caster.position().add(0, 0.2, 0), 4f, 0xFF7CDC5A);
                    for (LivingEntity e : Kinds.enemiesAround(caster, caster.position(), 3.5)) {
                        Kinds.hurt(caster, e, 6f * power);
                        e.knockback(0.8, caster.getX() - e.getX(), caster.getZ() - e.getZ());
                    }
                });
            });
            sound(caster, SoundEvents.SLIME_JUMP, 0.6f);
            return true;
        }
    });

    // ---------- Sorcière : potion de Nen ----------

    public static final Technique NEN_BREW = Techniques.register(new Kinds.Pulse("mob_nen_brew", TRANSMUTER, 25, 160, 5, 3f, 0.3f,
            List.of(new Techniques.Effect(MobEffects.WEAKNESS, 120, 1), new Techniques.Effect(MobEffects.MOVEMENT_SLOWDOWN, 80, 1)),
            new Techniques.Look(Techniques.Style.IMPACT, 0xFFB050FF, 1.4f), SoundEvents.WITCH_THROW));

    // ---------- Golem de fer : séisme ----------

    public static final Technique QUAKE_STOMP = Techniques.register(new Kinds.Pulse("mob_quake_stomp", ENHANCER, 25, 140, 5, 9f, 1.2f,
            List.of(new Techniques.Effect(MobEffects.MOVEMENT_SLOWDOWN, 40, 2)),
            new Techniques.Look(Techniques.Style.SHOCKWAVE, 0xFFD8D8D8, 2f), SoundEvents.IRON_GOLEM_ATTACK));

    // ---------- Blaze : pluie de feu ----------

    public static final Technique FIRE_RAIN = Techniques.register(new MobTechnique("mob_fire_rain", TRANSMUTER, 30, 160, 16) {
        @Override
        public boolean perform(LivingEntity caster, float power) {
            if (!(caster.level() instanceof ServerLevel level)) return false;
            LivingEntity t = target(caster, range);
            if (t == null) return false;
            for (int i = 0; i < 6; i++) {
                NenTasks.later(i * 5, () -> {
                    Vec3 at = t.position().add((caster.getRandom().nextDouble() - 0.5) * 4, 0.3, (caster.getRandom().nextDouble() - 0.5) * 4);
                    NenFx.impactAt(level, at, 1.6f, 0xFFFF7A20);
                    for (LivingEntity e : Kinds.enemiesAround(caster, at, 1.8)) {
                        Kinds.hurt(caster, e, 3f * power);
                        e.igniteForSeconds(3);
                    }
                });
            }
            sound(caster, SoundEvents.BLAZE_SHOOT, 0.8f);
            return true;
        }
    });

    /** Force le chargement de la classe (enregistrement des techniques). */
    public static void init() {}
}
