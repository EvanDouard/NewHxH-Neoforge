package com.evandouard.hxh.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Formes 3D dessinées à la main (sans modèle Blockbench) pour les attaques : sphères déformables, tubes qui suivent une
 * courbe, anneaux (tores), cônes. Ombrage doux calculé ici (lumière venant d'en haut à gauche, reflet sur les bords),
 * en couleur pleine ou en lumière additive (aura).
 * <p>
 * Toutes les positions sont dans le monde ; {@code cam} est la position de la caméra.
 */
public final class Mesh3D {
    private Mesh3D() {}

    /** Lumière principale (normalisée) : d'en haut, un peu de côté. */
    private static final Vec3 LIGHT_DIR = new Vec3(-0.4, 0.85, 0.35).normalize();
    private static final int FULL_BRIGHT = 0xF000F0;

    /** Déformation d'une surface : rayon multiplié selon la direction et le temps. */
    @FunctionalInterface
    public interface Warp {
        float scale(Vec3 dir);
    }

    /**
     * Où et comment dessiner : pile de transformations, tampons, {@code cam} (soustraite à chaque point : la caméra en
     * coordonnées monde, ou zéro si la pile est déjà centrée sur l'objet) et {@code eye} (d'où on regarde, dans le même
     * repère que les points, pour l'ombrage).
     */
    public record Ctx(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 eye) {
        public Ctx(PoseStack pose, MultiBufferSource buffers, Vec3 cam) {
            this(pose, buffers, cam, cam);
        }
    }

    /** Couche opaque-translucide ombrée (objets solides) ; {@link #glow} pour l'aura (additive). */
    public static VertexConsumer solid(Ctx c) {
        return c.buffers().getBuffer(RenderType.entityTranslucent(NenFxClient.WHITE));
    }

    public static VertexConsumer glow(Ctx c) {
        return new Premultiplied(c.buffers().getBuffer(RenderType.eyes(NenFxClient.WHITE)));
    }

    /**
     * La couche lumineuse est additive (ONE, ONE) : l'alpha y est ignoré. On multiplie donc la couleur par l'alpha,
     * sinon fondus, enveloppes « fresnel » et couches faibles brillent à pleine intensité et tout sature en blanc.
     */
    private static final class Premultiplied implements VertexConsumer {
        private final VertexConsumer d;

        Premultiplied(VertexConsumer d) {
            this.d = d;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            d.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            d.setColor(r * a / 255, g * a / 255, b * a / 255, 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            d.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            d.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            d.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            d.setNormal(x, y, z);
            return this;
        }
    }

    /** Couleur ombrée : diffuse + ambiante, et un liseré clair sur les bords vus de la caméra (rim). */
    static int shade(int rgb, Vec3 normal, Vec3 view, boolean lit) {
        if (!lit) return rgb & 0xFFFFFF;
        float diffuse = (float) Math.max(0, normal.dot(LIGHT_DIR));
        float rim = (float) Math.pow(1 - Math.abs(normal.dot(view)), 3);
        float k = 0.45f + 0.55f * diffuse;
        int r = (int) Mth.clamp(((rgb >> 16) & 255) * k + 255 * rim * 0.35f, 0, 255);
        int g = (int) Mth.clamp(((rgb >> 8) & 255) * k + 255 * rim * 0.35f, 0, 255);
        int b = (int) Mth.clamp((rgb & 255) * k + 255 * rim * 0.35f, 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static void v(VertexConsumer vc, Ctx c, Vec3 p, int rgb, float alpha) {
        PoseStack.Pose last = c.pose().last();
        vc.addVertex(last.pose(), (float) (p.x - c.cam().x), (float) (p.y - c.cam().y), (float) (p.z - c.cam().z))
                .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Mth.clamp((int) (alpha * 255), 0, 255))
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(last, 0f, 1f, 0f);
    }

    /**
     * Sphère (déformable) : {@code rings} × {@code segs} facettes. En mode {@code fresnel}, le centre est presque
     * transparent et les bords denses (bulle, enveloppe d'énergie).
     */
    public static void sphere(Ctx c, VertexConsumer vc, Vec3 center, float radius, int rgb, float alpha, int rings, int segs,
                              Warp warp, boolean lit, boolean fresnel) {
        for (int i = 0; i < rings; i++) {
            float la0 = -Mth.HALF_PI + Mth.PI * i / rings, la1 = -Mth.HALF_PI + Mth.PI * (i + 1) / rings;
            for (int j = 0; j < segs; j++) {
                float lo0 = Mth.TWO_PI * j / segs, lo1 = Mth.TWO_PI * (j + 1) / segs;
                Vec3 d00 = dir(la0, lo0), d01 = dir(la0, lo1), d11 = dir(la1, lo1), d10 = dir(la1, lo0);
                Vec3 p00 = center.add(d00.scale(radius * (warp == null ? 1 : warp.scale(d00))));
                Vec3 p01 = center.add(d01.scale(radius * (warp == null ? 1 : warp.scale(d01))));
                Vec3 p11 = center.add(d11.scale(radius * (warp == null ? 1 : warp.scale(d11))));
                Vec3 p10 = center.add(d10.scale(radius * (warp == null ? 1 : warp.scale(d10))));
                quadShaded(c, vc, p00, p01, p11, p10, d00, d01, d11, d10, rgb, alpha, lit, fresnel);
            }
        }
    }

    private static void quadShaded(Ctx c, VertexConsumer vc, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                   Vec3 n0, Vec3 n1, Vec3 n2, Vec3 n3, int rgb, float alpha, boolean lit, boolean fresnel) {
        Vec3[] p = {p0, p1, p2, p3};
        Vec3[] n = {n0, n1, n2, n3};
        for (int k = 0; k < 4; k++) {
            Vec3 view = c.eye().subtract(p[k]).normalize();
            float a = alpha;
            if (fresnel) {
                float edge = (float) (1 - Math.abs(n[k].dot(view)));
                a = alpha * (0.12f + 0.88f * edge * edge);
            }
            v(vc, c, p[k], shade(rgb, n[k], view, lit), a);
        }
    }

    private static Vec3 dir(float lat, float lon) {
        float cl = Mth.cos(lat);
        return new Vec3(cl * Mth.cos(lon), Mth.sin(lat), cl * Mth.sin(lon));
    }

    /** Deux vecteurs perpendiculaires à {@code axis} (normés). */
    static Vec3[] basis(Vec3 axis) {
        Vec3 a = axis.normalize();
        Vec3 u = Math.abs(a.y) < 0.95 ? a.cross(new Vec3(0, 1, 0)) : a.cross(new Vec3(1, 0, 0));
        u = u.normalize();
        Vec3 w = u.cross(a).normalize();
        return new Vec3[]{u, w};
    }

    /** Tore (anneau d'énergie) autour de {@code axis}. */
    public static void torus(Ctx c, VertexConsumer vc, Vec3 center, Vec3 axis, float major, float minor, int rgb, float alpha,
                             int segs, int sides, boolean lit) {
        Vec3[] b = basis(axis);
        Vec3 ax = axis.normalize();
        for (int i = 0; i < segs; i++) {
            float t0 = Mth.TWO_PI * i / segs, t1 = Mth.TWO_PI * (i + 1) / segs;
            Vec3 r0 = b[0].scale(Mth.cos(t0)).add(b[1].scale(Mth.sin(t0)));
            Vec3 r1 = b[0].scale(Mth.cos(t1)).add(b[1].scale(Mth.sin(t1)));
            for (int j = 0; j < sides; j++) {
                float s0 = Mth.TWO_PI * j / sides, s1 = Mth.TWO_PI * (j + 1) / sides;
                Vec3 n00 = r0.scale(Mth.cos(s0)).add(ax.scale(Mth.sin(s0)));
                Vec3 n01 = r0.scale(Mth.cos(s1)).add(ax.scale(Mth.sin(s1)));
                Vec3 n11 = r1.scale(Mth.cos(s1)).add(ax.scale(Mth.sin(s1)));
                Vec3 n10 = r1.scale(Mth.cos(s0)).add(ax.scale(Mth.sin(s0)));
                quadShaded(c, vc,
                        center.add(r0.scale(major)).add(n00.scale(minor)), center.add(r0.scale(major)).add(n01.scale(minor)),
                        center.add(r1.scale(major)).add(n11.scale(minor)), center.add(r1.scale(major)).add(n10.scale(minor)),
                        n00, n01, n11, n10, rgb, alpha, lit, false);
            }
        }
    }

    /** Rayon le long d'un tube : {@code t} va de 0 (début) à 1 (bout). */
    @FunctionalInterface
    public interface Profile {
        float radius(float t);
    }

    /** Tube qui suit des points (fil, gomme, chaîne lisse), rayon constant ou décroissant vers le bout. */
    public static void tube(Ctx c, VertexConsumer vc, List<Vec3> path, float radius, float endRadius, int rgb, float alpha,
                            int sides, boolean lit) {
        tube(c, vc, path, t -> Mth.lerp(t, radius, endRadius), rgb, alpha, sides, lit);
    }

    /** Tube au profil libre (lame effilée aux deux bouts, aiguille…). */
    public static void tube(Ctx c, VertexConsumer vc, List<Vec3> path, Profile profile, int rgb, float alpha,
                            int sides, boolean lit) {
        tube(c, vc, path, profile, rgb, alpha, sides, lit, false);
    }

    /** Tube au profil libre ; en mode {@code fresnel}, bords denses et cœur transparent (langue de flamme). */
    public static void tube(Ctx c, VertexConsumer vc, List<Vec3> path, Profile profile, int rgb, float alpha,
                            int sides, boolean lit, boolean fresnel) {
        if (path.size() < 2) return;
        Vec3[] prevRing = null;
        Vec3[] prevNormals = null;
        for (int i = 0; i < path.size(); i++) {
            Vec3 p = path.get(i);
            Vec3 tangent = (i < path.size() - 1 ? path.get(i + 1).subtract(p) : p.subtract(path.get(i - 1)));
            if (tangent.lengthSqr() < 1e-8) tangent = new Vec3(0, 0, 1);
            Vec3[] b = basis(tangent);
            float r = profile.radius((float) i / (path.size() - 1));
            Vec3[] ring = new Vec3[sides];
            Vec3[] normals = new Vec3[sides];
            for (int k = 0; k < sides; k++) {
                float a = Mth.TWO_PI * k / sides;
                normals[k] = b[0].scale(Mth.cos(a)).add(b[1].scale(Mth.sin(a)));
                ring[k] = p.add(normals[k].scale(r));
            }
            if (prevRing != null) {
                for (int k = 0; k < sides; k++) {
                    int k1 = (k + 1) % sides;
                    quadShaded(c, vc, prevRing[k], prevRing[k1], ring[k1], ring[k],
                            prevNormals[k], prevNormals[k1], normals[k1], normals[k], rgb, alpha, lit, fresnel);
                }
            }
            prevRing = ring;
            prevNormals = normals;
        }
    }

    /** Cône (pointe en {@code tip}), pour les lames, les crocs, les pointes d'aiguille. */
    public static void cone(Ctx c, VertexConsumer vc, Vec3 base, Vec3 tip, float radius, int rgb, float alpha, int sides, boolean lit) {
        Vec3 axis = tip.subtract(base);
        Vec3[] b = basis(axis);
        Vec3 ax = axis.normalize();
        for (int k = 0; k < sides; k++) {
            float a0 = Mth.TWO_PI * k / sides, a1 = Mth.TWO_PI * (k + 1) / sides;
            Vec3 n0 = b[0].scale(Mth.cos(a0)).add(b[1].scale(Mth.sin(a0)));
            Vec3 n1 = b[0].scale(Mth.cos(a1)).add(b[1].scale(Mth.sin(a1)));
            Vec3 q0 = base.add(n0.scale(radius)), q1 = base.add(n1.scale(radius));
            Vec3 sn0 = n0.add(ax.scale(0.4)).normalize(), sn1 = n1.add(ax.scale(0.4)).normalize();
            quadShaded(c, vc, q0, q1, tip, tip, sn0, sn1, ax, ax, rgb, alpha, lit, false);
        }
    }
}
