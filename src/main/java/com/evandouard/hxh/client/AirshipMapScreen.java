package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.network.AirshipTravelPayload;
import com.evandouard.hxh.network.OpenAirshipPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Carte du monde de Hunter x Hunter : les destinations du dirigeable sont des repères ; un clic ouvre la confirmation
 * (nom + prix), puis envoie l'intention au serveur, qui vérifie borne, prix et destination.
 */
public class AirshipMapScreen extends Screen {
    private static final ResourceLocation MAP = HxHMod.id("textures/gui/world_map.png");
    private static final int TEX_W = 1494, TEX_H = 1056;
    /** Étendue du monde représentée par la carte (blocs) : 249 × 176 cases de 32 blocs. */
    private static final int WORLD_W = 7968, WORLD_H = 5632;
    private static final int INK = 0xFF111111, PIN = 0xFFE23B3B, PIN_POOR = 0xFF8A8A8A, JENNY = 0xFFFFC23A;

    private final OpenAirshipPayload data;
    private int mapX, mapY, mapW, mapH;

    public AirshipMapScreen(OpenAirshipPayload data) {
        super(Component.translatable("airship.hxh.title"));
        this.data = data;
    }

    @Override
    protected void init() {
        int maxW = width - 40, maxH = height - 50;
        mapW = Math.min(maxW, maxH * TEX_W / TEX_H);
        mapH = mapW * TEX_H / TEX_W;
        mapX = (width - mapW) / 2;
        mapY = 28 + (maxH - mapH) / 2;
    }

    private int px(OpenAirshipPayload.Dest d) {
        return mapX + (int) ((long) d.x() * mapW / WORLD_W);
    }

    private int pz(OpenAirshipPayload.Dest d) {
        return mapY + (int) ((long) d.z() * mapH / WORLD_H);
    }

    private OpenAirshipPayload.Dest pinAt(double mx, double my) {
        OpenAirshipPayload.Dest best = null;
        for (OpenAirshipPayload.Dest d : data.destinations())
            if (Math.abs(mx - px(d)) <= 5 && Math.abs(my - pz(d)) <= 5) best = d;
        return best;
    }

    /** Pas de flou de fond (le flou de Minecraft rendait la carte floue) : un simple voile sombre. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        g.fill(mapX - 3, mapY - 3, mapX + mapW + 3, mapY + mapH + 3, INK);
        g.blit(MAP, mapX, mapY, mapW, mapH, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("airship.hxh.jenny", data.jenny()), mapX, 8, JENNY);
        OpenAirshipPayload.Dest hover = pinAt(mouseX, mouseY);
        for (OpenAirshipPayload.Dest d : data.destinations()) {
            int x = px(d), y = pz(d);
            int color = data.jenny() >= d.price() ? PIN : PIN_POOR;
            g.fill(x - 4, y - 4, x + 4, y + 4, INK);
            g.fill(x - 3, y - 3, x + 3, y + 3, color);
        }
        if (hover != null) {
            Component name = Component.translatable("airship.hxh.dest." + hover.id().substring(hover.id().indexOf(':') + 1));
            boolean poor = data.jenny() < hover.price();
            g.renderTooltip(font, java.util.List.of(
                    name.copy().withStyle(net.minecraft.ChatFormatting.BOLD),
                    Component.translatable("airship.hxh.price", hover.price()).withStyle(poor ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GOLD),
                    poor ? Component.translatable("airship.hxh.too_poor", hover.price() - data.jenny()) : Component.translatable("airship.hxh.click")),
                    java.util.Optional.empty(), mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        OpenAirshipPayload.Dest d = button == 0 ? pinAt(mx, my) : null;
        if (d != null && data.jenny() >= d.price()) {
            Component name = Component.translatable("airship.hxh.dest." + d.id().substring(d.id().indexOf(':') + 1));
            Minecraft.getInstance().setScreen(new ConfirmScreen(yes -> {
                if (yes) PacketDistributor.sendToServer(new AirshipTravelPayload(d.id()));
                Minecraft.getInstance().setScreen(yes ? null : this);
            }, Component.translatable("airship.hxh.confirm_title"), Component.translatable("airship.hxh.confirm", name, d.price())));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
