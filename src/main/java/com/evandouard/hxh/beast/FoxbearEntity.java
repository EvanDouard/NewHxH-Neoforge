package com.evandouard.hxh.beast;

import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Renard-ours (Foxbear) de l'île de la Baleine : un gros ours des forêts. Même comportement que l'ours polaire
 * (neutre, il riposte, la mère défend ses petits), en plus résistant et plus fort. Il donne de la viande de
 * renard-ours, ingrédient des recettes du Hunter gourmet. Stats à revoir à l'équilibrage final. Modèle provisoire
 * (ours polaire agrandi) en attendant le modèle Blockbench.
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
