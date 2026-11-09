package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.nen.NenSenses;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Dôme d'En : une vraie coupole dans la couleur d'aura de celui qui l'étend (comme Pitou autour du palais).
 * Translucide au milieu, plus dense sur les bords (effet de bulle), une bande claire qui balaie le dôme de bas en
 * haut, et un cercle encré au sol. Vu de l'intérieur, il est beaucoup plus discret pour ne pas gêner.
 */
public final class EnDomeClient {
    private static final int LIGHT = 0xF000F0;
    private static final int RINGS = 14, SEGMENTS = 40;

    private EnDomeClient() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, EnDomeClient::onRender);
    }

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;
        for (Player p : mc.level.players()) {
            if (!p.hasData(ModAttachments.HUNTER)) continue;
            HunterData d = p.getData(ModAttachments.HUNTER);
            if (!d.en()) continue;
            float r = (float) NenSenses.enRadius(d);
            if (r <= 0.5f) continue;
            dome(event.getPoseStack(), buffers, cam, p.getPosition(pt), r, 0xFF000000 | d.auraColor(),
                    mc.level.getGameTime() + pt);
            drew = true;
        }
        if (drew) buffers.endBatch();
    }

    private static void dome(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 base, float r, int color, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(NenFxClient.WHITE));
        boolean inside = cam.distanceTo(base) < r;
        float dim = inside ? 0.7f : 1f;
        // Bande qui balaie le dôme de bas en haut (un tour toutes les 3 s)
        float sweep = (time % 60f) / 60f;
        for (int i = 0; i < RINGS; i++) {
            float lat0 = (float) i / RINGS * Mth.HALF_PI, lat1 = (float) (i + 1) / RINGS * Mth.HALF_PI;
            for (int j = 0; j < SEGMENTS; j++) {
                float lon0 = (float) j / SEGMENTS * Mth.TWO_PI, lon1 = (float) (j + 1) / SEGMENTS * Mth.TWO_PI;
                Vec3 p00 = point(base, r, lat0, lon0), p01 = point(base, r, lat0, lon1);
                Vec3 p11 = point(base, r, lat1, lon1), p10 = point(base, r, lat1, lon0);
                Vec3 mid = point(base, r, (lat0 + lat1) / 2, (lon0 + lon1) / 2);
                Vec3 n = mid.subtract(base).normalize();
                Vec3 view = mid.subtract(cam).normalize();
                float facing = (float) Math.abs(n.dot(view));
                // bord de bulle plus dense, centre presque transparent
                float alpha = 0.12f + 0.55f * (1f - facing) * (1f - facing);
                float h = (float) i / RINGS;
                float band = Math.max(0f, 1f - Math.abs(h - sweep) * 8f);
                alpha += band * 0.25f;
                // haut du dôme un peu plus clair
                int c = lerp(color, 0xFFFFFFFF, band * 0.6f + h * 0.15f);
                alpha *= dim;
                vertex(vc, pose, cam, p00, c, alpha);
                vertex(vc, pose, cam, p01, c, alpha);
                vertex(vc, pose, cam, p11, c, alpha);
                vertex(vc, pose, cam, p10, c, alpha);
            }
        }
        // Deux parallèles plus clairs : on lit la forme du dôme même vu de l'intérieur
        for (float lat : new float[]{0.45f, 0.95f}) {
            int light = lerp(color, 0xFFFFFFFF, 0.5f);
            for (int j = 0; j < SEGMENTS; j++) {
                float lon0 = (float) j / SEGMENTS * Mth.TWO_PI, lon1 = (float) (j + 1) / SEGMENTS * Mth.TWO_PI;
                vertex(vc, pose, cam, point(base, r, lat, lon0), light, 0.45f * dim);
                vertex(vc, pose, cam, point(base, r, lat, lon1), light, 0.45f * dim);
                vertex(vc, pose, cam, point(base, r, lat + 0.025f, lon1), light, 0.45f * dim);
                vertex(vc, pose, cam, point(base, r, lat + 0.025f, lon0), light, 0.45f * dim);
            }
        }
        // Cercle encré au sol + liseré de couleur juste au-dessus
        ring(vc, pose, cam, base.add(0, 0.03, 0), r, 0.18f, 0xFF111111, 0.85f * dim);
        ring(vc, pose, cam, base.add(0, 0.04, 0), r - 0.18f, 0.12f, color, 0.8f * dim);
    }

    private static Vec3 point(Vec3 base, float r, float lat, float lon) {
        float cl = Mth.cos(lat);
        return base.add(r * cl * Mth.cos(lon), r * Mth.sin(lat), r * cl * Mth.sin(lon));
    }

    private static void ring(VertexConsumer vc, PoseStack pose, Vec3 cam, Vec3 c, float r, float width, int color, float alpha) {
        for (int j = 0; j < SEGMENTS * 2; j++) {
            float a0 = (float) j / (SEGMENTS * 2) * Mth.TWO_PI, a1 = (float) (j + 1) / (SEGMENTS * 2) * Mth.TWO_PI;
            float ro = r, ri = r - width;
            vertex(vc, pose, cam, c.add(ro * Mth.cos(a0), 0, ro * Mth.sin(a0)), color, alpha);
            vertex(vc, pose, cam, c.add(ro * Mth.cos(a1), 0, ro * Mth.sin(a1)), color, alpha);
            vertex(vc, pose, cam, c.add(ri * Mth.cos(a1), 0, ri * Mth.sin(a1)), color, alpha);
            vertex(vc, pose, cam, c.add(ri * Mth.cos(a0), 0, ri * Mth.sin(a0)), color, alpha);
        }
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static void vertex(VertexConsumer vc, PoseStack pose, Vec3 cam, Vec3 p, int color, float alpha) {
        PoseStack.Pose last = pose.last();
        vc.addVertex(last.pose(), (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, Mth.clamp((int) (alpha * 255), 0, 255))
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LIGHT)
                .setNormal(last, 0f, 1f, 0f);
    }
}
