package com.evandouard.hxh.client;

import com.evandouard.hxh.network.OpenShopPayload;
import com.evandouard.hxh.network.ShopActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Boutique d'un marchand, style manga : à gauche ce qu'il vend, à droite ce qu'il rachète, les Jenny en haut.
 * Chaque bouton n'envoie qu'une intention ; le serveur répond avec la boutique à jour.
 */
public class ShopScreen extends Screen {
    private static final int W = 400, ROW = 19;
    private static final int INK = 0xFF111111, PAPER = 0xFFFFFBF0, JENNY = 0xFFFFC23A;

    private OpenShopPayload data;
    /** Défilement (molette) de chaque colonne. */
    private int scrollSells, scrollBuys;

    public ShopScreen(OpenShopPayload data) {
        super(Component.translatable("npc.hxh." + data.npc()));
        this.data = data;
    }

    public String shopId() {
        return data.shop();
    }

    public void update(OpenShopPayload payload) {
        this.data = payload;
        rebuildWidgets();
    }

    /** Lignes visibles : autant que l'écran en permet (petite fenêtre ou grande interface → on fait défiler). */
    private int rows() {
        return Math.max(3, Math.min(8, (height - 70) / ROW));
    }

    private int panelHeight() {
        return 44 + rows() * ROW;
    }

    private int top() {
        return Math.max(4, height / 2 - panelHeight() / 2);
    }

    private int left() {
        return width / 2 - W / 2;
    }

    @Override
    protected void init() {
        int x = left(), y = top() + 40;
        List<OpenShopPayload.Line> sells = data.sells();
        scrollSells = Math.max(0, Math.min(scrollSells, sells.size() - rows()));
        scrollBuys = Math.max(0, Math.min(scrollBuys, data.buys().size() - rows()));
        for (int r = 0; r < rows() && r + scrollSells < sells.size(); r++) {
            int index = r + scrollSells;
            Button b = addRenderableWidget(Button.builder(Component.translatable("shop.hxh.buy"),
                    btn -> PacketDistributor.sendToServer(new ShopActionPayload(data.shop(), index, false, false)))
                    .bounds(x + W / 2 - 52, y + r * ROW, 44, 17).build());
            b.active = data.jenny() >= sells.get(index).price();
        }
        List<OpenShopPayload.Line> buys = data.buys();
        for (int r = 0; r < rows() && r + scrollBuys < buys.size(); r++) {
            int index = r + scrollBuys;
            boolean has = buys.get(index).owned() > 0;
            Button one = addRenderableWidget(Button.builder(Component.translatable("shop.hxh.sell"),
                    btn -> PacketDistributor.sendToServer(new ShopActionPayload(data.shop(), index, true, false)))
                    .bounds(x + W - 80, y + r * ROW, 38, 17).build());
            Button all = addRenderableWidget(Button.builder(Component.translatable("shop.hxh.sell_all"),
                    btn -> PacketDistributor.sendToServer(new ShopActionPayload(data.shop(), index, true, true)))
                    .bounds(x + W - 40, y + r * ROW, 32, 17).build());
            one.active = has;
            all.active = has;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = left(), y = top();
        int h = panelHeight();
        MangaHud.panel(g, x, y, W, h, PAPER, 4);
        // séparation des deux colonnes, trait encré
        g.fill(x + W / 2 - 1, y + 30, x + W / 2 + 1, y + h - 8, INK);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left(), y = top();
        MangaHud.outlined(g, MangaHud.manga(title), x + 10, y + 7, 0xFFFFFFFF, 1.2f);
        Component jenny = MangaHud.manga(Component.translatable("shop.hxh.jenny", data.jenny()));
        MangaHud.outlined(g, jenny, x + W - 12 - font.width(jenny) * 1.2f, y + 7, JENNY, 1.2f);
        g.drawString(font, Component.translatable("shop.hxh.sells"), x + 10, y + 28, INK, false);
        g.drawString(font, Component.translatable("shop.hxh.buys"), x + W / 2 + 8, y + 28, INK, false);
        int rowY = y + 40;
        List<OpenShopPayload.Line> sells = data.sells();
        for (int r = 0; r < rows() && r + scrollSells < sells.size(); r++) {
            OpenShopPayload.Line l = sells.get(r + scrollSells);
            int ry = rowY + r * ROW;
            item(g, l.stack(), x + 8, ry, mouseX, mouseY);
            g.drawString(font, font.plainSubstrByWidth(l.stack().getHoverName().getString(), 112), x + 28, ry, INK, false);
            int price = data.jenny() >= l.price() ? 0xFF7A5A00 : 0xFFC0392B;
            g.drawString(font, Component.translatable("shop.hxh.price", l.price()), x + 28, ry + 9, price, false);
        }
        List<OpenShopPayload.Line> buys = data.buys();
        for (int r = 0; r < rows() && r + scrollBuys < buys.size(); r++) {
            OpenShopPayload.Line l = buys.get(r + scrollBuys);
            int ry = rowY + r * ROW, bx = x + W / 2 + 6;
            item(g, l.stack(), bx, ry, mouseX, mouseY);
            g.drawString(font, font.plainSubstrByWidth(l.stack().getHoverName().getString(), 92), bx + 20, ry,
                    l.owned() > 0 ? INK : 0xFF999999, false);
            g.drawString(font, Component.translatable("shop.hxh.unit", l.price(), l.owned()), bx + 20, ry + 9,
                    0xFF7A5A00, false);
        }
        // flèches de défilement
        if (sells.size() > rows()) g.drawString(font, (scrollSells > 0 ? "▲ " : "") + (scrollSells + rows() < sells.size() ? "▼" : ""),
                x + W / 2 - 22, y + 28, INK, false);
        if (buys.size() > rows()) g.drawString(font, (scrollBuys > 0 ? "▲ " : "") + (scrollBuys + rows() < buys.size() ? "▼" : ""),
                x + W - 22, y + 28, INK, false);
        if (sells.isEmpty()) g.drawString(font, Component.translatable("shop.hxh.empty"), x + 10, rowY, 0xFF999999, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        int step = dy > 0 ? -1 : 1;
        if (mouseX < left() + W / 2) scrollSells += step;
        else scrollBuys += step;
        rebuildWidgets();
        return true;
    }

    private void item(GuiGraphics g, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        g.renderItem(stack, x, y);
        g.renderItemDecorations(font, stack, x, y);
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) g.renderTooltip(font, stack, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
