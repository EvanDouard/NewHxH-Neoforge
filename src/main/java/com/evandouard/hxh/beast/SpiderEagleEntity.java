package com.evandouard.hxh.beast;

import com.evandouard.hxh.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Aigle-araignée : grand oiseau des falaises (Mont Split). Il vole en planant, ne s'apprivoise pas et pond de temps
 * en temps un œuf, le même que celui de l'épreuve de l'Examen. Modèle provisoire : perroquet gris agrandi.
 */
public class SpiderEagleEntity extends Parrot {
    /** Délai entre deux pontes (ticks) : 10 à 20 minutes. */
    public static final int EGG_MIN = 12000, EGG_SPREAD = 12000;
    private int eggTime;

    public SpiderEagleEntity(EntityType<? extends Parrot> type, Level level) {
        super(type, level);
        eggTime = EGG_MIN + level.random.nextInt(EGG_SPREAD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Parrot.createAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    public static boolean canSpawn(EntityType<SpiderEagleEntity> type, LevelAccessor level, MobSpawnType reason, BlockPos pos,
                                   RandomSource random) {
        return pos.getY() > 80 && level.getRawBrightness(pos, 0) > 8;
    }

    /** Ne s'apprivoise pas, ne s'assoit pas, ne mange pas de graines. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || !isAlive() || isBaby()) return;
        if (--eggTime <= 0) {
            spawnAtLocation(ModItems.SPIDER_EAGLE_EGG.get());
            playSound(SoundEvents.CHICKEN_EGG, 1f, 0.6f);
            eggTime = EGG_MIN + random.nextInt(EGG_SPREAD);
        }
    }

    /** Pour les tests : ticks avant la prochaine ponte. */
    public void setEggTime(int ticks) {
        eggTime = ticks;
    }

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 1.6f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EggLayTime", eggTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("EggLayTime")) eggTime = tag.getInt("EggLayTime");
    }
}
