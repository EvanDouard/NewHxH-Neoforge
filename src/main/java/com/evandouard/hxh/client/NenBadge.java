package com.evandouard.hxh.client;

import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.hatsu.NenAffinity;
import com.evandouard.hxh.hatsu.Technique;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Pastille de type de Nen : petit rond de couleur avec le kanji de la catégorie (強 Renforcement vert, 変
 * Transformation violet, 具 Matérialisation rouge, 特 Spécialisation bleu, 操 Manipulation gris, 放 Émission jaune,
 * 体 Physique marron).
 */
public final class NenBadge {
    /** Diamètre d'une pastille, écart entre deux. */
    public static final int SIZE = 11, GAP = 2;

    private NenBadge() {}

    /** Largeur des pastilles d'une technique (une ou deux). */
    public static int width(Technique t) {
        int n = NenAffinity.badges(t).size();
        return n * SIZE + (n - 1) * GAP;
    }

    /** Dessine les pastilles de la technique à partir de (x, y), coin haut gauche. */
    public static void draw(GuiGraphics g, Font font, Technique t, int x, int y) {
        for (NenType type : NenAffinity.badges(t)) {
            draw(g, font, type, x, y);
            x += SIZE + GAP;
        }
    }

    /** Une pastille : disque bordé d'encre, kanji blanc au centre (police de secours de Minecraft, réduite). */
    public static void draw(GuiGraphics g, Font font, @Nullable NenType type, int x, int y) {
        int color = NenAffinity.color(type);
        float r = SIZE / 2f;
        for (int dy = 0; dy < SIZE; dy++) {
            float fy = dy + 0.5f - r;
            int half = (int) Math.floor(Math.sqrt(Math.max(0, r * r - fy * fy)) + 0.25f);
            if (half <= 0) continue;
            int inner = (int) Math.floor(Math.sqrt(Math.max(0, (r - 1) * (r - 1) - fy * fy)) + 0.25f);
            g.fill(x + (int) r - half, y + dy, x + (int) r + half + 1, y + dy + 1, 0xFF1A1A1A);
            if (inner > 0) g.fill(x + (int) r - inner + 1, y + dy, x + (int) r + inner, y + dy + 1, color);
        }
        String k = NenAffinity.kanji(type);
        var pose = g.pose();
        pose.pushPose();
        float scale = 0.75f;
        float w = font.width(k) * scale;
        pose.translate(x + SIZE / 2f - w / 2f, y + SIZE / 2f - font.lineHeight * scale / 2f + 0.5f, 0);
        pose.scale(scale, scale, 1);
        g.drawString(font, k, 0, 0, 0xFFFFFFFF, true);
        pose.popPose();
    }

    /** Texte en couleur pour les listes : « 強 Renforcement + 放 Émission » ou « 体 Physique ». */
    public static MutableComponent label(Technique t) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (NenType type : NenAffinity.badges(t)) {
            if (!first) out.append(Component.literal(" + ").withStyle(net.minecraft.ChatFormatting.GRAY));
            int c = NenAffinity.color(type) & 0xFFFFFF;
            out.append(Component.literal(NenAffinity.kanji(type) + " ").withStyle(s -> s.withColor(c).withBold(true)));
            out.append(Component.translatable(type == null ? "nen.hxh.physical" : type.translationKey()).withStyle(s -> s.withColor(c)));
            first = false;
        }
        return out;
    }
}
