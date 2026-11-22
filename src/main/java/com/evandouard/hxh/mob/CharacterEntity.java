package com.evandouard.hxh.mob;

import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Personnage jouable du manga en mob (Gon, Killua…) : même modèle GeckoLib que son PNJ, ses stats, son Nen et son
 * Hatsu viennent du mob d'exemple {@code data/hxh/mob_presets/<id>.json} (id = nom du type d'entité). Il se sert
 * de son Hatsu comme n'importe quel mob au Nen ({@link com.evandouard.hxh.nen.NenMobAI}).
 * <p>Neutre pour l'instant : il se promène, regarde les joueurs, ne frappe que celui qui l'attaque, et ses amis
 * personnages proches viennent l'aider.
 */
public class CharacterEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    /** Le mob d'exemple a été appliqué (stats, Nen, Hatsu, nom). */
    private boolean prepared = false;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public CharacterEntity(EntityType<? extends CharacterEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    /** Identifiant du personnage : nom du type d'entité ({@code gon}, {@code killua}). */
    public String characterId() {
        ResourceLocation key = EntityType.getKey(getType());
        return key.getPath();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // neutre : riposte seulement, et les autres personnages proches viennent l'aider
        targetSelector.addGoal(1, new HurtByTargetGoal(this, CharacterEntity.class).setAlertOthers(CharacterEntity.class));
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && !prepared) prepare();
        super.tick();
    }

    /** Applique le mob d'exemple du personnage (une seule fois : c'est sauvegardé ensuite). */
    public void prepare() {
        prepared = true;
        MobPreset preset = MobPresets.get(characterId());
        if (preset != null) preset.applyTo(this, false);
        else HunterStats.applyAttributes(this);
    }

    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        triggerAnim("attack", "punch");
        return hit;
    }

    /** Les personnages sont alliés entre eux : pas de riposte si l'un touche l'autre par mégarde (Hatsu de zone). */
    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof CharacterEntity || super.isAlliedTo(other);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HxhPrepared", prepared);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        prepared = tag.getBoolean("HxhPrepared");
    }

    // ---------- GeckoLib ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("punch", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
