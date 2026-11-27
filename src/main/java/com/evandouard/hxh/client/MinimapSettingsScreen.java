package com.evandouard.hxh.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Réglages de la minimap (ouverts depuis le carnet, touche O) : on la fait glisser à la souris pour la déplacer,
 * des boutons activent/cachent, changent la taille et le zoom, ou remettent la place par défaut (haut droite).
 */
public class MinimapSettingsScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private int grabX, grabY;
    private Button toggle;

    public MinimapSettingsScreen(Screen parent) {
        super(Component.translatable("screen.hxh.minimap.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height - 74;
        toggle = addRenderableWidget(Button.builder(toggleLabel(), b -> {
            MinimapClient.setEnabled(!MinimapClient.enabled());
            b.setMessage(toggleLabel());
        }).bounds(cx - 154, y, 152, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.hxh.minimap.reset"), b -> {
            MinimapClient.setPosition(1, 0);
            MinimapClient.setSize(100);
            MinimapClient.setZoom(3);
        }).bounds(cx + 2, y, 152, 20).build());
        addRenderableWidget(Button.builder(Component.literal("−"), b -> MinimapClient.setSize(HxHClientConfig.MINIMAP_SIZE.get() - 12))
                .bounds(cx - 154, y + 24, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.hxh.minimap.size"), b -> { }).bounds(cx - 132, y + 24, 60, 20).build()).active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> MinimapClient.setSize(HxHClientConfig.MINIMAP_SIZE.get() + 12))
                .bounds(cx - 70, y + 24, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("−"), b -> MinimapClient.setZoom(HxHClientConfig.MINIMAP_ZOOM.get() - 1))
                .bounds(cx + 2, y + 24, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.hxh.minimap.zoom"), b -> { }).bounds(cx + 24, y + 24, 60, 20).build()).active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> MinimapClient.setZoom(HxHClientConfig.MINIMAP_ZOOM.get() + 1))
                .bounds(cx + 86, y + 24, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx + 110, y + 24, 44, 20).build());
    }

    private static Component toggleLabel() {
        return Component.translatable(MinimapClient.enabled() ? "screen.hxh.minimap.hide" : "screen.hxh.minimap.show");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0x90000000);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.hxh.minimap.hint"), width / 2, 24, 0xFFC8C8C8);
        Minecraft mc = Minecraft.getInstance();
        int[] r = MinimapClient.rect(width, height);
        if (mc.player != null) MinimapClient.draw(g, mc, r[0], r[1], r[2]);
        // Contour clair : c'est cette zone qu'on attrape
        int c = dragging ? 0xFFFFE9A8 : 0xFFFFFFFF;
        g.fill(r[0] - 3, r[1] - 3, r[0] + r[2] + 3, r[1] - 2, c);
        g.fill(r[0] - 3, r[1] + r[2] + 2, r[0] + r[2] + 3, r[1] + r[2] + 3, c);
        g.fill(r[0] - 3, r[1] - 3, r[0] - 2, r[1] + r[2] + 3, c);
        g.fill(r[0] + r[2] + 2, r[1] - 3, r[0] + r[2] + 3, r[1] + r[2] + 3, c);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int[] r = MinimapClient.rect(width, height);
        if (button == 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[2]) {
            dragging = true;
            grabX = (int) mx - r[0];
            grabY = (int) my - r[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging) return super.mouseDragged(mx, my, button, dx, dy);
        int size = MinimapClient.rect(width, height)[2];
        double rangeX = width - size - 12, rangeY = height - size - 12;
        MinimapClient.setPosition(rangeX <= 0 ? 0 : (mx - grabX - 6) / rangeX, rangeY <= 0 ? 0 : (my - grabY - 6) / rangeY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
