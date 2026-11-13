package com.evandouard.hxh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Parrot;

/** Aigle-araignée : modèle provisoire (perroquet gris agrandi ×2,5), en attendant le modèle Blockbench. */
public class SpiderEagleRenderer extends ParrotRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/parrot/parrot_grey.png");

    public SpiderEagleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6f;
    }

    @Override
    protected void scale(Parrot entity, PoseStack pose, float partialTick) {
        pose.scale(2.5f, 2.5f, 2.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(Parrot entity) {
        return TEXTURE;
    }
}
