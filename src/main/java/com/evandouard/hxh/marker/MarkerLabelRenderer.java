package com.evandouard.hxh.marker;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/** Étiquette flottante au-dessus d'un repère : le PNJ choisi ou le numéro du point de parcours. */
public class MarkerLabelRenderer implements BlockEntityRenderer<MarkerBlockEntity> {
    public MarkerLabelRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MarkerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Component label = be.getBlockState().getBlock() instanceof MarkerBlock m && m.kind() == MarkerBlock.Kind.WAYPOINT
                ? Component.translatable("label.hxh.waypoint", be.index())
                : Component.translatable("npc.hxh." + be.npc());
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        pose.pushPose();
        pose.translate(0.5, 1.35, 0.5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025f, -0.025f, 0.025f);
        Matrix4f matrix = pose.last().pose();
        float x = -font.width(label) / 2f;
        int background = (int) (mc.options.getBackgroundOpacity(0.25f) * 255f) << 24;
        font.drawInBatch(label, x, 0, 0xFFFFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, 0xF000F0);
        pose.popPose();
    }
}
