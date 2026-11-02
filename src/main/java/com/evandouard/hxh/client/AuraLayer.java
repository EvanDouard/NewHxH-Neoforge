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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Aura visible autour de toute entité qui maîtrise le Nen, façon « lueur d'enchantement » :
 * le modèle est redessiné légèrement agrandi avec une texture tourbillonnante, en mélange additif
 * et pleine luminosité, donc il brille même dans le noir.
 * <ul>
 *     <li>Neutre : aura douce et constante</li>
 *     <li>Ten : plus nette, collée au corps</li>
 *     <li>Ren : intense, plus large, elle pulse</li>
 *     <li>Zetsu : aucune aura</li>
 *     <li>Soin : l'aura vire au vert</li>
 * </ul>
 */
public class AuraLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation SWIRL =
            ResourceLocation.fromNamespaceAndPath("hxh", "textures/entity/aura_swirl.png");
    /** Centre approximatif du corps dans l'espace du modèle (axe Y vers le bas, pieds à 1,5). */
    private static final float BODY_CENTER_Y = 0.75f;

    public AuraLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || !entity.hasData(ModAttachments.HUNTER)) return;
        HunterData data = entity.getData(ModAttachments.HUNTER);
        if (!data.nenUnlocked() || data.mode() == NenMode.ZETSU) return;

        float pulse = Mth.sin(ageInTicks * 0.25f) * 0.5f + 0.5f;
        float intensity;
        float scale;
        switch (data.mode()) {
            case TEN -> { intensity = 0.8f; scale = 1.04f; }
            case REN -> { intensity = 0.9f + 0.1f * pulse; scale = 1.12f + 0.03f * pulse; }
            default -> { intensity = 0.6f; scale = 1.06f; }
        }

        int rgb = data.healing() ? 0x7CFF8A : data.auraColor();
        int r = (int) (((rgb >> 16) & 0xFF) * intensity);
        int g = (int) (((rgb >> 8) & 0xFF) * intensity);
        int b = (int) ((rgb & 0xFF) * intensity);
        int color = FastColor.ARGB32.color(255, r, g, b);

        float scroll = ageInTicks * 0.01f;
        VertexConsumer consumer = buffers.getBuffer(RenderType.energySwirl(SWIRL, scroll % 1f, (scroll * 0.7f) % 1f));

        poseStack.pushPose();
        poseStack.translate(0, BODY_CENTER_Y, 0);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -BODY_CENTER_Y, 0);
        getParentModel().renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
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
