package com.evandouard.hxh.combat;

import com.evandouard.hxh.HxHGameRules;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.fx.NenFx;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Paliers d'impact des coups, d'après la Force en % du niveau max :
 * <pre>
 *   faible       &lt; 25 %   rien de plus que l'étoile d'impact
 *   moyen        25-50 %  éclats du sol (particules de blocs)
 *   lourd        50-75 %  éclats + les blocs autour sautent (vague)
 *   dévastateur  ≥ 75 %   tout + cratère (règle hxhNenDestruction, jamais dans la Tour Céleste)
 *                         + image d'impact : attaquant et cible figés ~0,8 s, éclair et lignes de vitesse,
 *                         puis la projection part ; la cible défonce le mur qu'elle percute.
 * </pre>
 * La frappe chargée parfaite prend le palier de la Force, la frappe chargée « bonne » et le coup de pied un palier
 * de moins, l'attaque basique est plafonnée au palier moyen.
 */
public final class Impacts {
    public enum Tier {
        WEAK, MEDIUM, HEAVY, DEVASTATING;

        public Tier down() {
            return values()[Math.max(0, ordinal() - 1)];
        }

        public Tier atMost(Tier max) {
            return ordinal() > max.ordinal() ? max : this;
        }
    }

    public static final float MEDIUM_FROM = 0.25f, HEAVY_FROM = 0.50f, DEVASTATING_FROM = 0.75f;
    /** Durée de l'image d'impact (attaquant et cible figés). */
    public static final int FREEZE_TICKS = 16;
    /** Après la projection : temps pendant lequel la cible défonce le mur qu'elle percute. */
    public static final int WALL_WATCH_TICKS = 24;
    public static final float CRATER_RADIUS = 1.8f, WALL_RADIUS = 1.3f;

    private record Frozen(LivingEntity attacker, LivingEntity target, Vec3 attackerPos, Vec3 targetPos, long until,
                          Runnable release) {}

    /**
     * Cible projetée qu'on surveille : si elle percute un mur, elle le défonce. {@code digs} = combien de murs elle
     * peut encore creuser (le coup de pied fort continue sa course après le premier), {@code radius} = taille du trou.
     */
    private static final class Flying {
        final LivingEntity target;
        final long until;
        final float radius;
        int digs;
        Vec3 lastVelocity = Vec3.ZERO;

        Flying(LivingEntity target, long until) {
            this(target, until, WALL_RADIUS, 1);
        }

        Flying(LivingEntity target, long until, float radius, int digs) {
            this.target = target;
            this.until = until;
            this.radius = radius;
            this.digs = digs;
        }
    }

    /** Coup de pied fort : la cible part loin et creuse un trou autour d'elle dans les murs qu'elle percute (2 au plus). */
    public static final float KICK_DIG_RADIUS = 1.6f;
    public static final int KICK_DIGS = 2;
    public static final int KICK_WATCH_TICKS = 30;

    /** Surveille une cible qu'on vient de projeter : elle creusera les murs qu'elle percute. */
    public static void watchWallHits(LivingEntity target, float radius, int digs, int ticks) {
        FLYING.removeIf(f -> f.target == target);
        FLYING.add(new Flying(target, target.level().getGameTime() + ticks, radius, digs));
    }

    private static final List<Frozen> FROZEN = new ArrayList<>();
    private static final List<Flying> FLYING = new ArrayList<>();

    private Impacts() {}

    /** Force en fraction du niveau max (0 sans statistiques). */
    public static float strengthRatio(LivingEntity e) {
        if (!e.hasData(ModAttachments.HUNTER)) return 0f;
        return HunterStats.get(e).level(Stat.STRENGTH) / (float) Stat.MAX_LEVEL;
    }

    public static Tier tierFor(float ratio) {
        if (ratio >= DEVASTATING_FROM) return Tier.DEVASTATING;
        if (ratio >= HEAVY_FROM) return Tier.HEAVY;
        if (ratio >= MEDIUM_FROM) return Tier.MEDIUM;
        return Tier.WEAK;
    }

    public static Tier tierOf(LivingEntity attacker) {
        return tierFor(strengthRatio(attacker));
    }

    public static boolean isFrozen(LivingEntity e) {
        for (Frozen f : FROZEN) if (f.attacker == e || f.target == e) return true;
        return false;
    }

    /** La règle de destruction est active et on n'est pas dans la Tour Céleste. */
    public static boolean destructive(ServerLevel level) {
        return HxHGameRules.canDestroy(level);
    }

    /**
     * Le coup a touché : effets du palier. {@code projection} est la poussée du coup ; elle part tout de suite,
     * sauf au palier dévastateur où elle attend la fin de l'image d'impact.
     */
    public static void land(LivingEntity attacker, LivingEntity target, Tier tier, Runnable projection) {
        if (!(attacker.level() instanceof ServerLevel level) || tier == Tier.WEAK) {
            projection.run();
            return;
        }
        BlockPos ground = groundUnder(level, target);
        debris(level, target, ground, tier == Tier.MEDIUM ? 10 : tier == Tier.HEAVY ? 22 : 40);
        if (tier == Tier.MEDIUM) {
            projection.run();
            return;
        }
        NenFx.quake(level, Vec3.atBottomCenterOf(ground.above()), tier == Tier.HEAVY ? 3f : 5f);
        if (tier == Tier.HEAVY) {
            projection.run();
            return;
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.8f, 1.4f);
        if (destructive(level)) crater(level, Vec3.atCenterOf(ground), CRATER_RADIUS, attacker);
        NenFx.impactFrame(attacker, target, FREEZE_TICKS);
        FROZEN.removeIf(f -> f.target == target || f.attacker == attacker);
        FROZEN.add(new Frozen(attacker, target, attacker.position(), target.position(),
                level.getGameTime() + FREEZE_TICKS, projection));
    }

    /** Seuils de dégâts (un seul coup, avant défense) d'une technique ratée qui frappe le décor. */
    public static final float MISS_DEBRIS = 8f, MISS_CRACK = 15f, MISS_CRATER = 25f;

    /**
     * Une technique ratée part quand même : si elle percute un bloc sur sa trajectoire, le décor encaisse selon ses
     * dégâts. Éclats dès 8, petit trou (rayon 1) dès 15, cratère dès 25 ; la casse demande la règle
     * hxhNenDestruction (jamais dans la Tour Céleste). @return blocs cassés
     */
    public static int missedStrike(LivingEntity caster, double range, float damage) {
        if (!(caster.level() instanceof ServerLevel level) || damage < MISS_DEBRIS) return 0;
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, caster));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return 0;
        BlockPos pos = hit.getBlockPos();
        BlockState s = level.getBlockState(pos);
        NenFx.debris(level, hit.getLocation(), damage >= MISS_CRACK ? 18 : 10, s.getMapColor(level, pos).col);
        if (damage < MISS_CRACK || !destructive(level)) return 0;
        return crater(level, Vec3.atCenterOf(pos), damage >= MISS_CRATER ? CRATER_RADIUS : 1.0f, caster);
    }

    /** Bloc plein sous les pieds de la cible (jusqu'à 3 blocs plus bas). */
    private static BlockPos groundUnder(ServerLevel level, LivingEntity target) {
        BlockPos p = BlockPos.containing(target.getX(), target.getY() - 0.2, target.getZ());
        for (int i = 0; i < 3 && level.getBlockState(p).getCollisionShape(level, p).isEmpty(); i++) p = p.below();
        return p;
    }

    /** Éclats du sol : particules de blocs autour des pieds (rien sous un mob en l'air). */
    private static void debris(ServerLevel level, LivingEntity target, BlockPos ground, int count) {
        BlockState s = level.getBlockState(ground);
        if (s.isAir()) return;
        // éclats dessinés par le mod, de la couleur du bloc (pas de particules de Minecraft)
        int color = s.getMapColor(level, ground).col;
        NenFx.debris(level, new Vec3(target.getX(), ground.getY() + 1.0, target.getZ()), Math.min(24, count / 2 + 4), color);
    }

    /** Sphère de blocs cassés (blocs pas trop durs, sans bloc-entité). */
    static int crater(ServerLevel level, Vec3 center, float r, LivingEntity breaker) {
        int ir = (int) Math.ceil(r), broken = 0;
        BlockPos c = BlockPos.containing(center);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-ir, -ir, -ir), c.offset(ir, ir, ir))) {
            if (p.distToCenterSqr(center) > r * r) continue;
            BlockState s = level.getBlockState(p);
            if (s.isAir() || !breakable(level, p, s)) continue;
            level.destroyBlock(p, false, breaker);
            broken++;
        }
        return broken;
    }

    static boolean breakable(ServerLevel level, BlockPos p, BlockState s) {
        float hardness = s.getDestroySpeed(level, p);
        return hardness >= 0 && hardness <= 50 && level.getBlockEntity(p) == null;
    }

    /** Chaque tick serveur : garde les figés en place, puis lâche la projection ; défonce les murs percutés. */
    public static void tickAll() {
        if (!FROZEN.isEmpty()) {
            List<Frozen> done = new ArrayList<>();
            for (Frozen f : FROZEN) {
                long now = f.target.level().getGameTime();
                if (now >= f.until || !f.target.isAlive() || !f.attacker.isAlive()) {
                    done.add(f);
                    continue;
                }
                hold(f.attacker, f.attackerPos);
                hold(f.target, f.targetPos);
            }
            for (Frozen f : done) {
                FROZEN.remove(f);
                if (!f.target.isAlive()) continue;
                f.release.run();
                FLYING.add(new Flying(f.target, f.target.level().getGameTime() + WALL_WATCH_TICKS));
            }
        }
        if (!FLYING.isEmpty()) {
            Iterator<Flying> it = FLYING.iterator();
            while (it.hasNext()) {
                Flying f = it.next();
                LivingEntity t = f.target;
                if (!t.isAlive() || !(t.level() instanceof ServerLevel level) || level.getGameTime() > f.until) {
                    it.remove();
                    continue;
                }
                if (!t.horizontalCollision) {
                    if (t.getDeltaMovement().horizontalDistanceSqr() > 0.01) f.lastVelocity = t.getDeltaMovement();
                    continue;
                }
                Vec3 v = f.lastVelocity.horizontalDistanceSqr() > 1e-4 ? f.lastVelocity : t.getDeltaMovement();
                Vec3 dir = v.horizontalDistanceSqr() > 1e-4 ? new Vec3(v.x, 0, v.z).normalize() : Vec3.ZERO;
                wallHit(level, t, dir, f.radius);
                f.digs--;
                if (f.digs <= 0 || !destructive(level)) {
                    it.remove();
                } else if (v.horizontalDistanceSqr() > 0.04) {
                    // le trou est fait : la cible continue sa course, un peu freinée, et peut creuser plus loin
                    t.setDeltaMovement(v.x * 0.7, Math.max(v.y, 0.1), v.z * 0.7);
                    t.hurtMarked = true;
                }
            }
        }
    }

    /**
     * La cible percute un mur : trou autour d'elle (règle hxhNenDestruction), éclats de la couleur du bloc, étoile
     * d'impact, onde de choc, son d'explosion sourd, et dégâts de choc.
     */
    public static void wallHit(ServerLevel level, LivingEntity t, Vec3 dir, float radius) {
        Vec3 at = t.getBoundingBox().getCenter().add(dir.scale(0.6));
        BlockPos wall = BlockPos.containing(t.getBoundingBox().getCenter().add(dir.scale(1.0)));
        BlockState s = level.getBlockState(wall);
        if (!s.isAir()) NenFx.debris(level, at, 24, s.getMapColor(level, wall).col);
        if (destructive(level)) crater(level, at, radius, t);
        NenFx.impactAt(level, at, 2.4f, NenFx.WHITE);
        NenFx.shockwaveAt(level, at, radius + 1.2f, NenFx.WHITE);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.5f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 0.6f);
        t.invulnerableTime = 0;
        t.hurt(t.damageSources().flyIntoWall(), 3f);
    }

    private static void hold(LivingEntity e, Vec3 pos) {
        e.setDeltaMovement(Vec3.ZERO);
        if (e.position().distanceToSqr(pos) > 1e-4) {
            if (e instanceof ServerPlayer p) p.connection.teleport(pos.x, pos.y, pos.z, p.getYRot(), p.getXRot());
            else e.teleportTo(pos.x, pos.y, pos.z);
        }
        e.hurtMarked = true;
    }
}
