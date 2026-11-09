package com.evandouard.hxh.client;

import com.evandouard.hxh.network.OpenComputerPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordinateur Hunter, style manga : la liste des fiches à gauche (par catégorie, les verrouillées en « ??? » avec leur
 * indice), la fiche choisie à droite. Molette pour faire défiler la liste ou le texte.
 */
public class HunterComputerScreen extends Screen {
    private static final int INK = 0xFF111111, PAPER = 0xFFFFFBF0, SCREEN = 0xFFE8F4EC, ACCENT = 0xFF2E8B57;
    private static final int LIST_W = 130, LINE = 11;

    private final OpenComputerPayload data;
    /** Lignes de la liste : en-têtes de catégorie (index -1) ou fiches. */
    private final List<Row> rows = new ArrayList<>();
    private int selected = -1;
    private int listScroll, textScroll;

    private record Row(String label, int entry) {}

    public HunterComputerScreen(OpenComputerPayload data) {
        super(Component.translatable("screen.hxh.computer.title"));
        this.data = data;
        String category = null;
        for (int i = 0; i < data.entries().size(); i++) {
            OpenComputerPayload.Entry e = data.entries().get(i);
            if (!e.category().equals(category)) {
                category = e.category();
                rows.add(new Row(category, -1));
            }
            rows.add(new Row(e.title(), i));
            if (selected < 0 && e.unlocked()) selected = i;
        }
    }

    private int panelW() {
        return Math.min(420, width - 20);
    }

    private int panelH() {
        return Math.min(240, height - 20);
    }

    private int left() {
        return width / 2 - panelW() / 2;
    }

    private int top() {
        return height / 2 - panelH() / 2;
    }

    private int visibleRows() {
        return (panelH() - 44) / LINE;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = left(), y = top(), w = panelW(), h = panelH();
        MangaHud.panel(g, x, y, w, h, PAPER, 4);
        // bandeau du site
        g.fill(x + 2, y + 2, x + w - 2, y + 24, ACCENT);
        g.fill(x + 2, y + 24, x + w - 2, y + 26, INK);
        // écran de lecture
        g.fill(x + LIST_W + 8, y + 32, x + w - 8, y + h - 8, INK);
        g.fill(x + LIST_W + 10, y + 34, x + w - 10, y + h - 10, SCREEN);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left(), y = top(), w = panelW(), h = panelH();
        MangaHud.outlined(g, MangaHud.manga(title), x + 8, y + 6, 0xFFFFFFFF, 1.3f);
        Component status = Component.translatable(data.hunter() ? "screen.hxh.computer.hunter" : "screen.hxh.computer.guest");
        g.drawString(font, status, x + w - 8 - font.width(status), y + 9, data.hunter() ? 0xFFFFE27A : 0xFFFFFFFF, true);

        // liste
        int ly = y + 32;
        int vis = visibleRows();
        listScroll = Math.max(0, Math.min(listScroll, rows.size() - vis));
        for (int r = 0; r < vis && r + listScroll < rows.size(); r++) {
            Row row = rows.get(r + listScroll);
            int ry = ly + r * LINE;
            if (row.entry() < 0) {
                g.drawString(font, font.plainSubstrByWidth(row.label().toUpperCase(), LIST_W), x + 8, ry, ACCENT, false);
                continue;
            }
            OpenComputerPayload.Entry e = data.entries().get(row.entry());
            boolean sel = row.entry() == selected;
            if (sel) g.fill(x + 6, ry - 1, x + LIST_W + 4, ry + LINE - 1, INK);
            int color = !e.unlocked() ? 0xFF999999 : sel ? 0xFFFFFFFF : INK;
            String label = (e.unlocked() ? "• " : "? ") + e.title();
            g.drawString(font, font.plainSubstrByWidth(label, LIST_W - 6), x + 12, ry, color, false);
        }
        if (rows.size() > vis) {
            g.drawString(font, (listScroll > 0 ? "▲" : " ") + (listScroll + vis < rows.size() ? "▼" : ""), x + LIST_W - 10, y + h - 14, INK, false);
        }

        // fiche
        int tx = x + LIST_W + 16, tw = w - LIST_W - 32, ty = y + 40;
        if (selected < 0) {
            g.drawWordWrap(font, Component.translatable("screen.hxh.computer.empty"), tx, ty, tw, 0xFF555555);
            return;
        }
        OpenComputerPayload.Entry e = data.entries().get(selected);
        if (!e.unlocked()) {
            MangaHud.outlined(g, MangaHud.manga(Component.translatable("screen.hxh.computer.locked")), tx, ty, 0xFFFF6B6B, 1.1f);
            if (!e.hint().isEmpty()) g.drawWordWrap(font, Component.literal(e.hint()), tx, ty + 18, tw, 0xFF555555);
            return;
        }
        g.drawString(font, Component.literal(e.title()).withStyle(s -> s.withBold(true)), tx, ty, INK, false);
        g.fill(tx, ty + 11, tx + tw, ty + 12, INK);
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String para : e.text().split("\n")) {
            if (para.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(para), tw));
        }
        int maxLines = (y + h - 14 - (ty + 16)) / 10;
        textScroll = Math.max(0, Math.min(textScroll, lines.size() - maxLines));
        for (int i = 0; i < maxLines && i + textScroll < lines.size(); i++) {
            g.drawString(font, lines.get(i + textScroll), tx, ty + 16 + i * 10, 0xFF1D2B22, false);
        }
        if (lines.size() > maxLines) {
            g.drawString(font, (textScroll > 0 ? "▲" : " ") + (textScroll + maxLines < lines.size() ? "▼" : ""), x + w - 22, y + h - 20, INK, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = left(), ly = top() + 32;
        if (mouseX >= x + 6 && mouseX < x + LIST_W + 4 && mouseY >= ly) {
            int r = (int) ((mouseY - ly) / LINE) + listScroll;
            if (r >= 0 && r < rows.size() && rows.get(r).entry() >= 0 && (int) ((mouseY - ly) / LINE) < visibleRows()) {
                selected = rows.get(r).entry();
                textScroll = 0;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        int step = dy > 0 ? -1 : 1;
        if (mouseX < left() + LIST_W + 6) listScroll += step;
        else textScroll += step;
        return true;
    }

    /** Pour les tests du scénario : choisit la fiche dont l'identifiant est donné. */
    public boolean select(String id) {
        for (int i = 0; i < data.entries().size(); i++) {
            if (data.entries().get(i).id().equals(id)) {
                selected = i;
                textScroll = 0;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
