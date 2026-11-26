package com.evandouard.hxh.client.geo;

import com.evandouard.hxh.summon.NenSummonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Rendu des invocations : modèle GeckoLib à la taille voulue par la technique, translucide (fondu d'entrée et de
 * sortie), teinté par la couleur d'aura, toujours en pleine lumière (c'est de l'aura). Pot-Clean affiche en plus le
 * compteur de la dette sur sa pancarte.
 */
public class SummonGeoRenderer extends GeoEntityRenderer<NenSummonEntity> {
    public SummonGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new SummonGeoModel());
        this.shadowRadius = 0f;
    }

    @Override
    public void render(NenSummonEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (entity.alpha(partialTick) <= 0.01f) return;
        poseStack.pushPose();
        float s = entity.scale();
        // GeckoLib n'oriente que les LivingEntity : on applique ici le lacet de l'invocation (et l'inclinaison du dragon
        // qui pique) ; GeckoLib ajoute ensuite son demi-tour fixe
        float yaw = net.minecraft.util.Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = net.minecraft.util.Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.translate(0, 0.4 * s, 0);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        if (Math.abs(pitch) > 0.5f) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-pitch));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-180f));
        }
        poseStack.translate(0, -0.4 * s, 0);
        poseStack.scale(s, s, s);
        int c = entity.color();
        float a = ((c >>> 24) & 0xFF) / 255f * entity.alpha(partialTick);
        MultiBufferSource tinted = type -> new Tint(bufferSource.getBuffer(type), ((c >> 16) & 0xFF) / 255f,
                ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, a);
        super.render(entity, entityYaw, partialTick, poseStack, tinted, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        String label = entity.label();
        if (!label.isEmpty()) {
            // chiffres de la dette sur la pancarte de Pot-Clean (devant lui, à mi-hauteur), toujours face à la caméra
            poseStack.pushPose();
            poseStack.translate(0, 0.32 * s, 0);
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            float k = 0.012f * s;
            poseStack.scale(k, -k, k);
            Font font = getFont();
            float x = -font.width(label) / 2f;
            font.drawInBatch(label, x, 0, 0xFF1A1A22, false, poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.SEE_THROUGH, 0x00000000, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
    }

    @Override
    public RenderType getRenderType(NenSummonEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    /** Multiplie la couleur de chaque sommet (teinte d'aura et fondu), sans dépendre de l'API de couleur de GeckoLib. */
    private record Tint(VertexConsumer to, float r, float g, float b, float a) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            to.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            to.setColor(Math.round(red * r), Math.round(green * g), Math.round(blue * b), Math.round(alpha * a));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            to.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            to.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            to.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            to.setNormal(x, y, z);
            return this;
        }
    }
}
