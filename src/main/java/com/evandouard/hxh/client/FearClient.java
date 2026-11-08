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
 * Peur vue de l'intérieur, façon crise de panique : la vue se dézoome lentement (effet « vertigo »), pulse au rythme
 * d'un cœur qui s'emballe, et tremble quand la terreur est totale. Plus l'écart de puissance est grand, plus c'est fort.
 * Respecte le réglage « Effets de champ de vision » de Minecraft.
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
        event.setFOV(event.getFOV() * (1.0 + (zoom + beat + breath) * effectScale()));
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

    private static float ease(float x) {
        return x * x * (3f - 2f * x);
    }
}
