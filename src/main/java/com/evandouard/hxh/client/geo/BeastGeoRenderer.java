package com.evandouard.hxh.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Rendu GeckoLib des bêtes (renard-ours, Mike, Great Stamp, aigle-araignée, singe à visage humain, fourmi-chimère,
 * Kiriko). GeckoLib applique déjà l'attribut d'échelle de l'entité ; on ajoute la demi-taille des petits et, au
 * besoin, un facteur propre au modèle (Mike : modèle à taille réelle, compensé de son attribut ×3).
 */
public class BeastGeoRenderer<T extends LivingEntity & GeoAnimatable> extends GeoEntityRenderer<T> {
    private final float modelScale;

    public BeastGeoRenderer(EntityRendererProvider.Context context, float shadow, float modelScale) {
        super(context, new BeastGeoModel<>());
        this.shadowRadius = shadow;
        this.modelScale = modelScale;
    }

    public static <T extends LivingEntity & GeoAnimatable> EntityRendererProvider<T> of(float shadow) {
        return ctx -> new BeastGeoRenderer<>(ctx, shadow, 1f);
    }

    public static <T extends LivingEntity & GeoAnimatable> EntityRendererProvider<T> of(float shadow, float modelScale) {
        return ctx -> new BeastGeoRenderer<>(ctx, shadow, modelScale);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight) {
        float s = modelScale * (entity.isBaby() ? 0.5f : 1f);
        if (s == 1f) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }
        poseStack.pushPose();
        poseStack.scale(s, s, s);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
