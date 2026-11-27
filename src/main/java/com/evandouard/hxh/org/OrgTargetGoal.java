package com.evandouard.hxh.org;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Cible à vue les membres des organisations ennemies d'un mob dont l'organisation est « hostile à vue »
 * (Fourmis-Chimères contre les utilisateurs de Nen). Les autres mobs n'ont pas ce but.
 */
public class OrgTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
    public OrgTargetGoal(Mob mob) {
        super(mob, LivingEntity.class, 10, true, false, target -> Organizations.attacksOnSight(mob, target));
    }
}
