package com.evandouard.hxh.client;

import com.evandouard.hxh.auction.Auctions;
import com.evandouard.hxh.network.AuctionBidPayload;
import com.evandouard.hxh.network.OpenAuctionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Salle des ventes, style manga (comme la boutique) : un lot par ligne, son prix, qui mène, et le bouton pour
 * enchérir. Chaque clic n'envoie qu'une intention ; le serveur répond avec la vente à jour.
 */
public class AuctionScreen extends Screen {
    private static final int W = 320, ROW = 24;
    private static final int INK = 0xFF111111, PAPER = 0xFFFFFBF0, JENNY = 0xFFFFC23A;

    private OpenAuctionPayload data;

    public AuctionScreen(OpenAuctionPayload data) {
        super(Component.translatable("npc.hxh." + data.npc()));
        this.data = data;
    }

    public String auctionId() {
        return data.auction();
    }

    public void update(OpenAuctionPayload payload) {
        this.data = payload;
        rebuildWidgets();
    }

    private int panelHeight() {
        return 52 + Math.max(1, data.lots().size()) * ROW;
    }

    private int top() {
        return Math.max(4, height / 2 - panelHeight() / 2);
    }

    private int left() {
        return width / 2 - W / 2;
    }

    @Override
    protected void init() {
        int x = left(), y = top() + 44;
        List<OpenAuctionPayload.Line> lots = data.lots();
        for (int i = 0; i < lots.size(); i++) {
            OpenAuctionPayload.Line l = lots.get(i);
            int slot = i;
            boolean won = l.status() == Auctions.WON_BY_YOU;
            Button b = addRenderableWidget(Button.builder(won ? Component.translatable("auction.hxh.sold")
                                    : Component.translatable("auction.hxh.bid", l.nextBid()),
                            btn -> PacketDistributor.sendToServer(new AuctionBidPayload(data.auction(), slot)))
                    .bounds(x + W - 98, y + i * ROW, 90, 18).build());
            b.active = !won && data.jenny() >= l.nextBid();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        MangaHud.panel(g, left(), top(), W, panelHeight(), PAPER, 4);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left(), y = top();
        MangaHud.outlined(g, MangaHud.manga(title), x + 10, y + 7, 0xFFFFFFFF, 1.2f);
        Component jenny = MangaHud.manga(Component.translatable("shop.hxh.jenny", data.jenny()));
        MangaHud.outlined(g, jenny, x + W - 12 - font.width(jenny) * 1.2f, y + 7, JENNY, 1.2f);
        g.drawString(font, Component.translatable("auction.hxh.today"), x + 10, y + 30, INK, false);
        int rowY = y + 44;
        List<OpenAuctionPayload.Line> lots = data.lots();
        for (int i = 0; i < lots.size(); i++) {
            OpenAuctionPayload.Line l = lots.get(i);
            int ry = rowY + i * ROW;
            ItemStack stack = l.stack();
            g.renderItem(stack, x + 8, ry);
            g.renderItemDecorations(font, stack, x + 8, ry);
            g.drawString(font, font.plainSubstrByWidth(stack.getHoverName().getString(), 170), x + 28, ry, INK, false);
            Component status = switch (l.status()) {
                case Auctions.WON_BY_YOU -> Component.translatable("auction.hxh.status.won", l.price());
                case Auctions.RIVAL_LEADS -> Component.translatable("auction.hxh.status.rival", l.price());
                default -> Component.translatable("auction.hxh.status.open", l.price());
            };
            int color = l.status() == Auctions.WON_BY_YOU ? 0xFF2E8B57 : l.status() == Auctions.RIVAL_LEADS ? 0xFFC0392B : 0xFF7A5A00;
            g.drawString(font, status, x + 28, ry + 9, color, false);
            if (mouseX >= x + 8 && mouseX < x + 24 && mouseY >= ry && mouseY < ry + 16) g.renderTooltip(font, stack, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
