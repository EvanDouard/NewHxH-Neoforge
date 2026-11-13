package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.nen.EnShape;
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

    /** Taille actuelle de l'En de chaque joueur (0 à 1 du rayon), animée : il se déploie, se rétracte. */
    private static final java.util.Map<java.util.UUID, Float> SCALE = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Float> PREVIOUS = new java.util.HashMap<>();

    private EnDomeClient() {}

    /**
     * Taille visée selon le mouvement : parfait à l'arrêt, réduit en marchant, rien en courant. En coupé : rien.
     */
    static float targetScale(Player p, HunterData d) {
        if (!d.en()) return 0f;
        if (p.isSprinting()) return 0f;
        double v = p.getDeltaMovement().horizontalDistance();
        return v > 0.03 ? 0.45f : 1f;
    }

    /** Une fois par tick : chaque En se déploie depuis le joueur, ou se rétracte (très vite quand on court). */
    public static void tick(Minecraft mc) {
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        for (Player p : mc.level.players()) {
            if (!p.hasData(ModAttachments.HUNTER)) continue;
            HunterData d = p.getData(ModAttachments.HUNTER);
            float cur = SCALE.getOrDefault(p.getUUID(), 0f);
            PREVIOUS.put(p.getUUID(), cur);
            float target = targetScale(p, d);
            float rate = target > cur ? 0.04f : (p.isSprinting() || !d.en() ? 0.25f : 0.08f);
            cur = target > cur ? Math.min(target, cur + rate) : Math.max(target, cur - rate);
            if (cur > 0f || d.en()) {
                SCALE.put(p.getUUID(), cur);
                seen.add(p.getUUID());
            }
        }
        SCALE.keySet().retainAll(seen);
        PREVIOUS.keySet().retainAll(seen);
    }

    /** Taille de l'En d'un joueur (0 à 1), lissée entre deux ticks. */
    public static float scale(Player p, float pt) {
        float cur = SCALE.getOrDefault(p.getUUID(), 0f);
        return Mth.lerp(pt, PREVIOUS.getOrDefault(p.getUUID(), cur), cur);
    }

    /** Rayon effectif de l'En du joueur maintenant (ce qu'il sent vraiment). */
    public static double effectiveRadius(Player p) {
        if (!p.hasData(ModAttachments.HUNTER)) return 0;
        return NenSenses.enRadius(p.getData(ModAttachments.HUNTER)) * SCALE.getOrDefault(p.getUUID(), 0f);
    }

    /** Forme effective de l'En du joueur maintenant (déployée selon le mouvement), ou null s'il est coupé. */
    @org.jetbrains.annotations.Nullable
    public static EnShape.Dims effectiveDims(Player p) {
        if (!p.hasData(ModAttachments.HUNTER)) return null;
        float k = SCALE.getOrDefault(p.getUUID(), 0f);
        if (k <= 0f) return null;
        return NenSenses.enDims(p.getData(ModAttachments.HUNTER)).scaled(k);
    }

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
            float k = scale(p, pt);
            EnShape.Dims dims = NenSenses.enDims(d).scaled(k);
            if (dims.radius() <= 0.15f) continue;
            int color = 0xFF000000 | d.auraColor();
            float time = mc.level.getGameTime() + pt;
            switch (dims.shape()) {
                case CUBE -> box(event.getPoseStack(), buffers, cam, p.getPosition(pt), dims, color, time);
                case CYLINDER -> cylinder(event.getPoseStack(), buffers, cam, p.getPosition(pt), dims, color, time);
                default -> dome(event.getPoseStack(), buffers, cam, p.getPosition(pt), (float) dims.radius(), color, time);
            }
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

    /** En en cube : faces translucides plus denses vues de biais, arêtes claires, carré encré au sol. */
    private static void box(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 base, EnShape.Dims d, int color, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(NenFxClient.WHITE));
        double a = d.radius();
        Vec3 lo = base.add(-a, -d.down(), -a), hi = base.add(a, d.up(), a);
        boolean inside = cam.x > lo.x && cam.x < hi.x && cam.y > lo.y && cam.y < hi.y && cam.z > lo.z && cam.z < hi.z;
        float dim = inside ? 0.6f : 1f;
        float sweep = (time % 60f) / 60f;
        int light = lerp(color, 0xFFFFFFFF, 0.5f);
        // quatre côtés découpés en bandes horizontales (bande qui balaie de bas en haut) + le dessus
        int bands = 10;
        double h = d.up() + d.down();
        for (int i = 0; i < bands; i++) {
            double y0 = lo.y + h * i / bands, y1 = lo.y + h * (i + 1) / bands;
            float band = Math.max(0f, 1f - Math.abs((float) i / bands - sweep) * 8f);
            float alpha = (0.16f + band * 0.25f) * dim;
            int c = lerp(color, 0xFFFFFFFF, band * 0.6f);
            quad(vc, pose, cam, new Vec3(lo.x, y0, lo.z), new Vec3(hi.x, y0, lo.z), new Vec3(hi.x, y1, lo.z), new Vec3(lo.x, y1, lo.z), c, alpha);
            quad(vc, pose, cam, new Vec3(lo.x, y0, hi.z), new Vec3(hi.x, y0, hi.z), new Vec3(hi.x, y1, hi.z), new Vec3(lo.x, y1, hi.z), c, alpha);
            quad(vc, pose, cam, new Vec3(lo.x, y0, lo.z), new Vec3(lo.x, y0, hi.z), new Vec3(lo.x, y1, hi.z), new Vec3(lo.x, y1, lo.z), c, alpha);
            quad(vc, pose, cam, new Vec3(hi.x, y0, lo.z), new Vec3(hi.x, y0, hi.z), new Vec3(hi.x, y1, hi.z), new Vec3(hi.x, y1, lo.z), c, alpha);
        }
        quad(vc, pose, cam, new Vec3(lo.x, hi.y, lo.z), new Vec3(hi.x, hi.y, lo.z), new Vec3(hi.x, hi.y, hi.z), new Vec3(lo.x, hi.y, hi.z),
                lerp(color, 0xFFFFFFFF, 0.15f), 0.14f * dim);
        // arêtes verticales et cadres haut / bas
        float e = 0.08f;
        for (double[] c : new double[][]{{lo.x, lo.z}, {hi.x, lo.z}, {hi.x, hi.z}, {lo.x, hi.z}}) {
            quad(vc, pose, cam, new Vec3(c[0] - e, lo.y, c[1]), new Vec3(c[0] + e, lo.y, c[1]), new Vec3(c[0] + e, hi.y, c[1]),
                    new Vec3(c[0] - e, hi.y, c[1]), light, 0.6f * dim);
            quad(vc, pose, cam, new Vec3(c[0], lo.y, c[1] - e), new Vec3(c[0], lo.y, c[1] + e), new Vec3(c[0], hi.y, c[1] + e),
                    new Vec3(c[0], hi.y, c[1] - e), light, 0.6f * dim);
        }
        frame(vc, pose, cam, lo.x, hi.x, lo.z, hi.z, hi.y, light, 0.6f * dim);
        frame(vc, pose, cam, lo.x, hi.x, lo.z, hi.z, base.y + 0.03, 0xFF111111, 0.85f * dim);
    }

    /** Cadre carré horizontal (bords d'un cube). */
    private static void frame(VertexConsumer vc, PoseStack pose, Vec3 cam, double x0, double x1, double z0, double z1, double y,
                              int color, float alpha) {
        double w = 0.15;
        quad(vc, pose, cam, new Vec3(x0, y, z0), new Vec3(x1, y, z0), new Vec3(x1, y, z0 + w), new Vec3(x0, y, z0 + w), color, alpha);
        quad(vc, pose, cam, new Vec3(x0, y, z1 - w), new Vec3(x1, y, z1 - w), new Vec3(x1, y, z1), new Vec3(x0, y, z1), color, alpha);
        quad(vc, pose, cam, new Vec3(x0, y, z0), new Vec3(x0 + w, y, z0), new Vec3(x0 + w, y, z1), new Vec3(x0, y, z1), color, alpha);
        quad(vc, pose, cam, new Vec3(x1 - w, y, z0), new Vec3(x1, y, z0), new Vec3(x1, y, z1), new Vec3(x1 - w, y, z1), color, alpha);
    }

    /** En en cylindre : paroi translucide (bord de bulle plus dense), anneaux clairs en haut et en bas, cercle au sol. */
    private static void cylinder(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 base, EnShape.Dims d, int color, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(NenFxClient.WHITE));
        float r = (float) d.radius();
        double y0 = base.y - d.down(), y1 = base.y + d.up();
        double hx = cam.x - base.x, hz = cam.z - base.z;
        boolean inside = hx * hx + hz * hz < r * r && cam.y > y0 && cam.y < y1;
        float dim = inside ? 0.6f : 1f;
        float sweep = (time % 60f) / 60f;
        int bands = 10;
        for (int i = 0; i < bands; i++) {
            double ya = y0 + (y1 - y0) * i / bands, yb = y0 + (y1 - y0) * (i + 1) / bands;
            float band = Math.max(0f, 1f - Math.abs((float) i / bands - sweep) * 8f);
            int c = lerp(color, 0xFFFFFFFF, band * 0.6f);
            for (int j = 0; j < SEGMENTS; j++) {
                float a0 = (float) j / SEGMENTS * Mth.TWO_PI, a1 = (float) (j + 1) / SEGMENTS * Mth.TWO_PI;
                Vec3 n = new Vec3(Mth.cos((a0 + a1) / 2), 0, Mth.sin((a0 + a1) / 2));
                Vec3 mid = base.add(n.scale(r)).add(0, (ya + yb) / 2 - base.y, 0);
                float facing = (float) Math.abs(n.dot(mid.subtract(cam).normalize()));
                float alpha = (0.12f + 0.5f * (1f - facing) * (1f - facing) + band * 0.25f) * dim;
                quad(vc, pose, cam, new Vec3(base.x + r * Mth.cos(a0), ya, base.z + r * Mth.sin(a0)),
                        new Vec3(base.x + r * Mth.cos(a1), ya, base.z + r * Mth.sin(a1)),
                        new Vec3(base.x + r * Mth.cos(a1), yb, base.z + r * Mth.sin(a1)),
                        new Vec3(base.x + r * Mth.cos(a0), yb, base.z + r * Mth.sin(a0)), c, alpha);
            }
        }
        int light = lerp(color, 0xFFFFFFFF, 0.5f);
        ring(vc, pose, cam, new Vec3(base.x, y1, base.z), r, 0.15f, light, 0.6f * dim);
        if (d.down() > 0.05) ring(vc, pose, cam, new Vec3(base.x, y0, base.z), r, 0.15f, light, 0.6f * dim);
        ring(vc, pose, cam, base.add(0, 0.03, 0), r, 0.18f, 0xFF111111, 0.85f * dim);
    }

    private static void quad(VertexConsumer vc, PoseStack pose, Vec3 cam, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
        vertex(vc, pose, cam, a, color, alpha);
        vertex(vc, pose, cam, b, color, alpha);
        vertex(vc, pose, cam, c, color, alpha);
        vertex(vc, pose, cam, d, color, alpha);
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
