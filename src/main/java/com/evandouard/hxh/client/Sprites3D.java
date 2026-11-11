package com.evandouard.hxh.client;

import com.evandouard.hxh.fx.NenFx;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * (Toujours redemander le tampon juste avant de dessiner : en demander un autre clôt le précédent.)
 * <p>
 * Modèles 3D des images de techniques, construits avec les formes de {@link Mesh3D} : dragon de Zeno (corps de
 * serpent qui ondule, tête à cornes), démons de Razor, Cookie de Biscuit, soldats de fumée de Morel, cœur de
 * Pariston, notes de Senritsu, bombe de Genthru, soleil de Feitan, armes de Kite, goutte de peur…
 * statue de Kannon de Netero (mille bras en éventail, mains jointes, auréole) et ses mains dorées.
 * Ce qui est plat par nature (ailes, marque de porte, papier, livre) reste dessiné.
 */
public final class Sprites3D {
    private Sprites3D() {}

    /** @return true si le modèle 3D a été dessiné (sinon on garde l'image). */
    public static boolean render(Mesh3D.Ctx c, NenFx.Sprite sprite, Vec3 at, float size, int color, float time, float alpha,
                                 Vec3 heading, float spin) {
        int rgb = color & 0xFFFFFF;
        Vec3 fwd = heading.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : heading.normalize();
        switch (sprite) {
            case DRAGON -> dragon(c, at, fwd, size * 0.75f, NenFx.GOLD & 0xFFFFFF, time, alpha);
            case DRAGON_DIVE -> dragon(c, at, fwd, size * 0.6f, NenFx.GOLD & 0xFFFFFF, time, alpha);
            case DEMON -> demon(c, at, size * 0.5f, rgb, time, alpha);
            case COOKIE -> cookie(c, at, size * 0.5f, time, alpha);
            case SMOKE_SOLDIER -> smokeSoldier(c, at, size * 0.5f, rgb, time, alpha);
            case HEART -> heart(c, at, size * 0.4f, 0xFF5C9E, time, alpha);
            case NOTE -> note(c, at, size * 0.4f, rgb, time, alpha);
            case FEAR -> drop(c, at, size * 0.18f, 0x8CC8FF, alpha);
            case STAFF -> staff(c, at, size * 0.5f, spin, alpha);
            case SCYTHE -> scythe(c, at, size * 0.5f, fwd, spin, alpha);
            case BLINKY -> blinky(c, at, size * 0.5f, fwd, time, alpha);
            default -> {
                return false;
            }
        }
        return true;
    }

    // ---------- Dragon de Zeno ----------

    /** Corps de serpent qui ondule derrière la tête, tête allongée, deux cornes, moustaches, crinière lumineuse. */
    static void dragon(Mesh3D.Ctx c, Vec3 head, Vec3 fwd, float s, int rgb, float time, float alpha) {
        Vec3[] b = Mesh3D.basis(fwd);
        List<Vec3> body = new ArrayList<>();
        int n = 20;
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            // corps en S bien marqué sur les côtés : visible même quand le dragon file dans l'axe de la caméra
            float wave = Mth.sin(time * 0.35f + t * 4.7f) * s * 0.9f * Mth.sin(t * Mth.PI);
            float wave2 = Mth.cos(time * 0.3f + t * 3.5f) * s * 0.35f * t;
            body.add(head.subtract(fwd.scale(t * s * 2.6f)).add(b[0].scale(wave)).add(b[1].scale(wave2 + t * s * 0.5f)));
        }
        Mesh3D.tube(c, Mesh3D.glow(c), body, t -> s * (0.36f - 0.28f * t), rgb, alpha * 0.45f, 8, false);
        Mesh3D.tube(c, Mesh3D.solid(c), body, t -> s * (0.26f - 0.2f * t), rgb, alpha, 10, true);
        // tête : museau (cône) vers l'avant, crâne (sphère)
        Mesh3D.sphere(c, Mesh3D.solid(c), head, s * 0.3f, rgb, alpha, 8, 12, null, true, false);
        Mesh3D.cone(c, Mesh3D.solid(c), head.add(fwd.scale(s * 0.1f)), head.add(fwd.scale(s * 0.62f)), s * 0.2f, rgb, alpha, 10, true);
        // cornes et moustaches
        Vec3 up = b[1].y >= 0 ? b[1] : b[1].scale(-1);
        for (int k = -1; k <= 1; k += 2) {
            Vec3 base = head.add(b[0].scale(k * s * 0.15f)).add(up.scale(s * 0.18f));
            Mesh3D.cone(c, Mesh3D.solid(c), base, base.add(up.scale(s * 0.4f)).subtract(fwd.scale(s * 0.35f)).add(b[0].scale(k * s * 0.1f)),
                    s * 0.05f, 0xFFF4C0, alpha, 6, true);
            Vec3 m0 = head.add(fwd.scale(s * 0.45f)).add(b[0].scale(k * s * 0.12f));
            List<Vec3> whisker = new ArrayList<>();
            for (int i = 0; i <= 6; i++) {
                float t = i / 6f;
                whisker.add(m0.add(b[0].scale(k * s * 0.5f * t)).subtract(fwd.scale(s * 0.4f * t))
                        .add(up.scale(Mth.sin(time * 0.3f + t * 4) * s * 0.08f)));
            }
            Mesh3D.tube(c, Mesh3D.glow(c), whisker, s * 0.025f, s * 0.008f, 0xFFF4C0, alpha, 4, false);
        }
        // yeux
        for (int k = -1; k <= 1; k += 2)
            Mesh3D.sphere(c, Mesh3D.glow(c), head.add(fwd.scale(s * 0.22f)).add(b[0].scale(k * s * 0.16f)).add(up.scale(s * 0.1f)),
                    s * 0.05f, 0xFFFFFF, alpha, 4, 6, null, false, false);
    }

    // ---------- Démon de Razor ----------

    static void demon(Mesh3D.Ctx c, Vec3 at, float s, int rgb, float time, float alpha) {
        float bob = Mth.sin(time * 0.3f + (float) at.x) * s * 0.08f;
        Vec3 body = at.add(0, bob, 0);
        Mesh3D.sphere(c, Mesh3D.solid(c), body, s * 0.6f, rgb, alpha, 10, 14, d -> 1f + (d.y < 0 ? 0.15f * (float) -d.y : 0), true, false);
        // cornes
        for (int k = -1; k <= 1; k += 2) {
            Vec3 base = body.add(k * s * 0.32, s * 0.38, 0);
            Mesh3D.cone(c, Mesh3D.solid(c), base, base.add(k * s * 0.18, s * 0.5, 0), s * 0.12f, 0x2A1010, alpha, 6, true);
        }
        // yeux jaunes et bouche
        for (int k = -1; k <= 1; k += 2)
            Mesh3D.sphere(c, Mesh3D.glow(c), body.add(k * s * 0.2, s * 0.12, s * 0.5), s * 0.09f, 0xFFE040, alpha, 4, 6, null, false, false);
        Mesh3D.torus(c, Mesh3D.glow(c), body.add(0, -s * 0.12, s * 0.52), new Vec3(0, 0, 1), s * 0.14f, s * 0.03f, 0xFF6A3D, alpha, 12, 4, false);
        Mesh3D.sphere(c, Mesh3D.glow(c), body, s * 0.85f, rgb, alpha * 0.35f, 8, 12, null, false, true);
    }

    // ---------- Cookie (Biscuit) ----------

    static void cookie(Mesh3D.Ctx c, Vec3 at, float s, float time, float alpha) {
        int pink = 0xFFB0D0;
        Vec3 body = at.add(0, -s * 0.35, 0), head = at.add(0, s * 0.45, 0);
        Mesh3D.sphere(c, Mesh3D.solid(c), body, s * 0.55f, pink, alpha, 10, 14, null, true, false);
        Mesh3D.sphere(c, Mesh3D.solid(c), head, s * 0.42f, 0xFFF0F4, alpha, 10, 14, null, true, false);
        // bonnet rose
        Mesh3D.sphere(c, Mesh3D.solid(c), head.add(0, s * 0.22, 0), s * 0.4f, pink, alpha, 8, 12, d -> d.y < -0.1 ? 0.2f : 1f, true, false);
        for (int k = -1; k <= 1; k += 2)
            Mesh3D.sphere(c, Mesh3D.solid(c), head.add(k * s * 0.15, s * 0.02, s * 0.36), s * 0.05f, 0x222222, alpha, 4, 6, null, false, false);
        // bulles de massage
        for (int i = 0; i < 4; i++) {
            float a = time * 0.08f + i * 1.57f;
            Mesh3D.sphere(c, Mesh3D.glow(c), at.add(Mth.cos(a) * s * 0.9, Mth.sin(a * 1.3f) * s * 0.4, Mth.sin(a) * s * 0.9),
                    s * 0.1f, 0x9AD7FF, alpha * 0.8f, 6, 8, null, false, true);
        }
    }

    // ---------- Soldat de fumée (Morel) ----------

    static void smokeSoldier(Mesh3D.Ctx c, Vec3 at, float s, int rgb, float time, float alpha) {
        for (int i = 0; i < 5; i++) {
            float y = -0.6f + i * 0.28f;
            float r = s * (0.45f - Math.abs(i - 1.5f) * 0.06f);
            float drift = Mth.sin(time * 0.2f + i) * s * 0.06f;
            Mesh3D.sphere(c, Mesh3D.solid(c), at.add(drift, y * s, 0), r, rgb, alpha * 0.55f, 8, 10,
                    d -> 1f + 0.12f * Mth.sin((float) (d.x * 9 + d.z * 7 + time * 0.3f)), true, false);
        }
        Mesh3D.sphere(c, Mesh3D.solid(c), at.add(0, s * 0.85, 0), s * 0.28f, rgb, alpha * 0.7f, 8, 10, null, true, false);
        for (int k = -1; k <= 1; k += 2)
            Mesh3D.sphere(c, Mesh3D.glow(c), at.add(k * s * 0.1, s * 0.9, s * 0.24), s * 0.05f, 0xFFFFFF, alpha, 4, 6, null, false, false);
    }

    // ---------- Cœur, note, goutte ----------

    static void heart(Mesh3D.Ctx c, Vec3 at, float s, int rgb, float time, float alpha) {
        float beat = 1f + 0.12f * Math.max(0, Mth.sin(time * 0.6f));
        float r = s * 0.42f * beat;
        Mesh3D.sphere(c, Mesh3D.solid(c), at.add(-r * 0.7, r * 0.4, 0), r, rgb, alpha, 10, 12, null, true, false);
        Mesh3D.sphere(c, Mesh3D.solid(c), at.add(r * 0.7, r * 0.4, 0), r, rgb, alpha, 10, 12, null, true, false);
        Mesh3D.cone(c, Mesh3D.solid(c), at.add(0, r * 0.35, 0), at.add(0, -r * 1.9, 0), r * 1.55f, rgb, alpha, 14, true);
        Mesh3D.sphere(c, Mesh3D.glow(c), at, s * 1.1f, rgb, alpha * 0.25f, 8, 10, null, false, true);
    }

    static void note(Mesh3D.Ctx c, Vec3 at, float s, int rgb, float time, float alpha) {
        Vec3 headPos = at.add(0, -s * 0.4, 0);
        Mesh3D.sphere(c, Mesh3D.solid(c), headPos, s * 0.25f, rgb, alpha, 8, 10, d -> 1f + 0.25f * (float) Math.abs(d.x), true, false);
        Vec3 top = headPos.add(s * 0.22, s * 1.1, 0);
        Mesh3D.tube(c, Mesh3D.solid(c), List.of(headPos.add(s * 0.22, 0, 0), top), s * 0.04f, s * 0.04f, rgb, alpha, 6, true);
        Mesh3D.tube(c, Mesh3D.solid(c), List.of(top, top.add(s * 0.35, -s * 0.3, 0)), s * 0.05f, s * 0.03f, rgb, alpha, 6, true);
    }

    static void drop(Mesh3D.Ctx c, Vec3 at, float s, int rgb, float alpha) {
        Mesh3D.sphere(c, Mesh3D.solid(c), at, s, rgb, alpha, 8, 10, null, true, false);
        Mesh3D.cone(c, Mesh3D.solid(c), at.add(0, s * 0.5, 0), at.add(0, s * 2.1, 0), s * 0.85f, rgb, alpha, 10, true);
    }

    // ---------- Armes de Kite, Blinky ----------

    static void staff(Mesh3D.Ctx c, Vec3 at, float s, float spin, float alpha) {
        Vec3 axis = new Vec3(Mth.sin(spin), Mth.cos(spin), 0);
        Mesh3D.tube(c, Mesh3D.solid(c), List.of(at.subtract(axis.scale(s)), at.add(axis.scale(s))), s * 0.05f, s * 0.05f, 0xC8B896, alpha, 6, true);
        Mesh3D.sphere(c, Mesh3D.solid(c), at.add(axis.scale(s * 1.05f)), s * 0.14f, 0xD03C50, alpha, 6, 8, null, true, false);
    }

    static void scythe(Mesh3D.Ctx c, Vec3 at, float s, Vec3 fwd, float spin, float alpha) {
        Vec3[] b = Mesh3D.basis(fwd);
        Vec3 axis = b[1].scale(Mth.cos(spin)).add(b[0].scale(Mth.sin(spin))).normalize();
        Vec3 bottom = at.subtract(axis.scale(s)), top = at.add(axis.scale(s));
        Mesh3D.tube(c, Mesh3D.solid(c), List.of(bottom, top), s * 0.05f, s * 0.05f, 0x8C5F37, alpha, 6, true);
        List<Vec3> blade = new ArrayList<>();
        Vec3 out = fwd.normalize();
        for (int i = 0; i <= 10; i++) {
            float t = i / 10f;
            float a = t * 1.9f;
            blade.add(top.add(out.scale(Mth.sin(a) * s * 0.8f)).subtract(axis.scale((1 - Mth.cos(a)) * s * 0.4f)));
        }
        Mesh3D.tube(c, Mesh3D.solid(c), blade, t -> s * (0.12f * (1 - t) + 0.01f), 0xE6EAF2, alpha, 6, true);
    }

    static void blinky(Mesh3D.Ctx c, Vec3 at, float s, Vec3 fwd, float time, float alpha) {
        Mesh3D.sphere(c, Mesh3D.solid(c), at, s * 0.55f, 0xE8E2F0, alpha, 10, 14, null, true, false);
        // bouche-aspirateur : un cône ouvert vers l'avant, et l'aspiration (anneaux qui rentrent)
        Mesh3D.cone(c, Mesh3D.solid(c), at.add(fwd.scale(s * 1.0f)), at.add(fwd.scale(s * 0.35f)), s * 0.35f, 0x5A4A70, alpha, 12, true);
        for (int k = 0; k < 3; k++) {
            float ph = ((time / 10f) + k / 3f) % 1f;
            Mesh3D.torus(c, Mesh3D.glow(c), at.add(fwd.scale(s * (1.0f + 1.2f * (1 - ph)))), fwd, s * (0.35f + 0.4f * (1 - ph)), s * 0.03f,
                    0xB0B0FF, alpha * ph, 16, 4, false);
        }
        for (int k = -1; k <= 1; k += 2) {
            Vec3 side = fwd.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1e-6) side = new Vec3(1, 0, 0);
            side = side.normalize();
            Mesh3D.sphere(c, Mesh3D.glow(c), at.add(side.scale(k * s * 0.2)).add(0, s * 0.25, 0).add(fwd.scale(s * 0.4)), s * 0.07f,
                    0x111111, alpha, 4, 6, null, false, false);
        }
    }

    // ---------- Soleil de Feitan ----------

    /** Boule de feu en 3D, couronne de rayons coniques qui tourne, halo. */
    public static void sun(Mesh3D.Ctx c, Vec3 at, float r, int color, float spin, float alpha) {
        int rgb = color & 0xFFFFFF;
        // cœur opaque non ombré (l'additif seul sature en blanc sur le ciel), puis lueur par-dessus
        Mesh3D.sphere(c, Mesh3D.solid(c), at, r, 0xFF7A18, alpha, 14, 20,
                d -> 1f + 0.05f * Mth.sin((float) (d.x * 8 + d.y * 6 + spin * 10)), false, false);
        Mesh3D.sphere(c, Mesh3D.glow(c), at, r * 0.6f, 0xFFE08A, alpha * 0.45f, 8, 12, null, false, false);
        for (int i = 0; i < 16; i++) {
            float a = spin + i * Mth.TWO_PI / 16;
            Vec3 d = new Vec3(Mth.cos(a), 0.25f * Mth.sin(i * 1.7f), Mth.sin(a)).normalize();
            Mesh3D.cone(c, Mesh3D.glow(c), at.add(d.scale(r * 0.9f)), at.add(d.scale(r * (1.7f + 0.3f * (i % 2)))), r * 0.18f, rgb, alpha, 6, false);
        }
        Mesh3D.sphere(c, Mesh3D.glow(c), at, r * 1.8f, rgb, alpha * 0.15f, 10, 14, null, false, true);
    }

    private static final int KANNON_GOLD = 0xE8B44A;

    /**
     * Statue de Kannon (Hyakushiki Kannon de Netero), debout sur un lotus, face à {@code fwd} : robe évasée, buste,
     * tête à chignon, mains jointes devant la poitrine, éventail de bras dans le dos, auréole derrière la tête et
     * grand nimbe autour du corps. Or translucide ombré + enveloppe lumineuse de la couleur de l'aura.
     */
    public static void kannon(Mesh3D.Ctx c, Vec3 base, Vec3 fwd, float h, int color, float time, float alpha, boolean halo) {
        int aura = color & 0xFFFFFF;
        Vec3 f = new Vec3(fwd.x, 0, fwd.z);
        f = f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
        Vec3 side = f.cross(new Vec3(0, 1, 0)).normalize();
        float breath = 1f + 0.015f * Mth.sin(time * 0.15f);
        // lotus : pétales en cônes inclinés vers l'extérieur
        for (int i = 0; i < 10; i++) {
            float a = Mth.TWO_PI * i / 10f;
            Vec3 out = side.scale(Mth.cos(a)).add(f.scale(Mth.sin(a)));
            Vec3 b0 = base.add(out.scale(h * 0.1));
            Mesh3D.cone(c, Mesh3D.solid(c), b0, b0.add(out.scale(h * 0.17)).add(0, h * 0.09, 0), h * 0.055f,
                    0xF0C8D0, alpha * 0.85f, 6, true);
        }
        // robe puis buste : un seul tube au profil évasé en bas, serré à la taille, large aux épaules
        List<Vec3> trunk = new ArrayList<>();
        for (int i = 0; i <= 8; i++) trunk.add(base.add(0, h * (0.06 + 0.58 * i / 8f), 0));
        Mesh3D.Profile body = t -> h * breath * (t < 0.55f ? Mth.lerp(t / 0.55f, 0.2f, 0.12f)
                : Mth.lerp((t - 0.55f) / 0.45f, 0.12f, 0.15f) * (t > 0.92f ? 0.75f : 1f));
        Mesh3D.tube(c, Mesh3D.solid(c), trunk, body, KANNON_GOLD, alpha * 0.8f, 12, true);
        Mesh3D.tube(c, Mesh3D.glow(c), trunk, t -> body.radius(t) * 1.12f, aura, alpha * 0.35f, 12, false, true);
        Vec3 neck = base.add(0, h * 0.66, 0);
        Vec3 head = neck.add(0, h * 0.075, 0);
        Mesh3D.sphere(c, Mesh3D.solid(c), head, h * 0.075f, KANNON_GOLD, alpha * 0.85f, 10, 12, null, true, false);
        Mesh3D.sphere(c, Mesh3D.solid(c), head.add(0, h * 0.08, 0).subtract(f.scale(h * 0.01)), h * 0.035f,
                KANNON_GOLD, alpha * 0.85f, 6, 8, null, true, false);
        // yeux mi-clos lumineux
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Vec3 eye = head.add(f.scale(h * 0.07)).add(side.scale(h * 0.025 * sgn)).add(0, h * 0.008, 0);
            Mesh3D.sphere(c, Mesh3D.glow(c), eye, h * 0.009f, 0xFFF6D8, alpha, 4, 6, null, false, false);
        }
        // auréole derrière la tête, grand nimbe autour du corps
        if (halo) {
            Mesh3D.torus(c, Mesh3D.glow(c), head.subtract(f.scale(h * 0.09)), f, h * 0.13f, h * 0.012f, aura, alpha * 0.8f,
                    28, 6, false);
            Mesh3D.torus(c, Mesh3D.glow(c), base.add(0, h * 0.5, 0).subtract(f.scale(h * 0.2)), f, h * 0.47f, h * 0.008f,
                    aura, alpha * 0.45f, 40, 5, false);
        }
        // mains jointes (gasshô) : deux avant-bras qui se rejoignent devant la poitrine, paumes vers le haut
        Vec3 chest = base.add(0, h * 0.5, 0).add(f.scale(h * 0.17));
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Vec3 shoulder = base.add(0, h * 0.6, 0).add(side.scale(h * 0.14 * sgn));
            Vec3 elbow = base.add(0, h * 0.44, 0).add(side.scale(h * 0.15 * sgn)).add(f.scale(h * 0.07));
            Vec3 wrist = chest.add(side.scale(h * 0.018 * sgn));
            Mesh3D.tube(c, Mesh3D.solid(c), List.of(shoulder, elbow, wrist), h * 0.04f, h * 0.03f, KANNON_GOLD,
                    alpha * 0.85f, 8, true);
            Mesh3D.cone(c, Mesh3D.solid(c), wrist, wrist.add(0, h * 0.1, 0).add(f.scale(h * 0.015)).add(side.scale(-h * 0.012 * sgn)),
                    h * 0.03f, KANNON_GOLD, alpha * 0.85f, 6, true);
        }
        // éventail de bras dans le dos (les « cent bras ») : chacun ondule un peu, main ouverte au bout
        int arms = 14;
        Vec3 back = base.add(0, h * 0.56, 0).subtract(f.scale(h * 0.06));
        for (int i = 0; i < arms; i++) {
            float u = (i + 0.5f) / arms;                       // 0..1 de gauche à droite
            float ang = Mth.lerp(u, -2.5f, 2.5f);               // éventail de presque 290°
            float sway = Mth.sin(time * 0.12f + i * 0.9f) * 0.08f;
            Vec3 dir = side.scale(Mth.sin(ang + sway)).add(0, Mth.cos(ang + sway), 0).subtract(f.scale(0.25)).normalize();
            Vec3 mid = back.add(dir.scale(h * 0.2)).add(0, -h * 0.02, 0);
            Vec3 end = back.add(dir.scale(h * 0.38));
            Mesh3D.tube(c, Mesh3D.solid(c), List.of(back.add(dir.scale(h * 0.08)), mid, end), h * 0.025f, h * 0.018f,
                    KANNON_GOLD, alpha * 0.7f, 6, true);
            Mesh3D.sphere(c, Mesh3D.solid(c), end.add(dir.scale(h * 0.025)), h * 0.03f, KANNON_GOLD, alpha * 0.75f, 5, 7,
                    d -> 1f - 0.45f * (float) Math.abs(d.dot(f)), true, false);
            Mesh3D.sphere(c, Mesh3D.glow(c), end.add(dir.scale(h * 0.025)), h * 0.045f, aura, alpha * 0.3f, 5, 7, null, false, true);
        }
    }

    /** Main dorée de Kannon (paume, quatre doigts, pouce), doigts vers {@code fwd} : frappe des Kannon. */
    public static void kannonHand(Mesh3D.Ctx c, Vec3 at, Vec3 fwd, float s, int color, float alpha) {
        Vec3 f = fwd.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : fwd.normalize();
        Vec3[] b = Mesh3D.basis(f);
        Vec3 side = b[0], up = b[1];
        Mesh3D.sphere(c, Mesh3D.solid(c), at, s * 0.32f, KANNON_GOLD, alpha, 8, 10,
                d -> 1f - 0.55f * (float) Math.abs(d.dot(up)), true, false);
        for (int i = 0; i < 4; i++) {
            float off = (i - 1.5f) * 0.14f;
            Vec3 root = at.add(f.scale(s * 0.25)).add(side.scale(s * off));
            float len = s * (0.42f - Math.abs(i - 1.5f) * 0.06f);
            Mesh3D.tube(c, Mesh3D.solid(c), List.of(root, root.add(f.scale(len * 0.55)), root.add(f.scale(len)).add(up.scale(-s * 0.05))),
                    s * 0.065f, s * 0.05f, KANNON_GOLD, alpha, 6, true);
        }
        Vec3 troot = at.add(side.scale(-s * 0.28));
        Mesh3D.tube(c, Mesh3D.solid(c), List.of(troot, troot.add(side.scale(-s * 0.12)).add(f.scale(s * 0.15)),
                troot.add(f.scale(s * 0.32)).add(side.scale(-s * 0.08))), s * 0.07f, s * 0.055f, KANNON_GOLD, alpha, 6, true);
        Mesh3D.sphere(c, Mesh3D.glow(c), at.add(f.scale(s * 0.15)), s * 0.6f, color & 0xFFFFFF, alpha * 0.35f, 8, 10, null, false, true);
    }
}
