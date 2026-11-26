package com.evandouard.hxh.summon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Créature ou objet invoqué par une technique (Kannon de Netero, Pot-Clean de Knuckle, Terpsichora et Docteur Blythe
 * de Pitou…) : un vrai modèle GeckoLib animé ({@code geo/entity/npc/summon_<modèle>.geo.json}), et non un effet.
 *
 * <p>Le serveur la crée (voir {@link NenSummons}) et décide de tout : modèle, entité suivie, placement, taille, couleur,
 * durée, gestes (animations déclenchées). Elle ne se sauvegarde pas, ne se touche pas, ne bloque rien, et disparaît
 * à la fin de sa durée ou quand l'entité suivie n'est plus là. Le client ne fait que la dessiner (et la recaler sur
 * l'entité suivie à chaque image pour qu'elle ne traîne pas).</p>
 */
public class NenSummonEntity extends Entity implements GeoEntity {
    /** Placement : derrière l'entité suivie (statue, marionnette), au-dessus d'elle (Pot-Clean, poupée), ou fixe. */
    public enum Anchor { BEHIND, ABOVE, FIXED }

    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(NenSummonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> FOLLOW = SynchedEntityData.defineId(NenSummonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANCHOR = SynchedEntityData.defineId(NenSummonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(NenSummonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET = SynchedEntityData.defineId(NenSummonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(NenSummonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(NenSummonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> LABEL = SynchedEntityData.defineId(NenSummonEntity.class, EntityDataSerializers.STRING);

    /** Gestes déclenchables (tous les modèles ne les ont pas tous : chacun n'est joué que sur le sien). */
    public static final String[] ACTIONS = {"strike", "zero", "collect", "pull"};
    /** Durée du fondu d'entrée et de sortie (ticks). */
    public static final int FADE = 8;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int age;
    /** Lanceur (serveur seulement) : ses invocations disparaissent quand ses effets sont effacés. */
    private int owner = -1;
    /** Geste en attente (serveur) : joué quelques ticks après l'apparition, quand les clients ont reçu l'entité. */
    private String pending;
    private int pendingAt;

    public NenSummonEntity(EntityType<? extends NenSummonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODEL, "kannon").define(FOLLOW, -1).define(ANCHOR, Anchor.FIXED.ordinal()).define(SCALE, 1f)
                .define(OFFSET, 0f).define(COLOR, 0xFFFFFFFF).define(LIFE, 40).define(LABEL, "");
    }

    // ---- réglages (côté serveur) -------------------------------------------------------------------------------

    void setup(Entity owner, String model, Entity follow, Anchor anchor, float scale, float offset, int color, int life) {
        this.owner = owner == null ? -1 : owner.getId();
        entityData.set(MODEL, model);
        entityData.set(FOLLOW, follow == null ? -1 : follow.getId());
        entityData.set(ANCHOR, anchor.ordinal());
        entityData.set(SCALE, scale);
        entityData.set(OFFSET, offset);
        entityData.set(COLOR, color);
        entityData.set(LIFE, life);
        snap();
    }

    /** Prolonge l'invocation (elle repart de zéro sans refaire son apparition). */
    public void extend(int life) {
        entityData.set(LIFE, Math.max(entityData.get(LIFE), age + life));
    }

    public void setLabel(String label) {
        if (!label.equals(entityData.get(LABEL))) entityData.set(LABEL, label);
    }

    /** Joue un geste ({@link #ACTIONS}) chez tous les clients qui la voient. */
    public void act(String action) {
        if (age < 3) actLater(action, 3 - age);
        else triggerAnim("action", action);
    }

    /** Joue un geste dans {@code delay} ticks. */
    public void actLater(String action, int delay) {
        pending = action;
        pendingAt = age + Math.max(1, delay);
    }

    public boolean ownedBy(Entity e) {
        return e != null && owner == e.getId();
    }

    // ---- lecture ----------------------------------------------------------------------------------------------

    public String model() { return entityData.get(MODEL); }
    public float scale() { return entityData.get(SCALE); }
    public int color() { return entityData.get(COLOR); }
    public String label() { return entityData.get(LABEL); }
    public int age() { return age; }
    public int life() { return entityData.get(LIFE); }
    public Anchor anchor() { return Anchor.values()[Mth.clamp(entityData.get(ANCHOR), 0, Anchor.values().length - 1)]; }

    public Entity followed() {
        int id = entityData.get(FOLLOW);
        return id < 0 ? null : level().getEntity(id);
    }

    /** Opacité du fondu (0 à 1) à l'instant donné. */
    public float alpha(float partialTick) {
        float t = age + partialTick;
        return Mth.clamp(Math.min(t / FADE, (life() - t) / FADE), 0f, 1f);
    }

    // ---- vie --------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        age++;
        snap();
        if (!level().isClientSide) {
            Entity f = followed();
            boolean lost = entityData.get(FOLLOW) >= 0 && (f == null || !f.isAlive());
            if (pending != null && age >= pendingAt) {
                triggerAnim("action", pending);
                pending = null;
            }
            if (age >= life() || lost) discard();
        }
    }

    /** Se recale sur l'entité suivie. */
    public void snap() {
        Entity f = followed();
        if (f == null) return;
        float yaw = f instanceof LivingEntity l ? l.yBodyRot : f.getYRot();
        float off = entityData.get(OFFSET);
        Vec3 p = switch (anchor()) {
            case BEHIND -> f.position().subtract(Vec3.directionFromRotation(0, yaw).scale(off));
            case ABOVE -> f.position().add(0, f.getBbHeight() + off, 0);
            case FIXED -> position();
        };
        setPos(p.x, p.y, p.z);
        setYRot(yaw);
        yRotO = yaw;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float s = Math.max(1f, scale());
        return getBoundingBox().inflate(s * 0.8, s * 1.2, s * 0.8).move(0, s * 0.6, 0);
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public boolean isAttackable() { return false; }

    /** Lu seulement par /summon (scénario CI, essais) : Model, Scale, Color, Life, Label. Jamais sauvegardée. */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Model")) entityData.set(MODEL, tag.getString("Model"));
        if (tag.contains("Scale")) entityData.set(SCALE, tag.getFloat("Scale"));
        if (tag.contains("Color")) entityData.set(COLOR, tag.getInt("Color"));
        if (tag.contains("Life")) entityData.set(LIFE, tag.getInt("Life"));
        if (tag.contains("Label")) entityData.set(LABEL, tag.getString("Label"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    // ---- animations -------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation base = RawAnimation.begin().thenPlay("appear").thenLoop("idle");
        controllers.add(new AnimationController<>(this, "base", 0, state -> state.setAndContinue(base)));
        AnimationController<NenSummonEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String a : ACTIONS) action.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
