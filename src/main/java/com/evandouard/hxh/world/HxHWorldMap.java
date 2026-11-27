package com.evandouard.hxh.world;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * La carte du monde de Hunter x Hunter, en grille : 1 case = 640 blocs (32 × SCALE), origine (0, 0) en haut à gauche, x vers l'est, z vers le sud.
 * Lue depuis {@code data/hxh/world/cells.txt} (modifiable à la main) : {@code ~} mer, {@code .} terre, et une lettre par région.
 * Le relief s'en déduit : plus on s'éloigne de la côte, plus le terrain monte (terre) ou descend (mer), avec du bruit par-dessus.
 * Tout ce qui est hors de la carte est de l'océan profond.
 */
public final class HxHWorldMap {
    /** Facteur d'échelle du monde (27 nov) : les distances sont 20 fois celles de la première version, pour imposer un moyen de transport. */
    public static final int SCALE = 20;
    /** Taille d'une case de la grille, en blocs. */
    public static final int CELL = 32 * SCALE;
    public static final int SEA_LEVEL = 63;

    private static volatile HxHWorldMap instance;

    private final int width, height;
    private final char[][] cells;
    private final float[][] target;
    /** Zones aplanies (data/hxh/world/flats.txt : « x z rayon y ») pour poser de grands lieux sur un sol plat. */
    private final java.util.List<double[]> flats = new ArrayList<>();
    private final PerlinNoise hills = PerlinNoise.create(new XoroshiroRandomSource(1337L), -5, 1.0, 1.0, 0.5, 0.5);
    /** Relief à grande échelle (collines et massifs de plusieurs centaines de blocs) et déformation des côtes : les cases font 640 blocs. */
    private final PerlinNoise broad = PerlinNoise.create(new XoroshiroRandomSource(4242L), -9, 1.0, 1.0, 0.5, 0.5);
    private final PerlinNoise warpX = PerlinNoise.create(new XoroshiroRandomSource(77L), -8, 1.0, 1.0);
    private final PerlinNoise warpZ = PerlinNoise.create(new XoroshiroRandomSource(78L), -8, 1.0, 1.0);
    private static final double WARP = 110;

    public static HxHWorldMap get() {
        HxHWorldMap m = instance;
        if (m == null) {
            synchronized (HxHWorldMap.class) {
                if (instance == null) instance = load();
                m = instance;
            }
        }
        return m;
    }

    private static HxHWorldMap load() {
        List<String> rows = new ArrayList<>();
        try (InputStream in = HxHWorldMap.class.getResourceAsStream("/data/hxh/world/cells.txt")) {
            if (in == null) throw new IllegalStateException("data/hxh/world/cells.txt introuvable");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) if (!line.isBlank()) rows.add(line.strip());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        HxHWorldMap m = new HxHWorldMap(rows);
        try (InputStream in = HxHWorldMap.class.getResourceAsStream("/data/hxh/world/flats.txt")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line; (line = r.readLine()) != null; ) {
                    line = line.strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] t = line.split("\\s+");
                    m.flats.add(new double[]{Double.parseDouble(t[0]), Double.parseDouble(t[1]), Double.parseDouble(t[2]), Double.parseDouble(t[3])});
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return m;
    }

    private HxHWorldMap(List<String> rows) {
        height = rows.size();
        width = rows.stream().mapToInt(String::length).max().orElse(0);
        cells = new char[height][width];
        for (int z = 0; z < height; z++)
            for (int x = 0; x < width; x++) cells[z][x] = x < rows.get(z).length() ? rows.get(z).charAt(x) : '~';
        target = new float[height][width];
        int[][] toWater = distances(true), toLand = distances(false);
        for (int z = 0; z < height; z++)
            for (int x = 0; x < width; x++) {
                target[z][x] = isLand(cells[z][x])
                        ? 64f + Math.min(toWater[z][x], 12) * 2.0f
                        : 62f - Math.min(toLand[z][x], 8) * 4.5f;
            }
    }

    /** Distance (en cases, 8 voisins) jusqu'à la mer (pour la terre) ou jusqu'à la terre (pour la mer) ; le bord de la carte compte comme mer. */
    private int[][] distances(boolean forLand) {
        int[][] d = new int[height][width];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        for (int z = 0; z < height; z++)
            for (int x = 0; x < width; x++) {
                boolean source = forLand ? !isLand(cells[z][x]) : isLand(cells[z][x]);
                d[z][x] = source ? 0 : Integer.MAX_VALUE;
                if (source) queue.add(new int[]{x, z});
                else if (forLand && (x == 0 || z == 0 || x == width - 1 || z == height - 1)) d[z][x] = 1;
            }
        if (forLand) for (int z = 0; z < height; z++) for (int x = 0; x < width; x++) if (d[z][x] == 1) queue.add(new int[]{x, z});
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = c[0] + dx, nz = c[1] + dz;
                    if (nx < 0 || nz < 0 || nx >= width || nz >= height) continue;
                    if (d[nz][nx] > d[c[1]][c[0]] + 1) {
                        d[nz][nx] = d[c[1]][c[0]] + 1;
                        queue.add(new int[]{nx, nz});
                    }
                }
        }
        return d;
    }

    public static boolean isLand(char c) {
        return c != '~';
    }

    public int widthBlocks() {
        return width * CELL;
    }

    public int heightBlocks() {
        return height * CELL;
    }

    /** Lettre de la case qui contient ce bloc ({@code ~} hors de la carte). */
    public char cellAt(int blockX, int blockZ) {
        int cx = Math.floorDiv(blockX, CELL), cz = Math.floorDiv(blockZ, CELL);
        if (cx < 0 || cz < 0 || cx >= width || cz >= height) return '~';
        return cells[cz][cx];
    }

    private float targetAt(int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= width || cz >= height) return 20f;
        return target[cz][cx];
    }

    /** Hauteur du sol (ou du fond marin) en un point : interpolation entre les cases, plus du relief sur la terre. */
    public double height(double x, double z) {
        double ox = x, oz = z;
        x = ox + warpX.getValue(ox, 0, oz) * WARP;
        z = oz + warpZ.getValue(ox, 0, oz) * WARP;
        double fx = x / CELL - 0.5, fz = z / CELL - 0.5;
        int ix = Mth.floor(fx), iz = Mth.floor(fz);
        double tx = smooth(fx - ix), tz = smooth(fz - iz);
        double base = Mth.lerp(tz, Mth.lerp(tx, targetAt(ix, iz), targetAt(ix + 1, iz)), Mth.lerp(tx, targetAt(ix, iz + 1), targetAt(ix + 1, iz + 1)));
        double n = hills.getValue(x, 0, z);
        if (base > SEA_LEVEL) {
            // à 640 blocs par case la pente venue de la grille est quasi plate : le relief vient surtout du bruit
            double inland = Mth.clamp((base - SEA_LEVEL - 1.5) / 4.0, 0, 1);
            double mass = Mth.clamp((base - SEA_LEVEL) / 24.0, 0, 1);
            base += n * inland * (2.0 + (base - SEA_LEVEL) * 0.9);
            base += broad.getValue(x, 0, z) * 18.0 * Math.sqrt(mass) + mass * 6.0;
        } else {
            base += n * 2.5;
        }
        if (base >= SEA_LEVEL - 1) {
            for (double[] f : flats) {
                double d = Math.hypot(ox - f[0], oz - f[1]);
                if (d >= f[2]) continue;
                double w = Mth.clamp((f[2] - d) / (f[2] * 0.25), 0, 1);
                base = Mth.lerp(smooth(w), base, f[3]);
            }
        }
        return Math.max(base, -20);
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }
}
