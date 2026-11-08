package com.evandouard.hxh.client;

import com.evandouard.hxh.hatsu.NenProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Dessine les projectiles de Nen : boule d'aura cernée de noir (Pierre), lame en croissant (Ciseaux). */
public class NenProjectileRenderer extends EntityRenderer<NenProjectile> {
    public NenProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(NenProjectile e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + pt;
        float r = e.radius();
        int color = e.color();
        pose.pushPose();
        pose.translate(0, e.getBbHeight() / 2, 0);
        if (e.kind() == NenProjectile.ORB) {
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            // halo, boule pulsante, et une étoile d'énergie qui tourne derrière
            quad(pose, buffers.getBuffer(RenderType.eyes(NenFxClient.GLOW)), r * 2.4f, 0, color, 200);
            quad(pose, buffers.getBuffer(RenderType.entityTranslucent(NenFxClient.IMPACT)), r * 1.5f, age * 0.25f, color, 170);
            float pulse = 1f + 0.06f * Mth.sin(age * 0.9f);
            quad(pose, buffers.getBuffer(RenderType.entityTranslucent(NenFxClient.ORB)), r * pulse, 0, color, 255);
        } else {
            // Croissant à plat, pointe vers l'avant, légèrement incliné
            pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(90 + e.getXRot()));
            pose.mulPose(Axis.ZP.rotationDegrees(-45));
            quad(pose, buffers.getBuffer(RenderType.eyes(NenFxClient.GLOW)), 1.3f, 0, color, 160);
            quad(pose, buffers.getBuffer(RenderType.entityTranslucent(NenFxClient.SLASH)), 1.0f, 0, color, 255);
        }
        pose.popPose();
        super.render(e, yaw, pt, pose, buffers, light);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float half, float angle, int color, int alpha) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotation(angle));
        Matrix4f m = pose.last().pose();
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        float[][] c = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (float[] p : c) {
            vc.addVertex(m, p[0] * half, p[1] * half, 0).setColor(r, g, b, alpha).setUv(p[2], p[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose.last(), 0, 0, 1);
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(NenProjectile entity) {
        return NenFxClient.ORB;
    }
}
