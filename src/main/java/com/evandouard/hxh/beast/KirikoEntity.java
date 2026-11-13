package com.evandouard.hxh.beast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Kiriko : bête magique de la forêt avant Zaban, capable de prendre l'apparence d'un humain. Dans l'épreuve, le faux
 * mari démasqué se transforme en Kiriko, salue le candidat puis s'envole ({@link #leaveIn}). Paisible, ne
 * s'apprivoise pas. Modèle provisoire (perroquet vert agrandi ×3) en attendant le modèle Blockbench.
 */
public class KirikoEntity extends Parrot {
    /** Ticks avant de s'envoler pour de bon (-1 : reste). */
    private int leaving = -1;

    public KirikoEntity(EntityType<? extends Parrot> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Parrot.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.FLYING_SPEED, 0.8);
    }

    /** S'envole et disparaît dans {@code ticks} ticks. */
    public void leaveIn(int ticks) {
        leaving = ticks;
    }

    public boolean isLeaving() {
        return leaving >= 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || leaving < 0) return;
        // monte en spirale, puis s'en va
        if (leaving < 60) {
            setNoGravity(true);
            setDeltaMovement(getDeltaMovement().add(new Vec3(Math.cos(tickCount * 0.2) * 0.05, 0.09, Math.sin(tickCount * 0.2) * 0.05)));
        }
        if (--leaving <= 0) discard();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return isLeaving();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Leaving", leaving);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Leaving")) leaving = tag.getInt("Leaving");
    }
}
