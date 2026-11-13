package com.evandouard.hxh.client;

import com.evandouard.hxh.beast.FoxbearEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renard-ours : modèle provisoire (ours polaire agrandi), en attendant le modèle Blockbench. */
public class FoxbearRenderer extends MobRenderer<FoxbearEntity, PolarBearModel<FoxbearEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/bear/polarbear.png");

    public FoxbearRenderer(EntityRendererProvider.Context context) {
        super(context, new PolarBearModel<>(context.bakeLayer(ModelLayers.POLAR_BEAR)), 1.2f);
    }

    @Override
    protected void scale(FoxbearEntity entity, PoseStack pose, float partialTick) {
        float s = entity.isBaby() ? 1f : 1.35f;
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(FoxbearEntity entity) {
        return TEXTURE;
    }
}
