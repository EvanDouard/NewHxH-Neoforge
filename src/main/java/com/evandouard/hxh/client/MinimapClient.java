package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.airship.Airship;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Minimap du monde HxH : la carte du monde recentrée sur le joueur, zoomable (touches − et +), affichée dans un coin
 * (haut droite par défaut) uniquement dans le monde HxH. On l'active avec la touche M ; sa position, sa taille et son zoom
 * se règlent depuis le carnet (touche O, bouton « Minimap »).
 */
public final class MinimapClient {
    private static final String CATEGORY = "key.categories.hxh";
    public static final KeyMapping TOGGLE = new KeyMapping("key.hxh.minimap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping ZOOM_IN = new KeyMapping("key.hxh.minimap_zoom_in", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CATEGORY);
    public static final KeyMapping ZOOM_OUT = new KeyMapping("key.hxh.minimap_zoom_out", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, CATEGORY);

    private static final ResourceLocation MAP = HxHMod.id("textures/gui/world_map.png");
    private static final int TEX_W = 1494, TEX_H = 1056;
    /** Pixels de texture par bloc : 6 px pour une case de 32 blocs. */
    private static final double TEX_PER_BLOCK = 6.0 / 32.0;
    /** Pixels d'écran par pixel de texture, par niveau de zoom. */
    private static final double[] ZOOMS = {0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0};
    private static final int MARGIN = 6;
    private static boolean kpPlusDown, kpMinusDown;

    private MinimapClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> {
            e.register(TOGGLE);
            e.register(ZOOM_IN);
            e.register(ZOOM_OUT);
        });
        modBus.addListener(RegisterGuiLayersEvent.class,
                e -> e.registerAbove(VanillaGuiLayers.HOTBAR, HxHMod.id("minimap"), MinimapClient::renderLayer));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> tick());
    }

    public static boolean inHxhWorld(Minecraft mc) {
        return mc.level != null && mc.level.dimension().equals(Airship.WORLD);
    }

    public static boolean enabled() {
        return HxHClientConfig.MINIMAP_ENABLED.get();
    }

    public static void setEnabled(boolean on) {
        HxHClientConfig.MINIMAP_ENABLED.set(on);
        HxHClientConfig.SPEC.save();
    }

    public static void setZoom(int level) {
        HxHClientConfig.MINIMAP_ZOOM.set(Math.max(0, Math.min(ZOOMS.length - 1, level)));
        HxHClientConfig.SPEC.save();
    }

    public static void setSize(int size) {
        HxHClientConfig.MINIMAP_SIZE.set(Math.max(64, Math.min(200, size)));
        HxHClientConfig.SPEC.save();
    }

    public static void setPosition(double fx, double fy) {
        HxHClientConfig.MINIMAP_X.set(Math.max(0, Math.min(1, fx)));
        HxHClientConfig.MINIMAP_Y.set(Math.max(0, Math.min(1, fy)));
        HxHClientConfig.SPEC.save();
    }

    /** Coin haut-gauche et côté de la minimap pour un écran donné : {x, y, côté}. */
    public static int[] rect(int screenW, int screenH) {
        int size = Math.min(HxHClientConfig.MINIMAP_SIZE.get(), Math.min(screenW, screenH) - 2 * MARGIN);
        int x = MARGIN + (int) Math.round(HxHClientConfig.MINIMAP_X.get() * (screenW - size - 2 * MARGIN));
        int y = MARGIN + (int) Math.round(HxHClientConfig.MINIMAP_Y.get() * (screenH - size - 2 * MARGIN));
        return new int[]{x, y, size};
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        // Les touches ne comptent que dans le jeu, jamais pendant un menu ou un chat
        if (mc.screen != null) {
            while (TOGGLE.consumeClick()) { }
            while (ZOOM_IN.consumeClick()) { }
            while (ZOOM_OUT.consumeClick()) { }
            return;
        }
        if (!inHxhWorld(mc)) {
            while (TOGGLE.consumeClick()) { }
            while (ZOOM_IN.consumeClick()) { }
            while (ZOOM_OUT.consumeClick()) { }
            return;
        }
        while (TOGGLE.consumeClick()) setEnabled(!enabled());
        long window = mc.getWindow().getWindow();
        boolean plus = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_ADD) == GLFW.GLFW_PRESS;
        boolean minus = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_SUBTRACT) == GLFW.GLFW_PRESS;
        boolean zoomIn = plus && !kpPlusDown, zoomOut = minus && !kpMinusDown;
        kpPlusDown = plus;
        kpMinusDown = minus;
        while (ZOOM_IN.consumeClick()) zoomIn = true;
        while (ZOOM_OUT.consumeClick()) zoomOut = true;
        if (enabled()) {
            if (zoomIn) setZoom(HxHClientConfig.MINIMAP_ZOOM.get() + 1);
            if (zoomOut) setZoom(HxHClientConfig.MINIMAP_ZOOM.get() - 1);
        }
    }

    private static void renderLayer(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !enabled() || !inHxhWorld(mc)) return;
        int[] r = rect(g.guiWidth(), g.guiHeight());
        draw(g, mc, r[0], r[1], r[2]);
    }

    /** Dessine la minimap (cadre, carte centrée sur le joueur, flèche, nord, région et coordonnées). */
    public static void draw(GuiGraphics g, Minecraft mc, int x, int y, int size) {
        double zoom = ZOOMS[HxHClientConfig.MINIMAP_ZOOM.get()];
        double bx = mc.player.getX(), bz = mc.player.getZ();
        g.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xFF0E1420);
        g.fill(x, y, x + size, y + size, 0xFF264E8C);
        g.enableScissor(x, y, x + size, y + size);
        double k = TEX_PER_BLOCK * zoom;
        int dx = (int) Math.round(x + size / 2.0 - bx * k), dy = (int) Math.round(y + size / 2.0 - bz * k);
        g.blit(MAP, dx, dy, (int) Math.round(TEX_W * zoom), (int) Math.round(TEX_H * zoom), 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        g.disableScissor();
        // Flèche du joueur, orientée comme son regard (haut de la carte = nord)
        g.pose().pushPose();
        g.pose().translate(x + size / 2.0f, y + size / 2.0f, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(mc.player.getYRot() + 180f));
        g.fill(-3, -1, 3, 3, 0xFF0E1420);
        g.fill(-2, 0, 2, 2, 0xFFFFFFFF);
        g.fill(-1, -3, 1, 0, 0xFF0E1420);
        g.fill(0, -2, 1, 0, 0xFFE03C3C);
        g.pose().popPose();
        g.drawString(mc.font, "N", x + size / 2 - 2, y + 3, 0xFFFFFFFF, true);
        Component region = AirshipMapScreen.regionNameAt(bx, bz);
        String coords = (int) bx + ", " + (int) bz;
        int ty = y + size + 4;
        if (region != null) {
            g.drawString(mc.font, region, x + (size - mc.font.width(region)) / 2, ty, 0xFFFFFFFF, true);
            ty += 10;
        }
        g.drawString(mc.font, coords, x + (size - mc.font.width(coords)) / 2, ty, 0xFFD8D8D8, true);
    }
}
