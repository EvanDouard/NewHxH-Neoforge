package com.evandouard.hxh.client.look;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.function.Function;

/**
 * Dessine les cubes en plus d'une apparence (cheveux, barbe, ventre…) en suivant les membres animés du modèle :
 * ils bougent avec la tête, les bras, les jambes, y compris pendant les animations de combat.
 */
public class CharacterExtrasLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private final Function<T, CharacterLook> lookOf;

    public CharacterExtrasLayer(RenderLayerParent<T, M> parent, Function<T, CharacterLook> lookOf) {
        super(parent);
        this.lookOf = lookOf;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        CharacterLook look = lookOf.apply(entity);
        if (look == null || look.extras().isEmpty() || look.extrasTexture() == null) return;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(look.extrasTexture()));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0f);
        M model = getParentModel();
        for (Map.Entry<String, ModelPart> e : look.extras().entrySet()) {
            ModelPart bone = bone(model, e.getKey());
            if (bone == null || !bone.visible) continue;
            poseStack.pushPose();
            bone.translateAndRotate(poseStack);
            e.getValue().render(poseStack, consumer, packedLight, overlay);
            poseStack.popPose();
        }
    }

    private static ModelPart bone(HumanoidModel<?> model, String part) {
        return switch (part) {
            case "head" -> model.head;
            case "body" -> model.body;
            case "right_arm" -> model.rightArm;
            case "left_arm" -> model.leftArm;
            case "right_leg" -> model.rightLeg;
            case "left_leg" -> model.leftLeg;
            default -> null;
        };
    }
}
