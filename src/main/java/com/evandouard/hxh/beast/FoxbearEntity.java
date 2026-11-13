package com.evandouard.hxh.beast;

import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Renard-ours (Foxbear) de l'île de la Baleine : un gros prédateur des forêts. Contrairement à l'ours polaire, il
 * chasse : un adulte attaque tout joueur à portée ; la mère défend ses petits comme l'ours polaire. Plus résistant et
 * plus fort qu'un ours. Modèle provisoire (celui de l'ours polaire, agrandi) en attendant le modèle Blockbench.
 * Plus tard : un petit apprivoisé qui grandit et protège le joueur (voir la feuille de route).
 */
public class FoxbearEntity extends PolarBear {
    public FoxbearEntity(EntityType<? extends PolarBear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Prédateur : un adulte s'en prend à tout joueur proche (survie)
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> !isBaby() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.FOXBEAR.get().create(level);
    }

    /** Apparition naturelle : comme les animaux (herbe, lumière). */
    public static boolean canSpawn(EntityType<? extends Animal> type, net.minecraft.world.level.ServerLevelAccessor level,
                                   net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos,
                                   net.minecraft.util.RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }
}
