package com.evandouard.hxh.mob;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Mastery;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.HatsuKit;
import com.evandouard.hxh.hatsu.HatsuKits;
import com.evandouard.hxh.hatsu.Techniques;
import com.evandouard.hxh.nen.NenModes;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Gon « ultime » : se bat comme Gon enfant (Jajanken sans l'ultime) jusqu'à ce que ses points de vie tombent sous
 * {@link #THRESHOLD} ; il passe alors en <b>mode adulte</b> (le Serment contre Neferpitou), une seule fois et pour
 * toujours : stats ×{@link #STAT_MULTIPLIER}, Nen au maximum (donc accès au Jajanken ultime, niveau 60 requis),
 * aura et vie pleines, modèle {@code gon_adulte} plus grand. Explosion d'aura, rugissement, message aux joueurs
 * proches. Le mode adulte est sauvegardé (NBT {@code HxhAdult}).
 */
public class GonUltimeEntity extends CharacterEntity {
    /** Seuil de vie (fraction du max) qui déclenche la transformation. */
    public static final float THRESHOLD = 0.3f;
    public static final int STAT_MULTIPLIER = 3;
    private static final EntityDataAccessor<Boolean> ADULT =
            SynchedEntityData.defineId(GonUltimeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDimensions ADULT_SIZE = EntityDimensions.scalable(0.95f, 2.5f).withEyeHeight(2.25f);

    public GonUltimeEntity(EntityType<? extends GonUltimeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ADULT, false);
    }

    public boolean isAdult() {
        return entityData.get(ADULT);
    }

    @Override
    public String modelId() {
        return isAdult() ? "gon_adulte" : "gon";
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isAdult() ? ADULT_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ADULT.equals(key)) refreshDimensions();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isAlive() && !isAdult() && isPrepared()
                && getHealth() < getMaxHealth() * THRESHOLD) {
            becomeAdult();
        }
    }

    /** Transformation en Gon adulte (une seule fois). */
    public void becomeAdult() {
        if (isAdult()) return;
        entityData.set(ADULT, true);
        refreshDimensions();
        HunterData data = HunterStats.get(this);
        for (Stat stat : Stat.values()) data.setLevel(stat, data.level(stat) * STAT_MULTIPLIER);
        data.setNenLevel(100);
        for (Mastery m : Mastery.values()) data.setMasteryLevel(m, Math.max(80, data.masteryLevel(m)));
        data.setAuraMax(NenModes.computeAuraMax(data));
        data.setAura(data.auraMax());
        HunterStats.applyAttributes(this);
        setHealth(getMaxHealth());
        // la prochaine technique lancée sera le Jajanken ultime
        HatsuKit kit = HatsuKits.get(data.kit());
        if (kit != null) {
            int epic = kit.techniques().indexOf(Techniques.GON_ROCK_EPIC.id());
            if (epic >= 0) data.setSelected(epic);
        }
        setCustomName(Component.translatable("entity.hxh.gon_adulte"));
        if (level() instanceof ServerLevel sl) burst(sl);
    }

    private void burst(ServerLevel sl) {
        Vec3 c = position().add(0, 1.4, 0);
        int rgb = HatsuKits.auraColor("gon");
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 2.5f);
        sl.sendParticles(dust, c.x, c.y, c.z, 160, 1.4, 1.8, 1.4, 0.05);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 60, 0.6, 2.0, 0.6, 0.25);
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2f, 0.8f);
        // l'onde d'aura repousse tout ce qui est autour
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5), e -> e != this)) {
            Vec3 push = e.position().subtract(position()).normalize().scale(1.4);
            e.push(push.x, 0.5, push.z);
            e.hurtMarked = true;
        }
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) {
            p.displayClientMessage(Component.translatable("message.hxh.gon_adult_transform"), false);
        }
    }

    // ---------- Animation des cheveux (adulte) ----------
    /**
     * Animations des cheveux de Gon adulte, dans leur propre contrôleur (indépendant du corps). Répartition des os
     * pour éviter que deux contrôleurs écrivent sur les mêmes os :
     * <ul>
     *   <li>{@code hair_motion_controller} : mouvement passif, uniquement les tronçons des mèches
     *       ({@code <mèche>_1}, {@code _2}…) — {@code animation.gon.hair_idle} ;</li>
     *   <li>à venir, {@code hair_fx_controller} : {@code hair_transform} sur {@code hair_growth_root} (déploiement),
     *       {@code hair_aura} et {@code hair_burst} sur les couches {@code hair_back / hair_core / hair_front} et
     *       les racines {@code <mèche>_0}. Ces os ne sont jamais touchés par le mouvement passif : les deux
     *       s'additionnent le long de la chaîne d'os.</li>
     * </ul>
     * Purement visuel côté client ; seul l'état adulte vient du serveur (donnée synchronisée {@link #ADULT}).
     */
    public static final String HAIR_CONTROLLER = "hair_motion_controller";
    private static final software.bernie.geckolib.animation.RawAnimation HAIR_IDLE =
            software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.gon.hair_idle");

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, HAIR_CONTROLLER, 10,
                state -> isAdult() ? state.setAndContinue(HAIR_IDLE) : software.bernie.geckolib.animation.PlayState.STOP));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HxhAdult", isAdult());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ADULT, tag.getBoolean("HxhAdult"));
        refreshDimensions();
    }
}
