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
    /** Carte pour qui sait que le Continent Noir existe : même style, sur une toile plus grande (Nouveau Monde, lac Mobius, Continent Noir). */
    private static final ResourceLocation MAP_DARK = HxHMod.id("textures/gui/world_map_dark.png");
    /** Taille de la zone du monde connu dans les deux textures ; la grande toile la pose en (DARK_DX, DARK_DY). */
    private static final int WORLD_TEX_W = 1494, WORLD_TEX_H = 1056;
    private static final int DARK_TEX_W = 2400, DARK_TEX_H = 1424, DARK_DX = 662, DARK_DY = 332, DARK_WORLD_W = 1076, DARK_WORLD_H = 760;
    /** Échelle du monde connu sur la grande toile (1076 / 1494). */
    private static final double DARK_WS = 0.72;
    /** Étendue du monde représentée par la carte (blocs) : 249 × 176 cases de 32 blocs. */
    private static final int WORLD_W = 7968, WORLD_H = 5632;
    private static final int INK = 0xFF0E1420, PIN_OK = 0xFFFFE9A8, PIN_POOR = 0xFF7C8594, JENNY = 0xFFFFC23A;
    /** Couleurs des régions (identiques à celles de la texture) pour l'en-tête de l'infobulle. */
    private static final String REGIONS = "mksKobprhwen";
    private static final int[] REGION_RGB = {0x84B2CE, 0xA082B8, 0xC4685C, 0xDEBE60, 0x6EAA6C, 0xDE9454, 0x58B0A8, 0x7CC47A, 0xE08AC8, 0xC8D860, 0x9A6AD0, 0xE89A50};

    private static char[][] grid;

    private final OpenAirshipPayload data;
    private final boolean dark;
    private final int texW, texH, offX, offY, worldTexW, worldTexH;
    /** Toile entière à l'écran, puis zone du monde connu (blocs ↔ pixels) : {@code mapW/mapH} = monde. */
    private int mapX, mapY, mapW, mapH, canvasX, canvasY, canvasW, canvasH;
    /** Position simulée du curseur (scénario de CI), sinon -1. */
    private int fakeX = -1, fakeY = -1;

    public AirshipMapScreen(OpenAirshipPayload data) {
        super(Component.translatable("airship.hxh.title"));
        this.data = data;
        this.dark = data.darkContinentKnown();
        this.texW = dark ? DARK_TEX_W : WORLD_TEX_W;
        this.texH = dark ? DARK_TEX_H : WORLD_TEX_H;
        this.offX = dark ? DARK_DX : 0;
        this.offY = dark ? DARK_DY : 0;
        this.worldTexW = dark ? DARK_WORLD_W : WORLD_TEX_W;
        this.worldTexH = dark ? DARK_WORLD_H : WORLD_TEX_H;
    }

    public boolean isDarkMap() {
        return dark;
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
        canvasW = Math.min(maxW, maxH * texW / texH);
        canvasH = canvasW * texH / texW;
        canvasX = (width - canvasW) / 2;
        canvasY = 28 + (maxH - canvasH) / 2;
        mapW = canvasW * worldTexW / texW;
        mapH = canvasH * worldTexH / texH;
        mapX = canvasX + canvasW * offX / texW;
        mapY = canvasY + canvasH * offY / texH;
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
        if (dark && what.startsWith("dark:") && what.length() == 6) {
            for (int[] p : DARK_POINTS) if (p[0] == what.charAt(5)) { fakeX = dpx(p); fakeY = dpy(p); return true; }
            return false;
        }
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

    /** Texte centré (sur plusieurs lignes si {@code split}), agrandi, avec ombre : {@code cx/cy} en pixels de la toile. */
    private void label(GuiGraphics g, Component text, double cx, double cy, float scale, int color, boolean split) {
        String[] lines = split ? text.getString().split(" ") : new String[]{text.getString()};
        g.pose().pushPose();
        g.pose().translate(canvasX + cx * canvasW / texW, canvasY + cy * canvasH / texH, 0);
        g.pose().scale(scale, scale, 1);
        int top = -lines.length * font.lineHeight / 2;
        for (int i = 0; i < lines.length; i++)
            g.drawString(font, lines[i], -font.width(lines[i]) / 2, top + i * font.lineHeight, color, true);
        g.pose().popPose();
    }

    /** Points sur les côtes du Continent Noir : région (pour le nom) et position sur la toile. Pas encore accessibles. */
    private static final int[][] DARK_POINTS = {{'s', 1330, 239}, {'b', 1613, 148}, {'k', 1313, 1220}, {'m', 1802, 1354}, {'o', 2117, 1247}};

    private int dpx(int[] p) {
        return canvasX + p[1] * canvasW / texW;
    }

    private int dpy(int[] p) {
        return canvasY + p[2] * canvasH / texH;
    }

    private int[] darkPointAt(double mx, double my) {
        if (!dark) return null;
        for (int[] p : DARK_POINTS) if (Math.abs(mx - dpx(p)) <= 4 && Math.abs(my - dpy(p)) <= 4) return p;
        return null;
    }

    /**
     * Carte du Continent Noir : noms des régions du monde connu, « Monde connu », lac Mobius (deux lobes), Nouveau Continent,
     * « Continent Noir » en haut et cinq croix (Saherta, Begerossé, Kukan'yu, Mimbo, Ochima) sur ses côtes.
     */
    private void drawDarkLabels(GuiGraphics g) {
        float s = Math.max(0.55f, canvasW / 1100f);
        char[][] grid = grid();
        for (int i = 0; i < 7; i++) {
            char c = REGIONS.charAt(i);
            long sx = 0, sz = 0, n = 0;
            for (int z = 0; z < grid.length; z++) for (int x = 0; x < grid[z].length; x++) if (grid[z][x] == c) { sx += x; sz += z; n++; }
            if (n == 0) continue;
            label(g, regionName(c), offX + (sx / (double) n + 0.5) * 6 * DARK_WS, offY + (sz / (double) n + 0.5) * 6 * DARK_WS, s * 0.55f, 0xFFFFFFFF, true);
        }
        label(g, Component.translatable("airship.hxh.dark_continent"), 1200, 78, s * 2.0f, 0xFFFFF4D8, false);
        label(g, Component.translatable("airship.hxh.known_world"), 1200, 700, s * 1.7f, 0xFFFFF4D8, false);
        label(g, Component.translatable("airship.hxh.lake_mobius"), 370, 730, s * 1.7f, 0xFFE8F4FF, false);
        label(g, Component.translatable("airship.hxh.lake_mobius"), 2030, 760, s * 1.7f, 0xFFE8F4FF, false);
        label(g, Component.translatable("airship.hxh.new_continent"), 1894, 534, s * 0.9f, 0xFFFFF4D8, true);
        // petits carrés comme les autres repères, grisés : pas encore accessibles en dirigeable
        for (int[] p : DARK_POINTS) {
            int x = dpx(p), y = dpy(p);
            g.fill(x - 3, y - 3, x + 3, y + 3, 0xE0000000);
            g.fill(x - 2, y - 2, x + 2, y + 2, PIN_POOR);
            label(g, regionName((char) p[0]), p[1], p[2] + (p[2] < texH / 2 ? -26 : 26), s * 0.9f, 0xFF3A2410, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (fakeX >= 0) { mouseX = fakeX; mouseY = fakeY; }
        g.fill(0, 0, width, height, 0x80000000);
        g.fill(canvasX - 3, canvasY - 3, canvasX + canvasW + 3, canvasY + canvasH + 3, INK);
        g.blit(dark ? MAP_DARK : MAP, canvasX, canvasY, canvasW, canvasH, 0, 0, texW, texH, texW, texH);
        if (dark) drawDarkLabels(g);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        Component jenny = Component.translatable("airship.hxh.jenny", data.jenny());
        g.drawString(font, jenny, canvasX + canvasW - font.width(jenny), 8, JENNY);

        OpenAirshipPayload.Dest hover = pinAt(mouseX, mouseY);
        char region = hover != null ? regionOf(hover) : regionAtScreen(mouseX, mouseY);
        if (REGIONS.indexOf(region) >= 0) drawRegionBorder(g, region);

        for (OpenAirshipPayload.Dest d : data.destinations()) {
            int x = px(d), y = pz(d);
            int r = d == hover ? 3 : 2;
            g.fill(x - r - 1, y - r - 1, x + r + 1, y + r + 1, 0xE0000000);
            g.fill(x - r, y - r, x + r, y + r, data.jenny() >= d.price() ? PIN_OK : PIN_POOR);
        }

        int[] darkPoint = hover == null ? darkPointAt(mouseX, mouseY) : null;
        if (darkPoint != null) {
            g.renderTooltip(font, java.util.List.of(regionName((char) darkPoint[0]).copy().withStyle(net.minecraft.ChatFormatting.BOLD),
                    Component.translatable("airship.hxh.dark_point").withStyle(net.minecraft.ChatFormatting.GRAY)),
                    java.util.Optional.empty(), mouseX, mouseY);
        } else if (hover != null) {
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
