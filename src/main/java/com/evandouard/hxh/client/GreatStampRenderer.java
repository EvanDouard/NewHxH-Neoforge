package com.evandouard.hxh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HoglinRenderer;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

/** Great Stamp : modèle provisoire (hoglin agrandi), en attendant le modèle Blockbench. */
public class GreatStampRenderer extends HoglinRenderer {
    public GreatStampRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Hoglin entity, PoseStack pose, float partialTick) {
        float s = entity.isBaby() ? 1f : 1.4f;
        pose.scale(s, s, s);
    }
}
