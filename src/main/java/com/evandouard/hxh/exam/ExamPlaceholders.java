package com.evandouard.hxh.exam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.evandouard.hxh.exam.ExamWorld.set;

/**
 * Lieux provisoires de l'Examen, en attendant les structures d'Evan (mêmes PNJ, même parcours).
 * Sol à {@code FLOOR_Y - 1}, on marche à {@code FLOOR_Y}.
 */
final class ExamPlaceholders {
    private ExamPlaceholders() {}

    static ExamWorld.Site build(ServerLevel level, ExamSite site, BlockPos c, boolean place) {
        return switch (site) {
            case ZABAN -> zaban(level, c, place);
            case HALL -> hall(level, c, place);
            case BISKA -> biska(level, c, place);
            case HOTEL -> hotel(level, c, place);
            case BOAT -> boat(level, c, place);
            case DOLLE -> dolle(level, c, place);
            case NUMELE -> numele(level, c, place);
            case MOUNT_SPLIT -> mountSplit(level, c, place);
            case ZEVIL -> zevil(level, c, place);
            case AIRSHIP -> airship(level, c, place);
        };
    }

    /** Boîte creuse : sol, murs, plafond ; l'intérieur vidé. */
    private static void room(ServerLevel level, BlockPos c, int hx, int hz, int h, Block floor, Block wall, Block ceiling) {
        for (int x = -hx; x <= hx; x++) {
            for (int z = -hz; z <= hz; z++) {
                set(level, c.offset(x, -1, z), floor);
                set(level, c.offset(x, h, z), ceiling);
                boolean edge = Math.abs(x) == hx || Math.abs(z) == hz;
                for (int y = 0; y < h; y++) set(level, c.offset(x, y, z), edge ? wall.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
    }

    // ------------------------------------------------------------------ Zaban

    private static ExamWorld.Site zaban(ServerLevel level, BlockPos c, boolean place) {
        if (place) {
            room(level, c, 6, 5, 4, Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.DARK_OAK_PLANKS);
            // comptoir de la gargote, tabourets, lanternes
            for (int x = -4; x <= 4; x++) set(level, c.offset(x, 0, -2), Blocks.STRIPPED_OAK_LOG);
            for (int x = -4; x <= 4; x += 2) set(level, c.offset(x, 0, 0), Blocks.OAK_FENCE);
            set(level, c.offset(-5, 0, -4), Blocks.SMOKER);
            set(level, c.offset(5, 0, -4), Blocks.BARREL);
            for (int x = -4; x <= 4; x += 4) set(level, c.offset(x, 3, 0), Blocks.LANTERN);
            set(level, c.offset(0, 3, -3), Blocks.LANTERN);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("cook", c.offset(0, 0, -3));
        return new ExamWorld.Site(c.offset(0, 0, 3), 180f, npcs, List.of(), List.of());
    }

    // ------------------------------------------------------------------ Hall + tunnel

    /** Longueur du tunnel droit, puis de l'escalier (une marche tous les 2 blocs). */
    private static final int TUNNEL = 90, STAIRS = 24;

    private static ExamWorld.Site hall(ServerLevel level, BlockPos c, boolean place) {
        int hx = 12, hz = 12;
        if (place) {
            room(level, c, hx, hz, 8, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS);
            for (int x = -9; x <= 9; x += 6) for (int z = -9; z <= 9; z += 6) set(level, c.offset(x, 8, z), Blocks.GLOWSTONE);
            // comptoir de Beans
            for (int x = -9; x <= -5; x++) set(level, c.offset(x, 0, -6), Blocks.POLISHED_DEEPSLATE);
            // ouverture vers le tunnel, au nord
            for (int x = -1; x <= 1; x++) for (int y = 0; y < 3; y++) set(level, c.offset(x, y, -hz), Blocks.AIR);
            // tunnel : 3 de large, 4 de haut, éclairé
            for (int i = 1; i <= TUNNEL; i++) tunnelSlice(level, c.offset(0, 0, -hz - i), 0, i % 6 == 0);
            // escalier qui monte (une marche tous les 2 blocs)
            int y = 0;
            for (int i = 1; i <= STAIRS; i++) {
                BlockPos p = c.offset(0, 0, -hz - TUNNEL - i);
                if (i % 2 == 0) y++;
                tunnelSlice(level, p, y, i % 6 == 0);
                if (i % 2 == 0) {
                    BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
                    for (int x = -1; x <= 1; x++) set(level, p.offset(x, y - 1, 0), stair);
                }
            }
            // palier de sortie, à la lumière
            BlockPos exit = c.offset(0, y, -hz - TUNNEL - STAIRS - 3);
            for (int x = -3; x <= 3; x++) for (int z = -2; z <= 2; z++) {
                set(level, exit.offset(x, -1, z), Blocks.MOSSY_STONE_BRICKS);
                for (int h = 0; h < 4; h++) set(level, exit.offset(x, h, z), Blocks.AIR);
            }
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("beans", c.offset(-7, 0, -8));
        npcs.put("satotz", c.offset(0, 0, -hz + 2));
        List<BlockPos> route = new ArrayList<>();
        for (int i = 8; i <= TUNNEL; i += 10) route.add(c.offset(0, 0, -hz - i));
        route.add(c.offset(0, STAIRS / 2, -hz - TUNNEL - STAIRS - 3));
        return new ExamWorld.Site(c.offset(0, 0, 8), 180f, npcs, route, List.of());
    }

    private static void tunnelSlice(ServerLevel level, BlockPos p, int y, boolean light) {
        for (int x = -2; x <= 2; x++) {
            set(level, p.offset(x, y - 1, 0), x == -2 || x == 2 ? Blocks.STONE_BRICKS : Blocks.STONE);
            set(level, p.offset(x, y + 4, 0), light && x == 0 ? Blocks.GLOWSTONE : Blocks.STONE_BRICKS);
            for (int h = 0; h < 4; h++) {
                set(level, p.offset(x, y + h, 0), x == -2 || x == 2 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
    }

    // ------------------------------------------------------------------ Biska

    private static ExamWorld.Site biska(ServerLevel level, BlockPos c, boolean place) {
        int r = 22;
        if (place) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > r * r) continue;
                    set(level, c.offset(x, -1, z), Blocks.GRASS_BLOCK);
                    for (int d = 2; d <= 4; d++) set(level, c.offset(x, -d, z), Blocks.DIRT);
                }
            }
            // arbres tout autour
            for (int k = 0; k < 16; k++) {
                double a = k / 16.0 * Math.PI * 2;
                tree(level, c.offset((int) Math.round(Math.cos(a) * (r - 3)), 0, (int) Math.round(Math.sin(a) * (r - 3))));
            }
            // cuisine en plein air de Menchi et Buhara
            for (int x = -4; x <= 4; x++) set(level, c.offset(x, 0, -8), Blocks.SPRUCE_SLAB);
            set(level, c.offset(-2, 0, -9), Blocks.CAMPFIRE);
            set(level, c.offset(2, 0, -9), Blocks.SMOKER);
            set(level, c.offset(0, 0, -9), Blocks.CAULDRON);
            // étang : les poissons du sushi de Menchi
            for (int x = 8; x <= 14; x++) for (int z = -10; z <= -5; z++) {
                set(level, c.offset(x, -1, z), Blocks.WATER);
                set(level, c.offset(x, -2, z), Blocks.WATER);
                set(level, c.offset(x, -3, z), Blocks.CLAY);
            }
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("buhara", c.offset(-3, 0, -6));
        npcs.put("menchi", c.offset(3, 0, -6));
        npcs.put("netero", c.offset(0, 0, -12));
        List<BlockPos> prey = List.of(c.offset(-12, 0, 6), c.offset(12, 0, 6), c.offset(0, 0, 14));
        return new ExamWorld.Site(c.offset(0, 0, 4), 180f, npcs, List.of(), prey);
    }

    private static void tree(ServerLevel level, BlockPos base) {
        for (int y = 0; y < 5; y++) set(level, base.above(y), Blocks.OAK_LOG);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 3; y <= 5; y++) {
            if (Math.abs(x) + Math.abs(z) + (y - 3) > 4) continue;
            BlockPos p = base.offset(x, y, z);
            if (level.getBlockState(p).isAir()) set(level, p, Blocks.OAK_LEAVES.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        }
    }

    // ------------------------------------------------------------------ Hôtel de l'Association

    private static ExamWorld.Site hotel(ServerLevel level, BlockPos c, boolean place) {
        if (place) {
            room(level, c, 7, 6, 5, Blocks.POLISHED_DIORITE, Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ);
            for (int z = -5; z <= 5; z++) set(level, c.offset(0, 0, z), Blocks.RED_CARPET);
            for (int x = -5; x <= 5; x += 5) set(level, c.offset(x, 4, 0), Blocks.SEA_LANTERN);
            set(level, c.offset(-2, 0, -5), Blocks.POTTED_BAMBOO);
            set(level, c.offset(2, 0, -5), Blocks.POTTED_BAMBOO);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("netero", c.offset(0, 0, -4));
        return new ExamWorld.Site(c.offset(0, 0, 4), 180f, npcs, List.of(), List.of());
    }

    // ------------------------------------------------------------------ Mer (bateau, Zevil)

    /** Mer carrée de demi-côté {@code r} : 3 blocs d'eau sur du sable. */
    private static void sea(ServerLevel level, BlockPos c, int r) {
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            for (int y = -4; y <= -2; y++) set(level, c.offset(x, y, z), Blocks.WATER);
            set(level, c.offset(x, -5, z), Blocks.SAND);
        }
    }

    /** Bateau de pêche vers Dolle : pont de 7 × 21, mât et voile ; le capitaine à la poupe. */
    private static ExamWorld.Site boat(ServerLevel level, BlockPos c, boolean place) {
        if (place) {
            sea(level, c, 32);
            for (int x = -3; x <= 3; x++) for (int z = -10; z <= 10; z++) {
                boolean edge = Math.abs(x) == 3 || Math.abs(z) == 10;
                set(level, c.offset(x, -1, z), Blocks.OAK_PLANKS);
                set(level, c.offset(x, -2, z), Blocks.SPRUCE_PLANKS);
                if (edge) set(level, c.offset(x, 0, z), Blocks.SPRUCE_FENCE);
            }
            for (int y = 0; y < 9; y++) set(level, c.offset(0, y, -2), Blocks.SPRUCE_LOG);
            for (int x = -3; x <= 3; x++) for (int y = 3; y < 8; y++) set(level, c.offset(x, y, -1), Blocks.WHITE_WOOL);
            set(level, c.offset(0, 0, 8), Blocks.BARREL);
            set(level, c.offset(2, 0, 8), Blocks.LANTERN);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("captain", c.offset(0, 0, 7));
        return new ExamWorld.Site(c.offset(0, 0, 3), 0f, npcs, List.of(), List.of());
    }

    /** Île de Zevil : plage, forêt au centre, ponton d'arrivée ; des candidats rôdent avec leur plaque. */
    private static ExamWorld.Site zevil(ServerLevel level, BlockPos c, boolean place) {
        int r = 30;
        if (place) {
            sea(level, c, r + 12);
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                int d2 = x * x + z * z;
                if (d2 > r * r) continue;
                for (int y = -4; y <= -2; y++) set(level, c.offset(x, y, z), d2 > (r - 5) * (r - 5) ? Blocks.SAND : Blocks.DIRT);
                set(level, c.offset(x, -1, z), d2 > (r - 5) * (r - 5) ? Blocks.SAND : Blocks.GRASS_BLOCK);
            }
            for (int k = 0; k < 14; k++) {
                double a = k * 2.3;
                double rr = 6 + (k % 4) * 4;
                tree(level, c.offset((int) Math.round(Math.cos(a) * rr), 0, (int) Math.round(Math.sin(a) * rr)));
            }
            // ponton
            for (int z = r - 2; z <= r + 8; z++) for (int x = -1; x <= 1; x++) set(level, c.offset(x, -1, z), Blocks.OAK_PLANKS);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("lippo", c.offset(2, 0, r + 6));
        List<BlockPos> spots = List.of(c.offset(-15, 0, -8), c.offset(14, 0, -10), c.offset(-6, 0, 16), c.offset(12, 0, 12));
        return new ExamWorld.Site(c.offset(0, 0, r + 4), 180f, npcs, List.of(), spots);
    }

    // ------------------------------------------------------------------ Dolle

    /** Rue de Dolle et maison de la Mamie au quiz. */
    private static ExamWorld.Site dolle(ServerLevel level, BlockPos c, boolean place) {
        if (place) {
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) set(level, c.offset(x, -1, z), Blocks.GRASS_BLOCK);
            for (int z = -12; z <= 12; z++) for (int x = -1; x <= 1; x++) set(level, c.offset(x, -1, z), Blocks.COBBLESTONE);
            BlockPos h = c.offset(0, 0, -8);
            room(level, h, 4, 3, 4, Blocks.SPRUCE_PLANKS, Blocks.WHITE_TERRACOTTA, Blocks.DARK_OAK_PLANKS);
            for (int y = 0; y < 2; y++) set(level, h.offset(0, y, 3), Blocks.AIR);
            set(level, h.offset(-3, 0, -2), Blocks.BOOKSHELF);
            set(level, h.offset(3, 0, -2), Blocks.LANTERN);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("quiz_granny", c.offset(0, 0, -9));
        return new ExamWorld.Site(c.offset(0, 0, 4), 180f, npcs, List.of(), List.of());
    }

    // ------------------------------------------------------------------ Numele

    /** Marais de Numele : boue, flaques, arbres morts ; un chemin sinueux que Satotz suit dans le brouillard. */
    private static ExamWorld.Site numele(ServerLevel level, BlockPos c, boolean place) {
        int len = 80;
        if (place) {
            for (int x = -16; x <= 16; x++) for (int z = -6; z <= len + 6; z++) {
                int h = (x * 31 + z * 17) & 7;
                set(level, c.offset(x, -1, -z), h == 0 ? Blocks.WATER : h < 3 ? Blocks.MUD : Blocks.GRASS_BLOCK);
                set(level, c.offset(x, -2, -z), Blocks.MUD);
                if (h == 0 && ((x + z) & 3) == 0) set(level, c.offset(x, 0, -z), Blocks.LILY_PAD);
            }
            for (int k = 0; k < 18; k++) {
                int x = (k % 2 == 0 ? -1 : 1) * (6 + (k * 7) % 9);
                int z = 4 + k * 4;
                for (int y = 0; y < 4 + k % 3; y++) set(level, c.offset(x, y, -z), Blocks.STRIPPED_DARK_OAK_LOG);
            }
            // chemin praticable (le joueur doit le suivre pour ne pas s'enliser)
            for (int i = 0; i <= len; i++) {
                int x = (int) Math.round(Math.sin(i / 9.0) * 5);
                for (int dx = -1; dx <= 1; dx++) set(level, c.offset(x + dx, -1, -i), Blocks.PACKED_MUD);
            }
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("satotz", c.offset(0, 0, -2));
        // l'homme-singe qui se fait passer pour Satotz, à l'écart du chemin
        npcs.put("fake_satotz", c.offset(9, 0, -14));
        List<BlockPos> route = new ArrayList<>();
        for (int i = 10; i <= len; i += 10) route.add(c.offset((int) Math.round(Math.sin(i / 9.0) * 5), 0, -i));
        return new ExamWorld.Site(c.offset(0, 0, 3), 180f, npcs, route, List.of());
    }

    // ------------------------------------------------------------------ Mont Split

    /** Profondeur de la faille du mont Split et hauteur des nids sous le bord. */
    static final int RAVINE_DEPTH = 36, NEST_DEPTH = 12, RAVINE_HALF = 3;

    /**
     * Mont Split : deux falaises et une faille de 7 blocs de large ; des œufs d'aigle-araignée pendent sur des fils à
     * 12 blocs sous le bord. Un courant d'air remonte celui qui tient un œuf ; une rivière au fond.
     */
    private static ExamWorld.Site mountSplit(ServerLevel level, BlockPos c, boolean place) {
        if (place) {
            for (int z = -14; z <= 14; z++) {
                for (int x = -14; x <= 14; x++) {
                    boolean gap = Math.abs(x) <= RAVINE_HALF;
                    for (int y = -RAVINE_DEPTH; y <= -1; y++) {
                        Block b = gap ? (y <= -RAVINE_DEPTH + 3 ? Blocks.WATER : Blocks.AIR) : (y == -1 ? Blocks.GRASS_BLOCK : Blocks.STONE);
                        set(level, c.offset(x, y, z), b);
                    }
                    set(level, c.offset(x, -RAVINE_DEPTH - 1, z), Blocks.STONE);
                }
            }
            // fils tendus d'une paroi à l'autre au niveau des nids
            for (int z = -10; z <= 10; z += 5) for (int dz = -1; dz <= 1; dz++) for (int x = -RAVINE_HALF; x <= RAVINE_HALF; x++)
                set(level, c.offset(x, -NEST_DEPTH, z + dz), Blocks.COBWEB);
            for (int z = -14; z <= 14; z += 7) set(level, c.offset(-6, 0, z), Blocks.SPRUCE_FENCE);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("menchi", c.offset(-7, 0, 2));
        List<BlockPos> nests = new ArrayList<>();
        for (int z = -10; z <= 10; z += 5) nests.add(c.offset(0, -NEST_DEPTH + 1, z));
        return new ExamWorld.Site(c.offset(-6, 0, 0), -90f, npcs, List.of(), nests);
    }

    // ------------------------------------------------------------------ Dirigeable de l'Association

    /** Salle de détente du dirigeable (jeu de la balle) : parquet, hublots sur le ciel, Netero au fond. */
    private static ExamWorld.Site airship(ServerLevel level, BlockPos c, boolean place) {
        if (place) {
            room(level, c, 9, 5, 6, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.WHITE_CONCRETE);
            for (int x = -7; x <= 7; x += 3) {
                set(level, c.offset(x, 2, -5), Blocks.GLASS);
                set(level, c.offset(x, 2, 5), Blocks.GLASS);
            }
            for (int x = -8; x <= 8; x += 4) set(level, c.offset(x, 4, 0), Blocks.LANTERN);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("netero", c.offset(0, 0, -3));
        return new ExamWorld.Site(c.offset(0, 0, 4), 180f, npcs, List.of(), List.of());
    }
}
