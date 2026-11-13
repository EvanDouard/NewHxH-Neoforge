package com.evandouard.hxh.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/** Singe à visage humain : modèle provisoire (corps humanoïde, texture de momie). */
public class ManFacedApeRenderer extends ZombieRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/husk.png");

    public ManFacedApeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return TEXTURE;
    }
}
