package com.evandouard.hxh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Parrot;

/** Kiriko : modèle provisoire (perroquet vert agrandi ×3), en attendant le modèle Blockbench. */
public class KirikoRenderer extends ParrotRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/parrot/parrot_green.png");

    public KirikoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.8f;
    }

    @Override
    protected void scale(Parrot entity, PoseStack pose, float partialTick) {
        pose.scale(3f, 3f, 3f);
    }

    @Override
    public ResourceLocation getTextureLocation(Parrot entity) {
        return TEXTURE;
    }
}
