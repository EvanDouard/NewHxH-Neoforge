package com.evandouard.hxh.nen;

import com.evandouard.hxh.HxHConfig;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.Technique;
import com.evandouard.hxh.hatsu.Techniques;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Comportement des mobs qui ont le Nen :
 * <ul>
 *     <li>ils lancent les techniques de leur Hatsu quand une cible est à portée ;</li>
 *     <li>ils passent en Ten pendant un combat et se soignent avec le Nen quand ils sont blessés ;</li>
 *     <li>les mobs passifs deviennent neutres (ils ripostent), les agressifs restent agressifs ;</li>
 *     <li>ils ne disparaissent plus (uniques) ;</li>
 *     <li>leur puissance à l'éveil dépend du temps de jeu total du monde.</li>
 * </ul>
 */
public final class NenMobAI {
    /** Niveau max d'une stat de mob : toujours sous le max d'un joueur (100). */
    public static final int MOB_MAX_LEVEL = 90;
    private static final Set<Mob> EQUIPPED = Collections.newSetFromMap(new WeakHashMap<>());
    /** Mobs passés en Ten automatiquement pour un combat (on ne touche pas à un Ten réglé à la main). */
    private static final Set<Mob> AUTO_TEN = Collections.newSetFromMap(new WeakHashMap<>());

    private NenMobAI() {}

    /** Appelé quand un mob vient d'éveiller son Nen (quelle que soit la voie). */
    public static void onAwakened(Mob mob) {
        mob.setPersistenceRequired();
        rollPowerIfFresh(mob);
        ensureGoals(mob);
    }

    /** Ajoute (une seule fois par entité chargée) les objectifs de combat au Nen. */
    public static void ensureGoals(Mob mob) {
        if (mob.level().isClientSide() || !EQUIPPED.add(mob)) return;
        boolean neutral = !(mob instanceof Enemy);
        mob.goalSelector.addGoal(1, new NenCombatGoal(mob, neutral));
        if (neutral && mob instanceof PathfinderMob pathfinder) {
            mob.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinder));
        }
    }

    // ---------- Puissance selon le temps de jeu ----------

    /**
     * Niveau d'une stat pour un mob qui s'éveille dans un monde joué depuis {@code hours} heures.
     * Monte vite au début puis plafonne (~65 à 100 h), avec 3 % de chances d'un mob d'élite (+40 %).
     */
    public static int rollLevel(double hours, RandomSource random) {
        double base = 80 * (1 - Math.exp(-hours / 60.0));
        double level = base * (0.6 + 0.6 * random.nextDouble());
        if (random.nextFloat() < 0.03f) level *= 1.4;
        return (int) Math.min(MOB_MAX_LEVEL, Math.round(level));
    }

    private static void rollPowerIfFresh(Mob mob) {
        HunterData data = HunterStats.get(mob);
        for (Stat s : Stat.values()) if (data.level(s) != 0) return; // déjà réglé à la main
        double hours = mob.level().getGameTime() / 72000.0;
        for (Stat s : Stat.values()) data.setLevel(s, rollLevel(hours, mob.getRandom()));
        // Niveau de Nen et maîtrises suivent la même logique (maîtrises un peu en dessous du Nen)
        int nen = rollLevel(hours, mob.getRandom());
        data.setNenLevel(nen);
        for (com.evandouard.hxh.data.Mastery m : com.evandouard.hxh.data.Mastery.values()) {
            data.setMasteryLevel(m, (int) Math.round(nen * (0.5 + 0.4 * mob.getRandom().nextDouble())));
        }
        HunterStats.applyAttributes(mob);
        mob.setHealth(mob.getMaxHealth());
    }

    // ---------- Chaque tick ----------

    /** Ten en combat, retour au neutre ensuite, soin par le Nen quand la vie baisse. */
    public static void tick(Mob mob) {
        HunterData data = HunterStats.get(mob);
        if (!data.nenUnlocked()) return;
        // Sans personnage : le Hatsu de son espèce (zombies de Nen, flèches d'aura, mines du creeper…)
        if (data.kit().isEmpty() && !mob.getTags().contains(com.evandouard.hxh.hatsu.MobTechniques.NEN_CLONE)) {
            String own = com.evandouard.hxh.hatsu.MobKits.idFor(mob);
            if (own != null) {
                var k = com.evandouard.hxh.hatsu.MobKits.get(own);
                if (data.nenType() == null && k != null) data.setNenType(k.type());
                data.setKit(own);
            }
        }
        // Un creeper qui maîtrise le Nen n'explose plus lui-même : ses mines le font pour lui
        if (mob instanceof net.minecraft.world.entity.monster.Creeper creeper && data.kit().equals("mob_creeper")) creeper.setSwellDir(-1);
        LivingEntity target = mob.getTarget();
        boolean fighting = target != null && target.isAlive();
        if (fighting && data.mode() == NenMode.NONE && NenModes.trySetMode(mob, NenMode.TEN) == null) AUTO_TEN.add(mob);
        if (!fighting && data.mode() == NenMode.TEN && AUTO_TEN.remove(mob)) NenModes.trySetMode(mob, NenMode.NONE);

        float ratio = mob.getHealth() / mob.getMaxHealth();
        if (!data.healing() && ratio < 0.5f && data.aura() > 20) NenHealing.setHealing(mob, true);
        if (data.healing() && ratio >= 0.9f) NenHealing.setHealing(mob, false);
    }

    /** Tourne le mob face à sa cible (les techniques visent dans la direction du regard). */
    static void face(Mob mob, LivingEntity target) {
        Vec3 d = target.getBoundingBox().getCenter().subtract(mob.getEyePosition());
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setXRot(pitch);
    }

    /**
     * Choisit une technique prête et adaptée (à portée, ou soin si blessé), la sélectionne et la lance.
     * @return true si une technique est partie
     */
    public static boolean tryCast(Mob mob, LivingEntity target) {
        HunterData data = HunterStats.get(mob);
        HatsuKit kit = Hatsu.kitOf(data);
        if (kit == null || kit.techniques().isEmpty()) return false;
        long now = mob.level().getGameTime();
        double distance = mob.distanceTo(target);
        int n = kit.techniques().size();
        for (int i = 0; i < n; i++) {
            int index = (data.selected() + i) % n;
            Technique t = Techniques.get(kit.techniques().get(index));
            if (t == null || !t.implemented() || Hatsu.castRefusal(data, t, now) != null) continue;
            // le Jajanken ultime reste au joueur : un mob n'y a jamais accès
            if (t instanceof Techniques.ChargedOrb orb && orb.isEpic()) continue;
            boolean useful = t.isHeal() ? mob.getHealth() < mob.getMaxHealth() * 0.7f : distance <= t.aiRange();
            if (!useful) continue;
            data.setSelected(index);
            face(mob, target);
            Hatsu.cast(mob);
            return true;
        }
        return false;
    }

    /** Coup au corps à corps pour les mobs qui n'en ont pas (cochon, villageois…). */
    static void neutralMelee(Mob mob, LivingEntity target) {
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        double damage = attack != null ? attack.getValue()
                : 1.0 + HunterStats.get(mob).level(Stat.STRENGTH) * HxHConfig.STRENGTH_DAMAGE_PER_LEVEL.get();
        mob.swing(InteractionHand.MAIN_HAND);
        target.hurt(mob.damageSources().mobAttack(mob), (float) damage);
    }

    /**
     * Objectif de combat. Pour un mob agressif, il ne gère que les techniques (son attaque d'origine
     * s'occupe du déplacement). Pour un mob neutre, il gère aussi l'approche et les coups.
     */
    static final class NenCombatGoal extends Goal {
        private final Mob mob;
        private final boolean neutral;
        private int castDelay = 20;
        private int meleeDelay = 0;
        private int repath = 0;

        NenCombatGoal(Mob mob, boolean neutral) {
            this.mob = mob;
            this.neutral = neutral;
            setFlags(neutral ? EnumSet.of(Flag.MOVE, Flag.LOOK) : EnumSet.noneOf(Flag.class));
        }

        private boolean active() {
            if (!mob.hasData(ModAttachments.HUNTER) || !HunterStats.get(mob).nenUnlocked()) return false;
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive() && !t.isSpectator();
        }

        @Override
        public boolean canUse() {
            return active();
        }

        @Override
        public boolean canContinueToUse() {
            return active();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            if (neutral) {
                mob.getNavigation().stop();
                // Neutre : une fois l'agresseur mort ou disparu, il oublie la querelle
                LivingEntity t = mob.getTarget();
                if (t == null || !t.isAlive()) mob.setTarget(null);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;
            mob.getLookControl().setLookAt(target, 30f, 30f);

            if (neutral) {
                if (--repath <= 0) {
                    repath = 10;
                    mob.getNavigation().moveTo(target, 1.2);
                }
                double reach = mob.getBbWidth() * 2 + target.getBbWidth() + 1.0;
                if (--meleeDelay <= 0 && mob.distanceToSqr(target) <= reach * reach) {
                    meleeDelay = 20;
                    neutralMelee(mob, target);
                }
            }

            if (--castDelay <= 0) {
                castDelay = tryCast(mob, target) ? 30 : 10;
            }
            tactics(target);
        }

        private int dashCooldown = 40;

        /**
         * Déplacements de combat au Nen : esquive des projectiles et des coups, recul pour se replacer (tireurs),
         * dash pour coller la cible (corps à corps). Les dashs partent dans toutes les directions.
         */
        private void tactics(LivingEntity target) {
            if (--dashCooldown > 0 || !mob.onGround()) return;
            HunterData data = HunterStats.get(mob);
            if (data.aura() < DASH_COST) return;
            double dist = mob.distanceTo(target);
            Vec3 to = target.position().subtract(mob.position()).multiply(1, 0, 1);
            if (to.lengthSqr() < 1e-4) return;
            to = to.normalize();
            Vec3 side = new Vec3(-to.z, 0, to.x).scale(mob.getRandom().nextBoolean() ? 1 : -1);
            Vec3 dir = null;
            // 1. un projectile arrive droit sur lui : pas de côté
            if (incomingProjectile()) dir = side;
            // 2. la cible frappe de près : esquive de côté ou en arrière
            else if (dist < 3.5 && target.swinging && mob.getRandom().nextFloat() < 0.5f)
                dir = mob.getRandom().nextBoolean() ? side : side.add(to.scale(-1)).normalize();
            // 3. tireur trop près : il recule (en biais) pour se replacer
            else if (ranged(mob) && dist < 7) dir = to.scale(-1).add(side.scale(0.5)).normalize();
            // 4. combattant au corps à corps à mi-distance : il fonce pour réduire l'écart
            else if (!ranged(mob) && dist > 4.5 && dist < 11 && mob.getRandom().nextFloat() < 0.6f) dir = to.add(side.scale(0.25)).normalize();
            if (dir == null) {
                dashCooldown = 5;
                return;
            }
            dash(mob, dir, dir.dot(to) > 0.5 ? 1.25 : 1.0);
            data.setAura(data.aura() - DASH_COST);
            dashCooldown = 35 + mob.getRandom().nextInt(30);
        }

        private boolean incomingProjectile() {
            for (net.minecraft.world.entity.projectile.Projectile p : mob.level().getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.Projectile.class, mob.getBoundingBox().inflate(7))) {
                if (p.getOwner() == mob || p.onGround()) continue;
                Vec3 v = p.getDeltaMovement();
                if (v.lengthSqr() < 0.04) continue;
                Vec3 toMe = mob.getBoundingBox().getCenter().subtract(p.position());
                if (toMe.normalize().dot(v.normalize()) > 0.9) return true;
            }
            return false;
        }
    }

    /** Aura dépensée par un dash de mob. */
    static final float DASH_COST = 4f;

    /** Tireur (arc, arbalète, sortilèges à distance) : il garde ses distances. */
    static boolean ranged(Mob mob) {
        return mob instanceof net.minecraft.world.entity.monster.RangedAttackMob
                || mob.getMainHandItem().getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem;
    }

    /** Dash : un bond rasant dans la direction donnée, avec des traits de vitesse là où il était. */
    public static void dash(Mob mob, Vec3 dir, double strength) {
        mob.setDeltaMovement(dir.x * strength, 0.25, dir.z * strength);
        mob.hurtMarked = true;
        mob.getNavigation().stop();
        if (mob.level() instanceof net.minecraft.server.level.ServerLevel level)
            com.evandouard.hxh.fx.NenFx.slashAt(level, mob.position().add(0, mob.getBbHeight() * 0.5, 0), 1.1f, 0xFFFFFFFF);
    }
}
