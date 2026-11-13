package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.network.HudPayload;
import com.evandouard.hxh.network.QuestStatusPayload;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Interface en jeu, style manga : traits noirs épais, ombres portées, jauge d'aura penchée et découpée,
 * quête suivie dans une bulle, notifications qui glissent, gros lettrage au centre pour les coups marquants.
 */
public final class MangaHud {
    // ---------- Palette ----------
    static final int INK = 0xFF111111;
    static final int PAPER = 0xFFFFFFFF;
    static final int GREY = 0xFF555555;
    static final int GOLD = 0xFFFFD23F;

    /** Police du lettrage manga (Bangers, SIL Open Font License). */
    static final net.minecraft.resources.ResourceLocation FONT = HxHMod.id("manga");

    /** Le même texte, en lettrage manga. */
    static Component manga(Component c) {
        return c.copy().withStyle(st -> st.withFont(FONT));
    }

    static Component manga(String s) {
        return manga(Component.literal(s));
    }

    // ---------- État des animations ----------
    /** Bulle : contenu affiché, depuis quand, et quête qui vient de disparaître (animation de sortie). */
    private static String bubbleKey = "";
    private static long bubbleSince = -100;
    private static QuestStatusPayload leaving;
    private static long leavingSince;
    /** Jauge : visible depuis quand, dernière posture (pop de l'onglet quand elle change). */
    private static long gaugeSince = -100;
    private static boolean gaugeShown;
    private static String lastState = "";
    private static long stateSince = -100;

    static final int BUBBLE_IN = 7, BUBBLE_OUT = 6, GAUGE_IN = 8, STATE_POP = 6;

    /** Pour le client scripté de la CI : reçoit le texte de chaque notification et lettrage affichés. */
    public static Consumer<String> listener = s -> {};

    private record Toast(Component title, Component subtitle, String badge, int color, long start) {}

    private record Callout(Component text, int color, boolean big, long start) {}

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static Callout callout;
    private static long ticks;

    static final int TOAST_IN = 6, TOAST_STAY = 70, TOAST_OUT = 8, TOAST_MAX = 3;
    static final int CALLOUT_TICKS = 30;

    private MangaHud() {}

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener(RegisterGuiLayersEvent.class, MangaHud::onRegisterLayers);
        HudPayload.clientHandler = MangaHud::onPayload;
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> {
            if (!Minecraft.getInstance().isPaused()) ticks++;
        });
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HxHMod.id("manga_aura"), MangaHud::renderAura);
        event.registerAbove(VanillaGuiLayers.HOTBAR, HxHMod.id("manga_quest"), MangaHud::renderQuestAndToasts);
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, HxHMod.id("manga_callout"), MangaHud::renderCallout);
    }

    private static void onPayload(HudPayload p) {
        if (p.kind() == HudPayload.TOAST) {
            TOASTS.add(new Toast(p.title(), p.subtitle(), p.badge(), p.color(), ticks));
            while (TOASTS.size() > TOAST_MAX) TOASTS.remove(0);
            listener.accept("[hud] " + p.title().getString() + " — " + p.subtitle().getString());
        } else {
            callout = new Callout(p.title(), p.color(), p.kind() == HudPayload.CALLOUT, ticks);
            listener.accept("[hud] " + p.title().getString());
        }
    }

    // ---------- Outils de dessin ----------

    /** Cadre manga : ombre portée noire, contour noir de 2 px, fond. */
    static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int shadow) {
        if (shadow > 0) g.fill(x + shadow, y + shadow, x + w + shadow, y + h + shadow, INK);
        g.fill(x, y, x + w, y + h, INK);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
    }

    /** Texte cerné de noir (lettrage manga), à l'échelle {@code scale}. */
    static void outlined(GuiGraphics g, Component text, float x, float y, int color, float scale) {
        outlined(g, text, x, y, color, scale, 1f);
    }

    static void outlined(GuiGraphics g, Component text, float x, float y, int color, float scale, float alpha) {
        if (alpha <= 0.02f) return;
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        FormattedCharSequence seq = text.getVisualOrderText();
        int a = Mth.clamp((int) (alpha * 255), 0, 255) << 24;
        font.drawInBatch8xOutline(seq, 0, 0, (color & 0xFFFFFF) | a, (INK & 0xFFFFFF) | a,
                g.pose().last().pose(), g.bufferSource(), 0xF000F0);
        g.flush();
        g.pose().popPose();
    }

    /** Cisaille horizontale (barres penchées) : à appeler après pushPose, autour du point (x, y) bas. */
    static void shear(GuiGraphics g, float x, float y, float k) {
        g.pose().translate(x, y, 0);
        g.pose().last().pose().mul(new Matrix4f().m10(k));
        g.pose().translate(-x, -y, 0);
    }

    private static float partial(DeltaTracker delta) {
        return delta.getGameTimeDeltaPartialTick(true);
    }

    // ---------- Jauge d'aura ----------

    private static void renderAura(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        HunterData data = mc.player.getData(ModAttachments.HUNTER);
        Font font = mc.font;
        int x = 10;
        int bottom = g.guiHeight() - 10;

        float now = ticks + partial(delta);
        if (!data.nenUnlocked()) {
            gaugeShown = false;
            if (data.meditating()) {
                tab(g, x, bottom - 12, manga(Component.translatable("hud.hxh.meditating", data.meditationTicks() / 20)), INK, 1f);
            }
            return;
        }
        if (!gaugeShown) {
            gaugeShown = true;
            gaugeSince = ticks;
        }
        // Apparition : la jauge monte depuis le bas de l'écran avec un petit rebond
        float in = easeBack((now - gaugeSince) / GAUGE_IN);
        g.pose().pushPose();
        g.pose().translate(0, (1f - in) * 40f, 0);

        // Ne déborde jamais sur la barre d'objets (petites fenêtres)
        int barW = Math.max(70, Math.min(120, g.guiWidth() / 2 - 91 - x - 12)), barH = 8;
        int barY = bottom - barH;
        float ratio = data.auraMax() > 0 ? Mth.clamp(data.aura() / data.auraMax(), 0f, 1f) : 0f;
        int color = auraColor(data);

        // Barre penchée : ombre, contour, fond blanc, aura, segments
        g.pose().pushPose();
        shear(g, x, bottom, -0.35f);
        g.fill(x + 3, barY + 3, x + barW + 3, bottom + 3, INK);
        g.fill(x - 2, barY - 2, x + barW + 2, bottom + 2, INK);
        g.fill(x, barY, x + barW, bottom, PAPER);
        g.fill(x, barY, x + Math.round(barW * ratio), bottom, color);
        // Éclair blanc quand la posture change
        float flash = 1f - (now - stateSince) / STATE_POP;
        if (flash > 0) g.fill(x, barY, x + barW, bottom, ((int) (flash * 200) << 24) | 0xFFFFFF);
        for (int i = 1; i < 10; i++) {
            int sx = x + barW * i / 10;
            g.fill(sx, barY, sx + 1, bottom, INK);
        }
        g.pose().popPose();

        // Ligne du dessus : onglet de posture, niveau de Nen, aura restante
        boolean ko = data.koUntil() > mc.level.getGameTime();
        Component state = ko ? Component.translatable("hud.hxh.ko")
                : data.healing() ? Component.translatable("hud.hxh.healing")
                : data.meditating() ? Component.translatable("netero".equals(data.kit()) ? "hud.hxh.prayer" : "hud.hxh.meditation")
                : Component.translatable(data.mode().translationKey());
        String stateText = state.getString().toUpperCase(Locale.ROOT);
        if (!stateText.equals(lastState)) {
            if (!lastState.isEmpty()) stateSince = ticks;
            lastState = stateText;
        }
        float pop = 1f + 0.45f * Math.max(0f, 1f - (now - stateSince) / STATE_POP);
        int tabY = barY - 16;
        int tabW = tab(g, x - 2, tabY, manga(stateText), ko ? 0xFF9A1E1E : INK, pop);
        Component nen = manga(Component.translatable("hud.hxh.nen_level", data.nenLevel()));
        outlined(g, nen, x + tabW + 4, tabY + 2, PAPER, 1f);
        int nenRight = x + tabW + 4 + font.width(nen);
        Component value = manga((int) data.aura() + "/" + data.auraMax());
        if (nenRight + 6 + font.width(value) > x + barW) value = manga(String.valueOf((int) data.aura()));
        if (nenRight + 6 + font.width(value) <= x + barW + 4) {
            outlined(g, value, x + barW - font.width(value), tabY + 2, PAPER, 1f);
        }
        renderKitBars(g, data, x, tabY - 4, barW);
        g.pose().popPose();
    }

    /** Jauges propres au personnage (Prière de Netero, Douleur de Feitan…), empilées au-dessus de l'onglet. */
    private static void renderKitBars(GuiGraphics g, HunterData data, int x, int top, int barW) {
        var bars = com.evandouard.hxh.hatsu.KitBars.of(data.kit());
        int y = top;
        for (var bar : bars) {
            float ratio = Mth.clamp(data.kitValue(bar.key()) / bar.max(), 0f, 1f);
            int h = 5, w = Math.round(barW * 0.8f);
            int by = y - h;
            g.pose().pushPose();
            shear(g, x, y, -0.35f);
            g.fill(x - 1, by - 1, x + w + 1, y + 1, INK);
            g.fill(x, by, x + w, y, 0xFF3A3A3A);
            g.fill(x, by, x + Math.round(w * ratio), y, bar.color());
            if (ratio >= 1f && (ticks / 5) % 2 == 0) g.fill(x, by, x + w, y, 0x55FFFFFF);
            for (int i = 1; i < 4; i++) g.fill(x + w * i / 4, by, x + w * i / 4 + 1, y, INK);
            g.pose().popPose();
            Component label = manga(Component.translatable(bar.label()).getString().toUpperCase(Locale.ROOT));
            outlined(g, label, x + w + 6, by - 2, bar.color(), 0.75f);
            y = by - 5;
        }
    }

    /** Onglet noir penché avec un texte blanc ; renvoie sa largeur. */
    private static int tab(GuiGraphics g, int x, int y, Component text, int fill, float scale) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(text) + 8;
        g.pose().pushPose();
        // pop autour du coin bas-gauche quand la posture change
        g.pose().translate(x, y + 12, 0);
        g.pose().scale(scale, scale, 1f);
        g.pose().translate(-x, -(y + 12), 0);
        g.pose().pushPose();
        shear(g, x, y + 12, -0.3f);
        g.fill(x, y, x + w, y + 12, fill);
        g.pose().popPose();
        g.drawString(font, text, x + 4, y + 2, PAPER, false);
        g.pose().popPose();
        return w;
    }

    static int auraColor(HunterData data) {
        return switch (data.mode()) {
            case TEN -> 0xFF5FD0FF;
            case ZETSU -> 0xFF8A8A8A;
            case REN -> 0xFFFF5A3C;
            case KEN -> 0xFFFFC23A;
            case IN -> 0xFFB06BFF;
            case NONE -> 0xFFE8E0C8;
        };
    }

    // ---------- Quête (bulle) et notifications ----------

    private static void renderQuestAndToasts(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        int y = 6;
        float now = ticks + partial(delta);
        framePartial = partial(delta);
        QuestStatusPayload q = QuestStatusPayload.latest;
        String key = q.isEmpty() ? "" : q.title().getString() + "|" + q.step().getString();
        if (!key.equals(bubbleKey)) {
            // Changement d'objectif : l'ancienne bulle sort, la nouvelle entre
            if (!bubbleKey.isEmpty() && lastBubble != null) {
                leaving = lastBubble;
                leavingSince = ticks;
            }
            bubbleKey = key;
            bubbleSince = ticks;
        }
        if (leaving != null) {
            float out = (now - leavingSince) / BUBBLE_OUT;
            if (out >= 1f) leaving = null;
            else if (q.isEmpty()) {
                // quête finie ou abandonnée : la bulle rétrécit et s'efface
                bubbleAnimated(g, leaving, 6, 6, 1f - ease(out), 1f - out);
            }
        }
        if (!q.isEmpty()) {
            lastBubble = q;
            float in = (now - bubbleSince) / BUBBLE_IN;
            int bottom = bubbleAnimated(g, q, 6, 6, easeBack(in), Math.min(1f, in * 2f));
            y = bottom + 12;
        }
        renderToasts(g, 6, y, partial(delta));
    }

    private static QuestStatusPayload lastBubble;

    /** Bulle à l'échelle {@code scale} autour de sa queue (elle « sort » du coin), renvoie le bas de la bulle. */
    private static int bubbleAnimated(GuiGraphics g, QuestStatusPayload q, int x, int y, float scale, float alpha) {
        if (scale <= 0.02f || alpha <= 0.02f) return y;
        g.pose().pushPose();
        float px = x + 14, py = y + 40;
        g.pose().translate(px, py, 0);
        g.pose().scale(scale, scale, 1f);
        g.pose().mulPose(Axis.ZP.rotationDegrees((1f - Math.min(1f, scale)) * -8f));
        g.pose().translate(-px, -py, 0);
        int bottom = questBubble(g, q, x, y);
        g.pose().popPose();
        return bottom;
    }

    /** Partie de tick du rendu en cours (pour que la boussole suive la souris sans à-coups). */
    private static float framePartial;

    /** Rayon de la boussole de quête. */
    static final int COMPASS_R = 13;

    /**
     * Boussole de quête : cercle d'encre avec une flèche qui pointe la cible de l'étape, par rapport à
     * l'endroit où le joueur regarde (flèche en haut = droit devant), et la distance en dessous.
     */
    private static void questCompass(GuiGraphics g, long target, int cx, int cy) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        net.minecraft.core.BlockPos t = net.minecraft.core.BlockPos.of(target);
        double dx = t.getX() + 0.5 - mc.player.getX();
        double dz = t.getZ() + 0.5 - mc.player.getZ();
        float angle = QuestStatusPayload.compassAngle(mc.player.getViewYRot(framePartial), dx, dz);
        int r = COMPASS_R;
        // disque : contour d'encre, intérieur papier
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, INK);
            int in = r - 2;
            if (Math.abs(dy) <= in) {
                int h2 = (int) Math.round(Math.sqrt(in * in - dy * dy));
                g.fill(cx - h2, cy + dy, cx + h2 + 1, cy + dy + 1, PAPER);
            }
        }
        // repère « devant »
        g.fill(cx, cy - r + 2, cx + 1, cy - r + 4, INK);
        g.pose().pushPose();
        g.pose().translate(cx + 0.5f, cy + 0.5f, 0);
        g.pose().mulPose(Axis.ZP.rotation(angle));
        // flèche : tige puis pointe triangulaire en lignes qui rétrécissent
        g.fill(-1, -2, 1, 7, INK);
        for (int i = 0; i < 6; i++) g.fill(-5 + i, -2 - i, 5 - i, -1 - i, i % 2 == 0 ? INK : 0xFFC0392B);
        g.fill(-1, -9, 1, -7, 0xFFC0392B);
        g.pose().popPose();
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, PAPER);
        // distance
        Font font = mc.font;
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        Component label = manga(dist >= 1000 ? String.format(Locale.ROOT, "%.1f km", dist / 1000f) : dist + " m");
        int lw = font.width(label);
        g.fill(cx - lw / 2 - 3, cy + r + 2, cx + lw / 2 + 3, cy + r + 13, INK);
        g.drawString(font, label, cx - lw / 2, cy + r + 4, PAPER, false);
    }


    /** Bulle de BD avec la quête suivie ; renvoie le bas de la bulle (queue comprise). */
    private static int questBubble(GuiGraphics g, QuestStatusPayload q, int x, int y) {
        Font font = Minecraft.getInstance().font;
        Component title = manga(q.title());
        Component step = q.step();
        boolean progress = q.goal() > 0;
        Component count = manga(q.progress() + "/" + q.goal());
        int w = Math.max(font.width(title), font.width(step) + (progress ? 0 : 0));
        w = Math.max(w, 90) + 16;
        int h = progress ? 40 : 30;

        // Coins arrondis : contour noir puis intérieur blanc, coins rognés
        g.fill(x + 2, y, x + w - 2, y + h, INK);
        g.fill(x, y + 2, x + w, y + h - 2, INK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, INK);
        g.fill(x + 4, y + 2, x + w - 4, y + h - 2, PAPER);
        g.fill(x + 2, y + 4, x + w - 2, y + h - 4, PAPER);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, PAPER);
        // Queue de la bulle
        int tx = x + 14;
        for (int i = 0; i < 6; i++) {
            g.fill(tx + i - 1, y + h - 1 + i, tx + 9 - i + 1, y + h + i, INK);
            if (i < 4) g.fill(tx + i + 1, y + h - 2 + i, tx + 9 - i - 1, y + h - 1 + i, PAPER);
        }

        g.drawString(font, title, x + 8, y + 6, INK, false);
        g.drawString(font, step, x + 8, y + 17, 0xFF333333, false);
        if (progress) {
            int bx = x + 8, by = y + 29, bw = w - 16 - font.width(count) - 6;
            g.fill(bx, by, bx + bw, by + 6, INK);
            g.fill(bx + 1, by + 1, bx + bw - 1, by + 5, PAPER);
            int fill = Math.round((bw - 2) * Mth.clamp(q.progress() / (float) q.goal(), 0f, 1f));
            g.fill(bx + 1, by + 1, bx + 1 + fill, by + 5, INK);
            g.drawString(font, count, bx + bw + 6, by - 1, INK, false);
        }
        if (q.hasTarget()) questCompass(g, q.target(), x + w + COMPASS_R + 6, y + COMPASS_R + 2);
        return y + Math.max(h, q.hasTarget() ? 2 * COMPASS_R + 15 : h) + 6;
    }

    private static void renderToasts(GuiGraphics g, int x, int y, float partial) {
        Font font = Minecraft.getInstance().font;
        float now = ticks + partial;
        Iterator<Toast> it = TOASTS.iterator();
        while (it.hasNext()) if (now - it.next().start() > TOAST_IN + TOAST_STAY + TOAST_OUT) it.remove();
        int i = 0;
        for (Toast t : TOASTS) {
            float age = now - t.start();
            // glisse depuis la gauche, puis repart
            float slide = age < TOAST_IN ? 1f - easeBack(age / TOAST_IN)
                    : age > TOAST_IN + TOAST_STAY ? ease((age - TOAST_IN - TOAST_STAY) / TOAST_OUT) : 0f;
            float tilt = age > TOAST_IN + TOAST_STAY ? -10f * ease((age - TOAST_IN - TOAST_STAY) / TOAST_OUT) : 0f;
            Component badge = manga(t.badge());
            Component title = manga(t.title());
            int badgeW = Math.max(22, font.width(badge) * 2 + 4); // badge dessiné en ×2
            int textX = 8 + badgeW + 6;
            int w = Math.max(font.width(title) + 4, font.width(t.subtitle())) + textX + 8;
            int h = 26;
            float dx = -slide * (w + 12);
            g.pose().pushPose();
            g.pose().translate(x + dx, y, 0);
            g.pose().rotateAround(Axis.ZP.rotationDegrees((i % 2 == 0 ? -1.5f : 1f) + tilt), w / 2f, h / 2f, 0);
            panel(g, 0, 0, w, h, PAPER, 3);
            outlined(g, badge, 7, 4, t.color(), 2f);
            g.drawString(font, title, textX, 4, INK, false);
            g.drawString(font, t.subtitle(), textX, 15, GREY, false);
            g.pose().popPose();
            y += h + 8;
            i++;
        }
    }

    // ---------- Gros lettrage au centre ----------

    private static void renderCallout(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Callout c = callout;
        if (c == null || mc.options.hideGui) return;
        float age = ticks + partial(delta) - c.start();
        if (age > CALLOUT_TICKS) {
            callout = null;
            return;
        }
        String text = c.text().getString().toUpperCase(Locale.ROOT);
        Component comp = manga(text);
        float base = c.big() ? 2.5f : 1.6f;
        // apparition avec dépassement (pop), puis petite montée en sortie
        float pop = age < 4 ? Mth.lerp(ease(age / 4f), 1.7f, 1f) : 1f;
        float rise = age > CALLOUT_TICKS - 8 ? (age - CALLOUT_TICKS + 8) * 1.5f : 0f;
        float out = age > CALLOUT_TICKS - 6 ? (age - CALLOUT_TICKS + 6) / 6f : 0f;
        float scale = base * pop * (1f - 0.35f * ease(out));
        int width = mc.font.width(comp);
        float cx = g.guiWidth() / 2f;
        float cy = g.guiHeight() * 0.66f - rise;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-4f));
        outlined(g, comp, -width * scale / 2f, -4 * scale, c.color(), scale, 1f - ease(out));
        g.pose().popPose();
    }

    /** Entrée avec léger dépassement (rebond). */
    static float easeBack(float x) {
        x = Mth.clamp(x, 0f, 1f);
        float c1 = 1.70158f, c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }
}
