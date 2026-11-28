package com.evandouard.hxh.beast;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Animations GeckoLib communes aux bêtes du bestiaire (modèles {@code beast_<id>} générés par tools/models/beast_*.py) :
 * {@code idle}, {@code walk}, {@code fly} (oiseaux en vol) et {@code attack} (joué quand la bête frappe).
 */
public final class BeastAnimations {
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    public static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private BeastAnimations() {}

    public static <T extends LivingEntity & GeoEntity> void register(T beast, AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(beast, "base", 5, state -> {
            if (beast instanceof Parrot p && p.isFlying()) return state.setAndContinue(FLY);
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>(beast, "attack", 2, state -> {
            boolean hitting = beast.swinging || beast instanceof Hoglin h && h.getAttackAnimationRemainingTicks() > 0;
            if (hitting) return state.setAndContinue(ATTACK);
            state.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }
}
