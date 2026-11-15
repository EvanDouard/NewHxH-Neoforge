package com.evandouard.hxh.client;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Gyo actif : un liseré de la couleur de l'aura borde l'écran (une dizaine de pixels) et s'efface vers le
 * centre, pour rappeler que l'aura est concentrée dans les yeux.
 */
public final class GyoVignette {
    private GyoVignette() {}

    /** Épaisseur du liseré, en pixels d'interface. */
    public static final int WIDTH = 10;
    /** Opacité au bord de l'écran (0-255). */
    private static final int EDGE_ALPHA = 0x70;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        HunterData d = mc.player.getData(ModAttachments.HUNTER);
        if (!d.gyo()) return;
        int rgb = d.auraColor() & 0xFFFFFF;
        int w = g.guiWidth(), h = g.guiHeight();
        for (int i = 0; i < WIDTH; i++) {
            double t = 1.0 - i / (double) WIDTH; // 1 au bord, 0 vers l'intérieur
            int alpha = (int) Math.round(EDGE_ALPHA * t * t);
            if (alpha <= 0) continue;
            int c = alpha << 24 | rgb;
            g.fill(i, i, w - i, i + 1, c);             // haut
            g.fill(i, h - i - 1, w - i, h - i, c);     // bas
            g.fill(i, i + 1, i + 1, h - i - 1, c);     // gauche
            g.fill(w - i - 1, i + 1, w - i, h - i - 1, c); // droite
        }
    }
}
