package com.evandouard.hxh.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Types de rendu propres au mod. Sous-classe de {@link RenderType} uniquement pour accéder à ses états protégés.
 */
public final class HxHRenderTypes extends RenderType {
    /** Lumière additive sans test de profondeur : se voit à travers les murs (repérage). */
    private static final RenderType XRAY_GLOW = create("hxh_xray_glow", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(NenFxClient.WHITE, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private HxHRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling,
                           boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    public static RenderType xrayGlow() {
        return XRAY_GLOW;
    }
}
