package com.evandouard.hxh.hatsu;

import com.evandouard.hxh.HxHGameRules;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Projectile de Nen qui file tout droit.
 * <ul>
 *     <li>{@link #ORB} — Jajanken : Pierre. Boule d'aura dont la taille et la force viennent de l'aura chargée.
 *         Elle creuse sa trace sur ~20 blocs (règle {@code hxhNenDestruction}), emporte la première cible
 *         une seconde, puis explose en bouquet final.</li>
 *     <li>{@link #BLADE} — Jajanken : Ciseaux. Lame d'aura qui file jusqu'à s'épuiser, toucher un bloc ou une cible.</li>
 * </ul>
 */
public class NenProjectile extends Entity {
    public static final int ORB = 0, BLADE = 1, CARD = 2;

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(NenProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(NenProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(NenProjectile.class, EntityDataSerializers.INT);

    /** Vitesse (blocs par tick) et portée (blocs). */
    static final double ORB_SPEED = 0.9, ORB_RANGE = 20, BLADE_SPEED = 1.3, BLADE_RANGE = 14, CARD_RANGE = 18;
    /** Temps pendant lequel la boule emporte sa cible avant d'exploser. */
    static final int CARRY_TICKS = 20;

    @Nullable private UUID owner;
    private Vec3 dir = new Vec3(0, 0, 1);
    private double traveled;
    @Nullable private LivingEntity carried;
    private int carryTicks;
    private float damage;
    /** Hauteur des pieds du lanceur au départ : la boule ne creuse pas sous lui (il tomberait dans sa propre trace). */
    private double launchFeetY = Double.NEGATIVE_INFINITY;

    public NenProjectile(EntityType<? extends NenProjectile> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Lance un projectile depuis les yeux du lanceur, dans la direction où il regarde. */
    public static NenProjectile launch(LivingEntity caster, int kind, float power, float damage, int color) {
        NenProjectile p = new NenProjectile(ModEntities.NEN_PROJECTILE.get(), caster.level());
        p.owner = caster.getUUID();
        p.dir = caster.getLookAngle().normalize();
        p.damage = damage;
        p.launchFeetY = caster.getY();
        p.entityData.set(KIND, kind);
        p.entityData.set(POWER, power);
        p.entityData.set(COLOR, color);
        Vec3 start = caster.getEyePosition().add(p.dir.scale(1.0)).add(0, -0.3, 0);
        p.moveTo(start.x, start.y, start.z, caster.getYRot(), caster.getXRot());
        p.setDeltaMovement(p.dir.scale(kind == ORB ? ORB_SPEED : BLADE_SPEED));
        caster.level().addFreshEntity(p);
        return p;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, ORB);
        builder.define(POWER, 1f);
        builder.define(COLOR, NenFx.ENHANCER);
    }

    public int kind() {
        return entityData.get(KIND);
    }

    /** Aura mise dans le coup (taille de la boule). */
    public float power() {
        return entityData.get(POWER);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    /** Rayon de la boule (blocs) selon l'aura chargée. */
    public float radius() {
        return kind() == ORB ? 0.6f + Math.min(2.4f, power() / 45f) : 0.6f;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        if (level().isClientSide()) {
            // Le client avance comme le serveur entre deux mises à jour (mouvement fluide)
            setPos(position().add(v));
            return;
        }
        if (dir.lengthSqr() < 1e-6) dir = v.lengthSqr() > 0 ? v.normalize() : new Vec3(0, 0, 1);
        ServerLevel level = (ServerLevel) level();
        LivingEntity caster = owner == null ? null : level.getEntity(owner) instanceof LivingEntity l ? l : null;

        if (carried != null) {
            // La cible est emportée par la boule, puis tout explose
            carryTicks++;
            Vec3 next = position().add(v.scale(0.6));
            setPos(next);
            carried.teleportTo(next.x + dir.x * 0.6, next.y - carried.getBbHeight() * 0.4, next.z + dir.z * 0.6);
            carried.setDeltaMovement(Vec3.ZERO);
            carried.hurtMarked = true;
            digAround(level, radius());
            if (carryTicks >= CARRY_TICKS || !carried.isAlive()) explode(level, caster);
            return;
        }

        Vec3 next = position().add(v);
        // Bloc sur le chemin : creusé (Pierre, règle active) ou fin de course
        if (kind() == ORB) {
            if (!digAround(level, radius())) {
                explode(level, caster);
                return;
            }
        } else if (!level.getBlockState(BlockPos.containing(next)).getCollisionShape(level, BlockPos.containing(next)).isEmpty()) {
            NenFx.slashAt(level, next, 1.4f, color());
            discard();
            return;
        }
        // Cible touchée
        float r = radius();
        AABB box = new AABB(next.subtract(r, r, r), next.add(r, r, r));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != caster
                && !(e instanceof com.evandouard.hxh.npc.NpcEntity))) {
            if (kind() == ORB) {
                carried = target;
                target.hurt(level.damageSources().indirectMagic(this, caster), damage * 0.3f);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 0.6f);
            } else {
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().indirectMagic(this, caster), damage);
                target.knockback(0.5, -dir.x, -dir.z);
                NenFx.slashAt(level, target.getBoundingBox().getCenter(), 1.8f, color());
                discard();
            }
            setPos(next);
            return;
        }
        setPos(next);
        traveled += v.length();
        double range = kind() == ORB ? ORB_RANGE : kind() == CARD ? CARD_RANGE : BLADE_RANGE;
        if (traveled >= range) {
            if (kind() == ORB) explode(level, caster);
            else discard(); // la lame s'épuise
        }
    }

    /** La règle de destruction est active et on n'est pas dans la Tour Céleste. */
    private boolean destructive(ServerLevel level) {
        return HxHGameRules.canDestroy(level);
    }

    /**
     * Creuse les blocs autour de la boule. @return false si un bloc l'arrête (règle désactivée, bloc trop dur)
     */
    private boolean digAround(ServerLevel level, float r) {
        boolean destroy = destructive(level);
        BlockPos c = BlockPos.containing(position());
        int ir = (int) Math.ceil(r);
        boolean blocked = false;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-ir, -ir, -ir), c.offset(ir, ir, ir))) {
            if (p.distToCenterSqr(position()) > r * r) continue;
            // Les premiers blocs : on laisse le sol sous les pieds du lanceur
            if (traveled < r + 2.5 && p.getY() < launchFeetY - 0.01) continue;
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.getCollisionShape(level, p).isEmpty()) continue;
            if (destroy && breakable(level, p, s)) {
                level.destroyBlock(p, false, this);
            } else if (p.distToCenterSqr(position()) < (r * 0.5) * (r * 0.5) + 0.3) {
                blocked = true;
            }
        }
        return !blocked;
    }

    static boolean breakable(ServerLevel level, BlockPos p, BlockState s) {
        float hardness = s.getDestroySpeed(level, p);
        return hardness >= 0 && hardness <= 50 && level.getBlockEntity(p) == null;
    }

    /** Bouquet final : dégâts et projection selon l'aura chargée, cratère si la règle est active. */
    private void explode(ServerLevel level, @Nullable LivingEntity caster) {
        float power = power();
        float blast = 2.5f + power / 30f;
        Vec3 c = position();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(blast),
                e -> e.isAlive() && e != caster && !(e instanceof com.evandouard.hxh.npc.NpcEntity))) {
            float falloff = (float) Math.max(0.35, 1 - e.position().distanceTo(c) / (blast + 1));
            e.invulnerableTime = 0;
            e.hurt(level.damageSources().indirectMagic(this, caster), damage * falloff);
            Vec3 push = e.position().subtract(c).normalize();
            e.knockback(0.6 + power / 60f, -push.x, -push.z);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.3 + power / 200f, 0));
            e.hurtMarked = true;
        }
        if (destructive(level)) {
            int ir = (int) Math.ceil(blast);
            BlockPos b = BlockPos.containing(c);
            for (BlockPos p : BlockPos.betweenClosed(b.offset(-ir, -ir, -ir), b.offset(ir, ir, ir))) {
                double d = Math.sqrt(p.distToCenterSqr(c));
                if (d > blast) continue;
                // Tout disparaît dans le rayon ; seule la dernière couronne est irrégulière
                if (d > blast - 1 && level.random.nextFloat() < 0.4f) continue;
                BlockState s = level.getBlockState(p);
                if (!s.isAir() && breakable(level, p, s)) level.destroyBlock(p, false, this);
            }
        }
        NenFx.impactAt(level, c, 2.5f + power / 25f, color());
        NenFx.shockwaveAt(level, c.add(0, -0.5, 0), blast * 1.5f, color());
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.8f);
        discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
