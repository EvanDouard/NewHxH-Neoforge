package com.evandouard.hxh.mixin;

import com.evandouard.hxh.client.NenFxClient;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mort d'un mob : à la place du nuage de fumée blanche de Minecraft, une étoile d'encre qui se dissipe.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "makePoofParticles", at = @At("HEAD"), cancellable = true)
    private void hxh$inkPoof(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide) return;
        NenFxClient.deathPoof(self);
        ci.cancel();
    }
}
