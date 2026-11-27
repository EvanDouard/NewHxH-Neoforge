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
    private static final int INK = 0xFF0E1420, PIN_OK = 0xFFFFE9A8, PIN_POOR = 0xFF7C8594, JENNY = 0xFFFFC23A;
    /** Couleurs des régions (identiques à celles de la texture) pour l'en-tête de l'infobulle. */
    private static final String REGIONS = "mksKobprhwen";
    private static final int[] REGION_RGB = {0x84B2CE, 0xA082B8, 0xC4685C, 0xDEBE60, 0x6EAA6C, 0xDE9454, 0x58B0A8, 0x7CC47A, 0xE08AC8, 0xC8D860, 0x9A6AD0, 0xE89A50};

    private static char[][] grid;

    private final OpenAirshipPayload data;
    private int mapX, mapY, mapW, mapH;
    /** Position simulée du curseur (scénario de CI), sinon -1. */
    private int fakeX = -1, fakeY = -1;

    public AirshipMapScreen(OpenAirshipPayload data) {
        super(Component.translatable("airship.hxh.title"));
        this.data = data;
    }

    /** Grille des régions (assets/hxh/world/regions.txt), lue une fois. */
    private static char[][] grid() {
        if (grid == null) {
            java.util.List<String> rows = new java.util.ArrayList<>();
            try (var in = Minecraft.getInstance().getResourceManager().open(HxHMod.id("world/regions.txt"));
                 var r = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                for (String line; (line = r.readLine()) != null; ) if (!line.isBlank()) rows.add(line.strip());
            } catch (java.io.IOException e) {
                HxHMod.LOGGER.warn("[HxH] Grille des régions introuvable : {}", e.toString());
            }
            char[][] g = new char[rows.size()][];
            for (int i = 0; i < g.length; i++) g[i] = rows.get(i).toCharArray();
            grid = g;
        }
        return grid;
    }

    private static char cell(int cx, int cz) {
        char[][] g = grid();
        if (cz < 0 || cz >= g.length || cx < 0 || cx >= g[cz].length) return '~';
        return g[cz][cx];
    }

    /** Lettre de la région sous un point de l'écran, ou '~' / '.' hors région. */
    private char regionAtScreen(double mx, double my) {
        if (mx < mapX || my < mapY || mx >= mapX + mapW || my >= mapY + mapH) return '~';
        return cell((int) ((mx - mapX) * WORLD_W / mapW / 32), (int) ((my - mapY) * WORLD_H / mapH / 32));
    }

    private char regionOf(OpenAirshipPayload.Dest d) {
        return cell(d.x() / 32, d.z() / 32);
    }

    /** Nom de la région à une position du monde (blocs), ou null hors région. Sert à la minimap. */
    public static Component regionNameAt(double bx, double bz) {
        char c = cell((int) Math.floor(bx / 32), (int) Math.floor(bz / 32));
        return REGIONS.indexOf(c) >= 0 ? regionName(c) : null;
    }

    private static Component regionName(char c) {
        return Component.translatable("airship.hxh.region." + c);
    }

    private static int regionColor(char c) {
        int i = REGIONS.indexOf(c);
        return i < 0 ? 0xC4B88C : REGION_RGB[i];
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
            if (Math.abs(mx - px(d)) <= 4 && Math.abs(my - pz(d)) <= 4) best = d;
        return best;
    }

    /** Scénario de CI : place le curseur sur un lieu (identifiant ou nom court) ou sur une région (« region:K »). */
    public boolean hover(String what) {
        if (what.startsWith("region:") && what.length() == 8) {
            char c = what.charAt(7);
            char[][] g = grid();
            long sx = 0, sz = 0, n = 0;
            for (int z = 0; z < g.length; z++) for (int x = 0; x < g[z].length; x++) if (g[z][x] == c) { sx += x; sz += z; n++; }
            if (n == 0) return false;
            fakeX = mapX + (int) (sx / n * 32 * mapW / WORLD_W);
            fakeY = mapY + (int) (sz / n * 32 * mapH / WORLD_H);
            return true;
        }
        for (OpenAirshipPayload.Dest d : data.destinations()) {
            if (d.id().equals(what) || d.id().endsWith(":" + what)) {
                fakeX = px(d);
                fakeY = pz(d);
                return true;
            }
        }
        return false;
    }

    /** Pas de flou ni de voile ici : le voile est posé une seule fois dans {@link #render}. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
    }

    /** Bordure de la région : un trait de 2 px sur chaque côté de case dont le voisin est d'une autre région. */
    private void drawRegionBorder(GuiGraphics g, char region) {
        char[][] grid = grid();
        for (int z = 0; z < grid.length; z++) {
            for (int x = 0; x < grid[z].length; x++) {
                if (grid[z][x] != region) continue;
                int x0 = mapX + x * 32 * mapW / WORLD_W, x1 = mapX + (x + 1) * 32 * mapW / WORLD_W;
                int y0 = mapY + z * 32 * mapH / WORLD_H, y1 = mapY + (z + 1) * 32 * mapH / WORLD_H;
                g.fill(x0, y0, x1, y1, 0x30FFFFFF);
                if (cell(x - 1, z) != region) g.fill(x0 - 1, y0, x0 + 1, y1, 0xFFFFFFFF);
                if (cell(x + 1, z) != region) g.fill(x1 - 1, y0, x1 + 1, y1, 0xFFFFFFFF);
                if (cell(x, z - 1) != region) g.fill(x0, y0 - 1, x1, y0 + 1, 0xFFFFFFFF);
                if (cell(x, z + 1) != region) g.fill(x0, y1 - 1, x1, y1 + 1, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (fakeX >= 0) { mouseX = fakeX; mouseY = fakeY; }
        g.fill(0, 0, width, height, 0x80000000);
        g.fill(mapX - 3, mapY - 3, mapX + mapW + 3, mapY + mapH + 3, INK);
        g.blit(MAP, mapX, mapY, mapW, mapH, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        Component jenny = Component.translatable("airship.hxh.jenny", data.jenny());
        g.drawString(font, jenny, mapX + mapW - font.width(jenny), 8, JENNY);

        OpenAirshipPayload.Dest hover = pinAt(mouseX, mouseY);
        char region = hover != null ? regionOf(hover) : regionAtScreen(mouseX, mouseY);
        if (REGIONS.indexOf(region) >= 0) drawRegionBorder(g, region);

        for (OpenAirshipPayload.Dest d : data.destinations()) {
            int x = px(d), y = pz(d);
            int r = d == hover ? 3 : 2;
            g.fill(x - r - 1, y - r - 1, x + r + 1, y + r + 1, 0xE0000000);
            g.fill(x - r, y - r, x + r, y + r, data.jenny() >= d.price() ? PIN_OK : PIN_POOR);
        }

        if (hover != null) {
            Component name = Component.translatable("airship.hxh.dest." + hover.id().substring(hover.id().indexOf(':') + 1));
            boolean poor = data.jenny() < hover.price();
            java.util.List<Component> lines = new java.util.ArrayList<>();
            if (REGIONS.indexOf(region) >= 0)
                lines.add(regionName(region).copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(regionColor(region)).withUnderlined(true)));
            lines.add(name.copy().withStyle(net.minecraft.ChatFormatting.BOLD));
            lines.add(Component.translatable("airship.hxh.price", hover.price()).withStyle(poor ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GOLD));
            lines.add(poor ? Component.translatable("airship.hxh.too_poor", hover.price() - data.jenny()) : Component.translatable("airship.hxh.click"));
            g.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
        } else if (REGIONS.indexOf(region) >= 0) {
            g.renderTooltip(font, java.util.List.of(regionName(region).copy().withStyle(
                    net.minecraft.network.chat.Style.EMPTY.withColor(regionColor(region)).withBold(true))),
                    java.util.Optional.empty(), mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        OpenAirshipPayload.Dest d = button == 0 ? pinAt(fakeX >= 0 ? fakeX : mx, fakeX >= 0 ? fakeY : my) : null;
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
