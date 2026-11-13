package com.evandouard.hxh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Spider;

/** Fourmi-Chimère soldat : modèle provisoire (araignée des cavernes agrandie ×1,5). */
public class ChimeraAntRenderer extends SpiderRenderer<Spider> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/spider/cave_spider.png");

    public ChimeraAntRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0f;
    }

    @Override
    protected void scale(Spider entity, PoseStack pose, float partialTick) {
        pose.scale(1.5f, 1.5f, 1.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(Spider entity) {
        return TEXTURE;
    }
}
