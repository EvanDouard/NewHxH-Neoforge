package com.evandouard.hxh.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

/**
 * Image d'impact (coup dévastateur) : l'écran passe en noir et blanc comme une case de manga.
 * <ol>
 *     <li>2 ticks : éclair blanc, lignes de vitesse noires ;</li>
 *     <li>4 ticks : négatif, fond noir et lignes blanches ;</li>
 *     <li>le reste : les lignes noires s'effacent pendant que les deux combattants restent figés.</li>
 * </ol>
 */
public final class ImpactFrameClient {
    /** Distance à la cible en deçà de laquelle on voit l'image d'impact. */
    public static final double RANGE = 24;
    private static final int INK = 0xFF111111;

    private static long start = -1;
    private static int duration;
    private static int seed;
    /** Éblouissement de cette image (Pierre sans retenue regardée en face), remis à zéro après dessin. */
    private static float glare;

    private ImpactFrameClient() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, e -> render(e.getGuiGraphics(),
                e.getPartialTick().getGameTimeDeltaPartialTick(false)));
    }

    static void trigger(Vec3 at, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.position().distanceTo(at) > RANGE) return;
        start = mc.level.getGameTime();
        duration = Math.max(4, ticks);
        seed = (int) start;
        MangaHud.listener.accept("[hud] Image d'impact");
    }

    /** Demande un voile blanc pour cette image (le plus fort l'emporte). */
    static void glare(float amount) {
        glare = Math.max(glare, amount);
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return start >= 0 && mc.level != null && mc.level.getGameTime() - start <= duration;
    }

    private static void render(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (glare > 0.01f) {
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), (Mth.clamp((int) (glare * 255), 0, 255) << 24) | 0xFFFDF2);
            glare = 0;
        }
        if (start < 0 || mc.level == null) return;
        float age = mc.level.getGameTime() - start + pt;
        if (age > duration || age < 0) {
            if (age > duration) start = -1;
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        float cx = w / 2f, cy = h / 2f;
        if (age < 2) {
            g.fill(0, 0, w, h, 0xE6FFFFFF);
            lines(g, cx, cy, w, h, INK, 1f, 0);
        } else if (age < 6) {
            g.fill(0, 0, w, h, 0xC8111111);
            lines(g, cx, cy, w, h, 0xFFFFFFFF, 1f, 1);
        } else {
            float fade = 1f - (age - 6) / Math.max(1f, duration - 6);
            g.fill(0, 0, w, h, ((int) (fade * 60) << 24) | 0xFFFFFF);
            lines(g, cx, cy, w, h, INK, fade, 2);
        }
    }

    /** Lignes de vitesse : triangles fins qui partent du bord de l'écran vers le centre (sans l'atteindre). */
    private static void lines(GuiGraphics g, float cx, float cy, int w, int h, int color, float alpha, int frame) {
        RandomSource r = RandomSource.create(seed * 31L + frame);
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        float reach = (float) Math.hypot(w, h) * 0.6f;
        float hole = Math.min(w, h) * 0.22f;
        int a = Mth.clamp((int) (alpha * ((color >>> 24) & 0xFF)), 0, 255);
        int argb = (a << 24) | (color & 0xFFFFFF);
        int n = 64;
        for (int i = 0; i < n; i++) {
            float ang = (i + r.nextFloat() * 0.8f) / n * Mth.TWO_PI;
            float spread = 0.008f + r.nextFloat() * 0.022f;
            float inner = hole * (0.8f + r.nextFloat() * 0.9f);
            float x0 = cx + Mth.cos(ang - spread) * reach, y0 = cy + Mth.sin(ang - spread) * reach;
            float x1 = cx + Mth.cos(ang + spread) * reach, y1 = cy + Mth.sin(ang + spread) * reach;
            float tx = cx + Mth.cos(ang) * inner, ty = cy + Mth.sin(ang) * inner;
            vc.addVertex(m, x0, y0, 0).setColor(argb);
            vc.addVertex(m, tx, ty, 0).setColor(argb);
            vc.addVertex(m, tx, ty, 0).setColor(argb);
            vc.addVertex(m, x1, y1, 0).setColor(argb);
            // même triangle dans l'autre sens : visible quel que soit le sens de culling
            vc.addVertex(m, x1, y1, 0).setColor(argb);
            vc.addVertex(m, tx, ty, 0).setColor(argb);
            vc.addVertex(m, tx, ty, 0).setColor(argb);
            vc.addVertex(m, x0, y0, 0).setColor(argb);
        }
        g.flush();
    }
}
