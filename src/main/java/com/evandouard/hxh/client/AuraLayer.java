package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.data.NenMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Lueur d'aura collée au corps : le modèle redessiné un peu agrandi, en lumière additive, sans texture.
 * C'est la base des deux styles (voir {@link AuraClient}) :
 * <ul>
 *     <li>vapeur (aura calme, au repos / Ten) : deux couches très douces ;</li>
 *     <li>flammes (Ren, Ken, aura sauvage) : une couche plus vive et plus serrée, les flammes font le reste.</li>
 * </ul>
 * Ryu poussé vers l'attaque : la lueur du corps s'efface (l'aura part dans les poings).
 */
public class AuraLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    /** Centre approximatif du corps dans l'espace du modèle (axe Y vers le bas, pieds à 1,5). */
    private static final float BODY_CENTER_Y = 0.75f;

    public AuraLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        AuraClient.Look look = AuraClient.look(entity);
        if (look == null) return;
        HunterData data = entity.getData(ModAttachments.HUNTER);
        float pulse = Mth.sin(ageInTicks * 0.15f) * 0.5f + 0.5f;
        float body = look.bodyVisibility();
        if (look.flames()) {
            float big = Math.max(0f, look.strength() - 1f); // Ren : enveloppe plus large
            shell(poseStack, buffers, look.color(), (0.22f + 0.08f * pulse) * Math.min(1f, look.strength()) * body, 1.06f + 0.1f * big);
            shell(poseStack, buffers, look.color(), 0.12f * Math.min(1f, look.strength()) * body, 1.14f + 0.25f * big + 0.02f * pulse);
        } else {
            float k = data.mode() == NenMode.TEN ? 1f : 0.6f;
            shell(poseStack, buffers, look.color(), (0.10f + 0.04f * pulse) * k * body, 1.08f);
            shell(poseStack, buffers, look.color(), (0.07f + 0.03f * pulse) * k * body, 1.16f + 0.02f * pulse);
        }
    }

    private void shell(PoseStack poseStack, MultiBufferSource buffers, int rgb, float intensity, float scale) {
        if (intensity <= 0.01f) return;
        int r = (int) (((rgb >> 16) & 0xFF) * intensity);
        int g = (int) (((rgb >> 8) & 0xFF) * intensity);
        int b = (int) ((rgb & 0xFF) * intensity);
        VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(NenFxClient.WHITE));
        poseStack.pushPose();
        poseStack.translate(0, BODY_CENTER_Y, 0);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -BODY_CENTER_Y, 0);
        getParentModel().renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color(255, r, g, b));
        poseStack.popPose();
    }

    /** Ajoute la couche d'aura aux joueurs et à tous les mobs vivants. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addToRenderers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer living) living.addLayer(new AuraLayer(living));
        }
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof LivingEntityRenderer living) living.addLayer(new AuraLayer(living));
        }
    }
}
