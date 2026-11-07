package com.evandouard.hxh.mixin;

import com.evandouard.hxh.client.CombatAnimClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applique les animations de combat après l'animation normale du modèle humanoïde (joueurs, PNJ, adversaires).
 * Le modèle du joueur recopie ensuite les bras sur les manches, qui suivent donc le mouvement.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void hxh$combatAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                float netHeadYaw, float headPitch, CallbackInfo ci) {
        CombatAnimClient.applyToModel((HumanoidModel<?>) (Object) this, entity, ageInTicks, headPitch);
    }
}
