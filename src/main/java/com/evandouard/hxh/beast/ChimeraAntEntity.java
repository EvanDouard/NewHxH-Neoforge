package com.evandouard.hxh.beast;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Fourmi-Chimère soldat : chasse de jour comme de nuit (contrairement à l'araignée), grimpe aux murs, solide.
 * Une partie d'entre elles maîtrise le Nen (mob d'exemple naturel {@code fourmi_chimere_soldat}, avec prime).
 * Modèle provisoire (araignée agrandie) en attendant le modèle Blockbench ; stats à l'équilibrage final.
 */
public class ChimeraAntEntity extends Spider {
    public ChimeraAntEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    public static boolean canSpawn(EntityType<ChimeraAntEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                   RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && checkMobSpawnRules(type, level, reason, pos, random);
    }

    /** Toujours hostile (l'araignée de Minecraft ne chasse que dans le noir). */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    /** Pour les tests : la fourmi chasse-t-elle en plein jour ? (ses buts de cible ne dépendent pas de la lumière) */
    public boolean huntsInDaylight() {
        return true;
    }
}
