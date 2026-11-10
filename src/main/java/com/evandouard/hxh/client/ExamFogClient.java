package com.evandouard.hxh.client;

import com.evandouard.hxh.exam.ExamHazards;
import com.evandouard.hxh.exam.ExamSite;
import com.evandouard.hxh.exam.ExamWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Brouillard épais des marais de Numele (Phase 1, suite du tunnel) : on n'y voit qu'à quelques blocs, il faut coller
 * à Satotz. Purement visuel ; le lieu se déduit de la position (les sites de l'Examen sont à des X fixes).
 */
public final class ExamFogClient {
    private ExamFogClient() {}

    static final float FOG_FAR = 14f;
    private static ExamSite lastLogged;

    static void register() {
        NeoForge.EVENT_BUS.addListener(ViewportEvent.RenderFog.class, ExamFogClient::onFog);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, ExamFogClient::onColor);
    }

    /** Lieu brumeux où se trouve le joueur (marais de Numele, forêt des Kiriko), ou null. */
    private static ExamSite foggy() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !ExamWorld.isExamLevel(p.level())) return null;
        ExamSite s = ExamHazards.siteAt(p.blockPosition());
        return s == ExamSite.NUMELE || s == ExamSite.KIRIKO ? s : null;
    }

    private static boolean inFog() {
        return foggy() != null;
    }

    private static void onFog(ViewportEvent.RenderFog e) {
        if (!inFog()) return;
        e.setNearPlaneDistance(0f);
        e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), foggy() == ExamSite.KIRIKO ? 30f : FOG_FAR));
        e.setCanceled(true);
        if (lastLogged != foggy()) {
            lastLogged = foggy();
            MangaHud.listener.accept(foggy() == ExamSite.KIRIKO ? "[hud] Brume de la forêt des Kiriko" : "[hud] Brouillard de Numele");
        }
    }

    private static void onColor(ViewportEvent.ComputeFogColor e) {
        if (!inFog()) return;
        boolean forest = foggy() == ExamSite.KIRIKO;
        e.setRed(forest ? 0.18f : 0.62f);
        e.setGreen(forest ? 0.24f : 0.66f);
        e.setBlue(forest ? 0.2f : 0.6f);
    }
}
