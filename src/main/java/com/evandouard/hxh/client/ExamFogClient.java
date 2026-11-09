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
    private static boolean logged;

    static void register() {
        NeoForge.EVENT_BUS.addListener(ViewportEvent.RenderFog.class, ExamFogClient::onFog);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, ExamFogClient::onColor);
    }

    private static boolean inFog() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && ExamWorld.isExamLevel(p.level()) && ExamHazards.siteAt(p.blockPosition()) == ExamSite.NUMELE;
    }

    private static void onFog(ViewportEvent.RenderFog e) {
        if (!inFog()) return;
        e.setNearPlaneDistance(0f);
        e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), FOG_FAR));
        e.setCanceled(true);
        if (!logged) {
            logged = true;
            MangaHud.listener.accept("[hud] Brouillard de Numele");
        }
    }

    private static void onColor(ViewportEvent.ComputeFogColor e) {
        if (!inFog()) return;
        e.setRed(0.62f);
        e.setGreen(0.66f);
        e.setBlue(0.6f);
    }
}
