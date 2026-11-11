package com.evandouard.hxh.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Boule d'aura en 3D (Jajanken : Pierre, boule de Silva, Aura Ball de Razor…), en couches :
 * <ul>
 *     <li>noyau lumineux qui pulse ;</li>
 *     <li>sphère interne d'aura concentrée, ombrée, qui tourne (bandes) ;</li>
 *     <li>enveloppe d'énergie translucide (bords denses) qui ondule ;</li>
 *     <li>deux anneaux qui orbitent sur des axes différents ;</li>
 *     <li>des ondes qui gonflent et s'effacent (accumulation de puissance).</li>
 * </ul>
 * {@code epic} : Jajanken ultime, noyau chauffé à blanc, enveloppe plus agitée.
 */
public final class AuraOrb3D {
    private AuraOrb3D() {}

    public static void render(Mesh3D.Ctx c, Vec3 center, float r, int color, float time, boolean epic) {
        int rgb = color & 0xFFFFFF;
        int light = mix(rgb, 0xFFFFFF, epic ? 0.75f : 0.45f);
        float pulse = 1f + 0.07f * Mth.sin(time * 0.9f);
        // sphère interne ombrée (bandes qui tournent)
        VertexConsumer solid = Mesh3D.solid(c);
        float spin = time * 0.25f;
        Mesh3D.sphere(c, solid, center, r * 0.62f, mix(rgb, 0xFFFFFF, 0.2f), 0.95f, 12, 18,
                d -> 1f + 0.05f * Mth.sin((float) (Math.atan2(d.z, d.x) * 5 + spin * 6 + d.y * 4)), true, false);
        VertexConsumer glow = Mesh3D.glow(c);
        // noyau
        Mesh3D.sphere(c, glow, center, r * 0.38f * pulse, light, 0.9f, 8, 12, null, false, false);
        // enveloppe qui ondule
        float agitation = epic ? 0.16f : 0.09f;
        Mesh3D.sphere(c, glow, center, r * pulse, rgb, 0.85f, 14, 22,
                d -> 1f + agitation * Mth.sin((float) (d.y * 7 + time * 0.6f)) * Mth.cos((float) (Math.atan2(d.z, d.x) * 3 - time * 0.4f)),
                false, true);
        // anneaux en orbite
        Vec3 ax1 = new Vec3(Mth.sin(time * 0.05f), 1, Mth.cos(time * 0.05f)).normalize();
        Vec3 ax2 = new Vec3(1, Mth.sin(time * 0.07f) * 0.5, Mth.cos(time * 0.03f)).normalize();
        Mesh3D.torus(c, glow, center, ax1, r * 1.3f, r * 0.045f, light, 0.8f, 28, 6, false);
        Mesh3D.torus(c, glow, center, ax2, r * 1.15f, r * 0.035f, rgb, 0.7f, 28, 6, false);
        // ondes d'accumulation (deux en décalé)
        for (int k = 0; k < 2; k++) {
            float ph = ((time / 18f) + k * 0.5f) % 1f;
            Mesh3D.sphere(c, glow, center, r * (1f + ph * 0.9f), rgb, 0.45f * (1 - ph), 10, 16, null, false, true);
        }
    }

    static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }
}
