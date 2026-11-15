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
    /** Fiches du serveur, puis celles du bestiaire (toujours lisibles, traduites ici). */
    private final List<OpenComputerPayload.Entry> entries = new ArrayList<>();
    /** Lignes de la liste : en-têtes de catégorie (index -1) ou fiches. */
    private final List<Row> rows = new ArrayList<>();
    private int selected = -1;
    private int listScroll, textScroll;

    private record Row(String label, int entry) {}

    public HunterComputerScreen(OpenComputerPayload data) {
        super(Component.translatable("screen.hxh.computer.title"));
        this.data = data;
        entries.addAll(data.entries());
        entries.addAll(ComputerRecipes.entries());
        entries.addAll(Bestiary.computerEntries());
        String category = null;
        for (int i = 0; i < entries.size(); i++) {
            OpenComputerPayload.Entry e = entries.get(i);
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
            OpenComputerPayload.Entry e = entries.get(row.entry());
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
        OpenComputerPayload.Entry e = entries.get(selected);
        if (!e.unlocked()) {
            MangaHud.outlined(g, MangaHud.manga(Component.translatable("screen.hxh.computer.locked")), tx, ty, 0xFFFF6B6B, 1.1f);
            if (!e.hint().isEmpty()) g.drawWordWrap(font, Component.literal(e.hint()), tx, ty + 18, tw, 0xFF555555);
            return;
        }
        g.drawString(font, Component.literal(e.title()).withStyle(s -> s.withBold(true)), tx, ty, INK, false);
        g.fill(tx, ty + 11, tx + tw, ty + 12, INK);
        // Recette : la grille d'artisanat avec les icônes, puis le résultat
        ComputerRecipes.Layout layout = ComputerRecipes.layout(e.id());
        int textTop = ty + 16;
        if (layout != null) textTop = drawRecipe(g, layout, tx, ty + 16, mouseX, mouseY) + 6;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String para : e.text().split("\n")) {
            if (para.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(para), tw));
        }
        int maxLines = (y + h - 14 - textTop) / 10;
        textScroll = Math.max(0, Math.min(textScroll, lines.size() - maxLines));
        for (int i = 0; i < maxLines && i + textScroll < lines.size(); i++) {
            g.drawString(font, lines.get(i + textScroll), tx, textTop + i * 10, 0xFF1D2B22, false);
        }
        if (layout != null) recipeTooltip(g, layout, tx, ty + 16, mouseX, mouseY);
        if (lines.size() > maxLines) {
            g.drawString(font, (textScroll > 0 ? "▲" : " ") + (textScroll + maxLines < lines.size() ? "▼" : ""), x + w - 22, y + h - 20, INK, false);
        }
    }

    private static final int SLOT = 18;

    /** Haut gauche de la grille de la recette (sous la ligne qui dit où la fabriquer). */
    private int gridTop(int top) {
        return top + 12;
    }

    /** Une case : cadre encré, fond clair, comme un inventaire de manga. */
    private static void slot(GuiGraphics g, int x, int y, boolean big) {
        int s = big ? SLOT + 8 : SLOT;
        g.fill(x, y, x + s, y + s, INK);
        g.fill(x + 1, y + 1, x + s - 1, y + s - 1, 0xFF8B8B8B); // gris des cases de Minecraft : la vitre se voit
    }

    /** L'objet montré pour un ingrédient : les variantes défilent toutes les secondes (« n'importe quelle planche »). */
    private static net.minecraft.world.item.ItemStack shown(net.minecraft.world.item.crafting.Ingredient ing) {
        net.minecraft.world.item.ItemStack[] items = ing.getItems();
        if (items.length == 0) return net.minecraft.world.item.ItemStack.EMPTY;
        return items[(int) ((net.minecraft.Util.getMillis() / 1000) % items.length)];
    }

    /** Dessine la grille, la flèche et le résultat. Renvoie le bas de la zone dessinée. */
    private int drawRecipe(GuiGraphics g, ComputerRecipes.Layout l, int x, int top, int mouseX, int mouseY) {
        Component kind = l.cooking() ? Component.translatable(l.kindKey()).append(" ")
                .append(Component.translatable("screen.hxh.computer.recipe_time", l.cookSeconds()))
                : Component.translatable(l.kindKey());
        g.drawString(font, kind, x, top, 0xFF1D2B22, false);
        int gy = gridTop(top);
        int rows = l.cooking() ? 1 : 3;
        int gridW = l.width() * SLOT;
        for (int i = 0; i < l.grid().size(); i++) {
            int sx = x + (i % l.width()) * SLOT, sy = gy + (i / l.width()) * SLOT;
            slot(g, sx, sy, false);
            net.minecraft.world.item.ItemStack st = shown(l.grid().get(i));
            if (!st.isEmpty()) g.renderItem(st, sx + 1, sy + 1);
        }
        int midY = gy + rows * SLOT / 2;
        // flèche
        int ax = x + gridW + 8;
        g.fill(ax, midY - 1, ax + 18, midY + 1, INK);
        for (int k = 0; k < 4; k++) g.fill(ax + 14 + k, midY - 4 + k, ax + 15 + k, midY + 4 - k, INK);
        int rx = ax + 26, ry = midY - (SLOT + 8) / 2;
        slot(g, rx, ry, true);
        g.renderItem(l.result(), rx + 5, ry + 5);
        g.renderItemDecorations(font, l.result(), rx + 5, ry + 5);
        return gy + rows * SLOT;
    }

    /** Info-bulle de l'objet survolé dans la grille ou le résultat. */
    private void recipeTooltip(GuiGraphics g, ComputerRecipes.Layout l, int x, int top, int mouseX, int mouseY) {
        int gy = gridTop(top);
        for (int i = 0; i < l.grid().size(); i++) {
            int sx = x + (i % l.width()) * SLOT, sy = gy + (i / l.width()) * SLOT;
            if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT) {
                net.minecraft.world.item.ItemStack st = shown(l.grid().get(i));
                if (!st.isEmpty()) g.renderTooltip(font, st, mouseX, mouseY);
                return;
            }
        }
        int rows = l.cooking() ? 1 : 3;
        int midY = gy + rows * SLOT / 2;
        int rx = x + l.width() * SLOT + 34, ry = midY - (SLOT + 8) / 2;
        if (mouseX >= rx && mouseX < rx + SLOT + 8 && mouseY >= ry && mouseY < ry + SLOT + 8) {
            g.renderTooltip(font, l.result(), mouseX, mouseY);
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
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id().equals(id)) {
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
