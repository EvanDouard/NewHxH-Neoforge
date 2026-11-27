package com.evandouard.hxh.client;

import com.evandouard.hxh.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Peur vue de l'intérieur, façon crise de panique : par à-coups la vue « zoome » (champ de vision qui se resserre) et
 * un tunnel sombre aux bords nets se referme sur le centre de l'écran ; entre deux accès elle se dézoome un peu
 * (vertigo), pulse au rythme d'un cœur qui s'emballe, et tremble quand la terreur est totale. Plus l'écart de
 * puissance est grand, plus c'est fort. Le zoom respecte le réglage « Effets de champ de vision » de Minecraft.
 */
public final class FearClient {
    /** Dézoom maximal (terreur) : +28 % de champ de vision. */
    private static final float MAX_ZOOM_OUT = 0.28f;
    /** Amplitude d'un battement de cœur. */
    private static final float BEAT_PULSE = 0.05f;

    private static float intensity, previous;
    private static int sinceBeat = 100;
    private static int ticks;

    private FearClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFov.class, FearClient::onFov);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, FearClient::onAngles);
    }

    /** Intensité 0 → 1 visée selon le niveau de peur (I, II, III). */
    static float targetFor(int amplifier) {
        return Math.min(1f, (amplifier + 1) / 3f);
    }

    /** Intervalle entre deux battements (ticks) : le cœur s'accélère avec la peur. */
    static int beatInterval(int amplifier) {
        return amplifier >= 2 ? 8 : amplifier == 1 ? 12 : 18;
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        previous = intensity;
        if (player == null || mc.isPaused()) {
            if (player == null) intensity = previous = 0f;
            return;
        }
        ticks++;
        MobEffectInstance fear = player.getEffect(ModEffects.FEAR);
        float target = fear == null ? 0f : targetFor(fear.getAmplifier());
        // Monte vite (la panique saisit), redescend doucement (on reprend son souffle)
        intensity = target > intensity ? Math.min(target, intensity + 0.04f) : Math.max(target, intensity - 0.015f);
        sinceBeat++;
        if (fear != null && sinceBeat >= beatInterval(fear.getAmplifier())) {
            sinceBeat = 0;
            player.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.5f + 0.5f * intensity, 0.9f + 0.15f * fear.getAmplifier());
        }
    }

    private static float effectScale() {
        return Minecraft.getInstance().options.fovEffectScale().get().floatValue();
    }

    private static void onFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) return; // pas la main en vue subjective
        float partial = (float) event.getPartialTick();
        float i = Mth.lerp(partial, previous, intensity);
        if (i <= 0.001f) return;
        float t = ticks + partial;
        float zoom = MAX_ZOOM_OUT * ease(i);
        // battement : coup sec puis retombée
        float beat = (float) Math.exp(-(sinceBeat + partial) / 2.5f) * BEAT_PULSE * i;
        // respiration courte et rapide
        float breath = Mth.sin(t * 0.35f) * 0.015f * i;
        // accès de panique : le champ de vision se resserre d'un coup (jusqu'à −30 %), comme si la vue « zoomait »
        float panic = -0.30f * surge(t) * ease(i);
        event.setFOV(event.getFOV() * Math.max(0.5, 1.0 + (zoom + beat + breath + panic) * effectScale()));
    }

    private static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        float partial = (float) event.getPartialTick();
        float i = Mth.lerp(partial, previous, intensity);
        if (i < 0.5f) return;
        // tremblement quand la terreur domine
        float k = (i - 0.5f) * 2f * effectScale();
        float t = ticks + partial;
        event.setRoll(event.getRoll() + (Mth.sin(t * 1.9f) + 0.5f * Mth.sin(t * 4.3f)) * 0.9f * k);
        event.setPitch(event.getPitch() + Mth.sin(t * 2.7f) * 0.35f * k);
    }

    /** Accès de panique : 0 entre deux crises, monte vite vers 1 puis retombe (cycle d'environ 6 s, irrégulier). */
    static float surge(float t) {
        float wave = Mth.sin(t * 0.105f) + 0.6f * Mth.sin(t * 0.043f + 1.3f);
        return Mth.clamp((wave - 0.35f) / 1.0f, 0f, 1f);
    }

    /** Resserrement de l'écran (0 → 1) pour l'intensité et la phase de panique du moment. */
    static float tunnel(float intensity, float surge) {
        return Mth.clamp(intensity * (0.3f + 0.7f * surge), 0f, 1f);
    }

    public static void registerLayers(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent.class, e -> e.registerAbove(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.CAMERA_OVERLAYS, com.evandouard.hxh.HxHMod.id("fear_tunnel"), FearClient::renderTunnel));
    }

    /** Vision en tunnel : deux ellipses concentriques aux bords durs, plus sombres vers l'extérieur. */
    private static void renderTunnel(net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.DeltaTracker delta) {
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float i = Mth.lerp(partial, previous, intensity);
        if (i <= 0.01f) return;
        float t = ticks + partial;
        float k = tunnel(i, surge(t));
        if (k <= 0.02f) return;
        int w = g.guiWidth(), h = g.guiHeight();
        // l'ellipse claire va de tout l'écran (k = 0) à un étroit couloir central (k = 1)
        float scale = 1.45f - 1.0f * k;
        ring(g, w, h, scale, 0x78100000);
        ring(g, w, h, scale * 0.82f, 0x90100000);
    }

    /** Remplit tout ce qui est hors de l'ellipse de demi-axes (scale × demi-largeur, scale × demi-hauteur). */
    private static void ring(net.minecraft.client.gui.GuiGraphics g, int w, int h, float scale, int argb) {
        float cx = w / 2f, cy = h / 2f, rx = cx * scale, ry = cy * scale;
        for (int y = 0; y < h; y += 2) {
            float dy = (y + 1 - cy) / ry;
            if (dy >= 1f) { g.fill(0, y, w, y + 2, argb); continue; }
            int half = (int) (rx * Mth.sqrt(1f - dy * dy));
            if (cx - half > 0) g.fill(0, y, (int) (cx - half), y + 2, argb);
            if (cx + half < w) g.fill((int) (cx + half), y, w, y + 2, argb);
        }
    }

    private static float ease(float x) {
        return x * x * (3f - 2f * x);
    }
}
