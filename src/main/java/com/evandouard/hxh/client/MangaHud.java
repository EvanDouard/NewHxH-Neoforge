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
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        FormattedCharSequence seq = text.getVisualOrderText();
        font.drawInBatch8xOutline(seq, 0, 0, color, INK, g.pose().last().pose(), g.bufferSource(), 0xF000F0);
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

        if (!data.nenUnlocked()) {
            if (data.meditating()) {
                tab(g, x, bottom - 12, Component.translatable("hud.hxh.meditating", data.meditationTicks() / 20), INK);
            }
            return;
        }

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
        for (int i = 1; i < 10; i++) {
            int sx = x + barW * i / 10;
            g.fill(sx, barY, sx + 1, bottom, INK);
        }
        g.pose().popPose();

        // Ligne du dessus : onglet de posture, niveau de Nen, aura restante
        boolean ko = data.koUntil() > mc.level.getGameTime();
        Component state = ko ? Component.translatable("hud.hxh.ko")
                : data.healing() ? Component.translatable("hud.hxh.healing")
                : data.meditating() ? Component.translatable("hud.hxh.meditation")
                : Component.translatable(data.mode().translationKey());
        int tabY = barY - 16;
        int tabW = tab(g, x - 2, tabY, Component.literal(state.getString().toUpperCase(Locale.ROOT)), ko ? 0xFF9A1E1E : INK);
        outlined(g, Component.translatable("hud.hxh.nen_level", data.nenLevel()), x + tabW + 4, tabY + 3, PAPER, 1f);
        int nenRight = x + tabW + 4 + font.width(Component.translatable("hud.hxh.nen_level", data.nenLevel()));
        Component value = Component.literal((int) data.aura() + "/" + data.auraMax());
        if (nenRight + 6 + font.width(value) > x + barW) value = Component.literal(String.valueOf((int) data.aura()));
        if (nenRight + 6 + font.width(value) <= x + barW + 4) {
            outlined(g, value, x + barW - font.width(value), tabY + 3, PAPER, 1f);
        }
    }

    /** Onglet noir penché avec un texte blanc ; renvoie sa largeur. */
    private static int tab(GuiGraphics g, int x, int y, Component text, int fill) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(text) + 8;
        g.pose().pushPose();
        shear(g, x, y + 12, -0.3f);
        g.fill(x, y, x + w, y + 12, fill);
        g.pose().popPose();
        g.drawString(font, text, x + 4, y + 2, PAPER, false);
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
        QuestStatusPayload q = QuestStatusPayload.latest;
        if (!q.titleKey().isEmpty()) y = questBubble(g, q, 6, 6) + 12;
        renderToasts(g, 6, y, partial(delta));
    }

    /** Bulle de BD avec la quête suivie ; renvoie le bas de la bulle (queue comprise). */
    private static int questBubble(GuiGraphics g, QuestStatusPayload q, int x, int y) {
        Font font = Minecraft.getInstance().font;
        Component title = Component.translatable(q.titleKey()).copy().withStyle(ChatFormatting.BOLD);
        Component step = q.stepArg().isEmpty() ? Component.translatable(q.stepKey()) : Component.translatable(q.stepKey(), q.stepArg());
        boolean progress = q.goal() > 0;
        Component count = Component.literal(q.progress() + "/" + q.goal());
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
        return y + h + 6;
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
            float slide = age < TOAST_IN ? 1f - ease(age / TOAST_IN)
                    : age > TOAST_IN + TOAST_STAY ? ease((age - TOAST_IN - TOAST_STAY) / TOAST_OUT) : 0f;
            int badgeW = Math.max(22, font.width(t.badge()) * 2 + 4); // badge dessiné en ×2
            int textX = 8 + badgeW + 6;
            int w = Math.max(font.width(t.title()) + 4, font.width(t.subtitle())) + textX + 8;
            int h = 26;
            float dx = -slide * (w + 12);
            g.pose().pushPose();
            g.pose().translate(x + dx, y, 0);
            g.pose().rotateAround(Axis.ZP.rotationDegrees(i % 2 == 0 ? -1.5f : 1f), w / 2f, h / 2f, 0);
            panel(g, 0, 0, w, h, PAPER, 3);
            outlined(g, Component.literal(t.badge()), 7, 6, t.color(), 2f);
            g.drawString(font, t.title().copy().withStyle(ChatFormatting.BOLD), textX, 5, INK, false);
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
        Component comp = Component.literal(text);
        float base = c.big() ? 2.5f : 1.6f;
        // apparition avec dépassement (pop), puis petite montée en sortie
        float pop = age < 4 ? Mth.lerp(ease(age / 4f), 1.7f, 1f) : 1f;
        float rise = age > CALLOUT_TICKS - 8 ? (age - CALLOUT_TICKS + 8) * 1.5f : 0f;
        float scale = base * pop;
        int width = mc.font.width(comp);
        float cx = g.guiWidth() / 2f;
        float cy = g.guiHeight() * 0.66f - rise;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-4f));
        outlined(g, comp, -width * scale / 2f, -4 * scale, c.color(), scale);
        g.pose().popPose();
    }

    static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }
}
