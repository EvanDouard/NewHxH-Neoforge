package com.evandouard.hxh.mixin;

import com.evandouard.hxh.client.SensesClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Contour des entités senties par l'En ou le Gyo : elles brillent, dans la couleur d'aura du joueur au lieu du blanc de Minecraft.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void hxh$senseColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide) return;
        int color = SensesClient.outlineColor(self);
        if (color != -1) cir.setReturnValue(color);
    }

    /** Les entités senties par l'En (dès le niveau 0) ou le Gyo brillent pour le joueur local seulement. */
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void hxh$sensedGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide && SensesClient.sensed(self)) cir.setReturnValue(true);
    }
}
