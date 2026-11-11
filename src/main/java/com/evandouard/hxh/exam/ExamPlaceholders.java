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
            case TOWER -> trickTower(level, c, place);
            case KIRIKO -> kirikoForest(level, c, place);
            case ISLAND_HOTEL -> islandHotel(level, c, place);
            case OPENING_288 -> opening288(level, c, place);
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
            // la « salle » qui descend : une plateforme de 3 × 3 avec deux sièges, au-dessus d'un puits fermé
            zabanLift(level, c, 0);
            for (int y = -ZABAN_SHAFT - 1; y <= -2; y++) for (int x = -2; x <= 2; x++) for (int z = ZABAN_LIFT_Z - 2; z <= ZABAN_LIFT_Z + 2; z++) {
                boolean wall = Math.abs(x) == 2 || Math.abs(z - ZABAN_LIFT_Z) == 2 || y == -ZABAN_SHAFT - 1;
                set(level, c.offset(x, y, z), wall ? Blocks.STONE_BRICKS : Blocks.AIR);
            }
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("cook", c.offset(0, 0, -3));
        return new ExamWorld.Site(c.offset(0, 0, 3), 180f, npcs, List.of(), List.of());
    }

    /** Plateforme de la salle qui descend (Zaban) : centre en z, profondeur du puits. */
    public static final int ZABAN_LIFT_Z = 2, ZABAN_SHAFT = 12;

    /**
     * Pose la plateforme de Zaban {@code depth} blocs sous le sol (et efface celle d'au-dessus) : plancher de 3 × 3
     * et deux sièges.
     */
    public static void zabanLift(ServerLevel level, BlockPos c, int depth) {
        for (int x = -1; x <= 1; x++) for (int z = ZABAN_LIFT_Z - 1; z <= ZABAN_LIFT_Z + 1; z++) {
            if (depth > 0) {
                set(level, c.offset(x, -depth, z), Blocks.AIR);
                set(level, c.offset(x, 1 - depth, z), Blocks.AIR);
            }
            set(level, c.offset(x, -1 - depth, z), Blocks.DARK_OAK_PLANKS);
        }
        for (int x : new int[]{-1, 1})
            set(level, c.offset(x, -depth, ZABAN_LIFT_Z), Blocks.OAK_STAIRS.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.SOUTH));
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
        seal(level, c, -r, r, -r, r, -5, -1);
    }

    /**
     * Ferme un bassin posé dans le vide : de la pierre sous le fond (le sable ne tombe plus) et des murs de barrière
     * tout autour (l'eau ne coule plus par les bords, ce qui faisait laguer à l'arrivée).
     */
    static void seal(ServerLevel level, BlockPos c, int x0, int x1, int z0, int z1, int yBottom, int yTop) {
        for (int x = x0 - 1; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) {
            set(level, c.offset(x, yBottom - 1, z), Blocks.STONE);
            boolean edge = x < x0 || x > x1 || z < z0 || z > z1;
            if (edge) for (int y = yBottom; y <= yTop; y++) set(level, c.offset(x, y, z), Blocks.BARRIER);
        }
    }

    /** Abris du pont du bateau (sous la voile, dans la cabine, à la proue) : décalages depuis le centre. */
    static final int[][] BOAT_SHELTERS = {{0, 0, -4}, {-2, 0, 11}, {-3, 0, -12}, {3, 0, 4}};

    /**
     * Bateau de pêche vers Dolle : pont de 9 × 31, mât et voile, cabine du capitaine à la poupe, caisses ; les abris
     * (où se mettre quand la grosse vague arrive) sont {@link #BOAT_SHELTERS}.
     */
    private static ExamWorld.Site boat(ServerLevel level, BlockPos c, boolean place) {
        if (place) {
            sea(level, c, 40);
            for (int x = -4; x <= 4; x++) for (int z = -15; z <= 15; z++) {
                boolean edge = Math.abs(x) == 4 || Math.abs(z) == 15;
                set(level, c.offset(x, -1, z), Blocks.OAK_PLANKS);
                set(level, c.offset(x, -2, z), Blocks.SPRUCE_PLANKS);
                if (edge) set(level, c.offset(x, 0, z), Blocks.SPRUCE_FENCE);
            }
            // mât et voile
            for (int y = 0; y < 10; y++) set(level, c.offset(0, y, -2), Blocks.SPRUCE_LOG);
            for (int x = -4; x <= 4; x++) for (int y = 3; y < 9; y++) set(level, c.offset(x, y, -1), Blocks.WHITE_WOOL);
            // cabine du capitaine (poupe), ouverte vers l'avant
            for (int x = -3; x <= 3; x++) for (int z = 9; z <= 14; z++) for (int y = 0; y <= 3; y++) {
                boolean wall = Math.abs(x) == 3 || z == 14 || (z == 9 && Math.abs(x) > 1);
                if (y == 3) set(level, c.offset(x, y, z), Blocks.SPRUCE_SLAB);
                else if (wall) set(level, c.offset(x, y, z), Blocks.SPRUCE_PLANKS);
            }
            set(level, c.offset(2, 0, 13), Blocks.BARREL);
            set(level, c.offset(-2, 2, 13), Blocks.LANTERN);
            // caisses à la proue et sur le pont (abris)
            for (int[] b : new int[][]{{-3, 0, -13}, {-2, 0, -13}, {-3, 1, -13}, {3, 0, 3}, {3, 0, 5}, {3, 1, 3}})
                set(level, c.offset(b[0], b[1], b[2]), Blocks.BARREL);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("captain", c.offset(0, 0, 11));
        return new ExamWorld.Site(c.offset(0, 0, 3), 0f, npcs, List.of(), List.of());
    }

    /** Île de Zevil : plage, forêt au centre, ponton d'arrivée ; des candidats rôdent avec leur plaque. */
    private static ExamWorld.Site zevil(ServerLevel level, BlockPos c, boolean place) {
        int r = 44;
        if (place) {
            sea(level, c, r + 12);
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                int d2 = x * x + z * z;
                if (d2 > r * r) continue;
                for (int y = -4; y <= -2; y++) set(level, c.offset(x, y, z), d2 > (r - 5) * (r - 5) ? Blocks.SAND : Blocks.DIRT);
                set(level, c.offset(x, -1, z), d2 > (r - 5) * (r - 5) ? Blocks.SAND : Blocks.GRASS_BLOCK);
            }
            for (int k = 0; k < 30; k++) {
                double a = k * 2.3;
                double rr = 6 + (k % 7) * 4.5;
                tree(level, c.offset((int) Math.round(Math.cos(a) * rr), 0, (int) Math.round(Math.sin(a) * rr)));
            }
            // ponton
            for (int z = r - 2; z <= r + 8; z++) for (int x = -1; x <= 1; x++) set(level, c.offset(x, -1, z), Blocks.OAK_PLANKS);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("lippo", c.offset(2, 0, r + 6));
        // candidats : en jeu, les repères « apparition de proies » (G) de la structure d'Evan remplacent ces points
        List<BlockPos> spots = List.of(c.offset(-15, 0, -8), c.offset(14, 0, -10), c.offset(-6, 0, 16), c.offset(12, 0, 12),
                c.offset(-28, 0, 10), c.offset(26, 0, -22), c.offset(4, 0, -32), c.offset(-20, 0, -26));
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

    // ------------------------------------------------------------------ Tour de Trick

    /**
     * Le long chemin : à l'extérieur de la tour, côté est (du nord au sud), puis sud (d'est en ouest), puis ouest
     * jusqu'à l'entrée du 2e étage ; il descend régulièrement du 1er au 2e étage.
     */
    static List<BlockPos> towerLongPath(BlockPos c) {
        List<int[]> xz = new ArrayList<>();
        for (int z = -6; z <= 9; z++) xz.add(new int[]{9, z});
        for (int x = 8; x >= -9; x--) xz.add(new int[]{x, 9});
        for (int z = 8; z >= 2; z--) xz.add(new int[]{-9, z});
        xz.add(new int[]{-8, 2});
        List<BlockPos> out = new ArrayList<>();
        int n = xz.size();
        for (int i = 0; i < n; i++) {
            int y = TOWER_F1 - Math.round((TOWER_F1 - TOWER_F2) * i / (float) (n - 1));
            out.add(c.offset(xz.get(i)[0], y, xz.get(i)[1]));
        }
        return out;
    }

    /** Hauteurs (au-dessus du sol) : toit, plancher du 1er étage, plate-forme du 2e. */
    static final int TOWER_ROOF = 40, TOWER_F1 = 28, TOWER_F2 = 12, TOWER_HALF = 7;
    /** Trappes du toit (x, z) ; une seule, différente selon l'exemplaire, s'ouvre sur le vide. */
    static final int[][] TOWER_TRAPDOORS = {{-4, -4}, {4, -4}, {0, 0}, {-4, 3}, {4, 3}};

    /**
     * Tour de Trick provisoire : tour carrée de 15 × 15 et 40 de haut. Toit à trappes (une seule vraie, au-dessus
     * d'un bassin du 1er étage), 1er étage avec Tonpa (vote de la majorité), plate-forme du 2e (combat contre un
     * prisonnier) et escalier le long du mur nord jusqu'à la sortie ouest.
     */
    private static ExamWorld.Site trickTower(ServerLevel level, BlockPos c, boolean place) {
        int h = TOWER_HALF;
        int real = Math.floorMod(c.getZ() / 400, TOWER_TRAPDOORS.length);
        if (place) {
            for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) set(level, c.offset(x, -1, z), Blocks.COARSE_DIRT);
            for (int x = -h; x <= h; x++) for (int z = -h; z <= h; z++) {
                boolean wall = Math.abs(x) == h || Math.abs(z) == h;
                for (int y = -1; y <= TOWER_ROOF + 1; y++)
                    set(level, c.offset(x, y, z), wall ? Blocks.STONE_BRICKS : Blocks.AIR);
                if (wall) continue;
                set(level, c.offset(x, -1, z), Blocks.POLISHED_ANDESITE);
                set(level, c.offset(x, TOWER_F1 - 1, z), Blocks.STONE_BRICKS);
                set(level, c.offset(x, TOWER_ROOF - 1, z), Blocks.CHISELED_STONE_BRICKS);
                if (z >= -4) set(level, c.offset(x, TOWER_F2 - 1, z), Blocks.SMOOTH_STONE);
            }
            // toit : créneaux et trappes
            for (int x = -h; x <= h; x += 2) {
                set(level, c.offset(x, TOWER_ROOF + 1, -h), Blocks.AIR);
                set(level, c.offset(x, TOWER_ROOF + 1, h), Blocks.AIR);
            }
            for (int i = 0; i < TOWER_TRAPDOORS.length; i++) {
                int[] t = TOWER_TRAPDOORS[i];
                set(level, c.offset(t[0], TOWER_ROOF - 1, t[1]), Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
                if (i != real) set(level, c.offset(t[0], TOWER_ROOF - 2, t[1]), Blocks.STONE_BRICKS);
            }
            // bassin sous la vraie trappe
            int[] r = TOWER_TRAPDOORS[real];
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                // une seule couche d'eau (suffit à amortir la chute) posée sur la pierre : rien ne fuit vers le 2e étage
                set(level, c.offset(r[0] + dx, TOWER_F1 - 1, r[1] + dz), Blocks.WATER);
                set(level, c.offset(r[0] + dx, TOWER_F1 - 2, r[1] + dz), Blocks.STONE_BRICKS);
            }
            // 1er étage : deux portes (O et X) sur le mur sud
            set(level, c.offset(-3, TOWER_F1, h), Blocks.WHITE_WOOL);
            set(level, c.offset(3, TOWER_F1, h), Blocks.BLACK_WOOL);
            for (int x : new int[]{-3, 3}) for (int y = 0; y < 2; y++) set(level, c.offset(x, TOWER_F1 + y, h - 1), Blocks.IRON_BARS);
            for (int x = -6; x <= 6; x += 4) set(level, c.offset(x, TOWER_F1 + 3, -6), Blocks.LANTERN);
            // escalier du 2e étage jusqu'au sol, le long du mur nord, puis sortie à l'ouest
            for (int k = 0; k <= TOWER_F2 - 2; k++) for (int z = -6; z <= -5; z++)
                set(level, c.offset(6 - k, TOWER_F2 - 2 - k, z), Blocks.STONE_BRICKS);
            for (int y = 0; y < 2; y++) set(level, c.offset(-h, y, -5), Blocks.AIR);
            // pièges de l'escalier : plaques de pression sur certaines marches, distributeurs de flèches dans le mur
            for (int k = 2; k <= TOWER_F2 - 2; k += 3) {
                BlockPos step = c.offset(6 - k, TOWER_F2 - 2 - k, -6);
                set(level, step.above(), Blocks.STONE_PRESSURE_PLATE);
                level.setBlock(c.offset(6 - k, TOWER_F2 - 1 - k, -h), Blocks.DISPENSER.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, net.minecraft.core.Direction.SOUTH), 2);
                if (level.getBlockEntity(c.offset(6 - k, TOWER_F2 - 1 - k, -h)) instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity disp)
                    disp.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 16));
            }
            // le long chemin (vote perdu) : galerie qui fait le tour de la tour par l'extérieur en descendant
            List<BlockPos> path = towerLongPath(c);
            java.util.Set<BlockPos> cells = new java.util.HashSet<>();
            for (BlockPos p : path) cells.add(new BlockPos(p.getX(), 0, p.getZ()));
            for (int i = 0; i < path.size(); i++) {
                BlockPos p = path.get(i);
                set(level, p.below(), Blocks.STONE_BRICKS);
                for (int y = 0; y < 3; y++) set(level, p.above(y), Blocks.AIR);
                set(level, p.above(3), Blocks.STONE_BRICKS);
                for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    BlockPos side = p.offset(n[0], 0, n[1]);
                    if (cells.contains(new BlockPos(side.getX(), 0, side.getZ()))) continue;
                    if (Math.abs(side.getX() - c.getX()) <= h && Math.abs(side.getZ() - c.getZ()) <= h) continue; // mur de la tour
                    for (int y = -1; y <= 3; y++) set(level, side.above(y), Blocks.STONE_BRICKS);
                }
                if (i % 8 == 4) set(level, p.above(2), Blocks.LANTERN);
            }
            // entrée au 2e étage par le mur ouest
            BlockPos end = path.get(path.size() - 1);
            for (int y = 0; y < 2; y++) set(level, new BlockPos(c.getX() - h, end.getY() + y, end.getZ()), Blocks.AIR);
            // 2e étage éclairé (le combat doit se voir) : lanternes au mur et au plafond
            for (int x = -6; x <= 6; x += 3) {
                set(level, c.offset(x, TOWER_F2 + 2, 6), Blocks.LANTERN);
                set(level, c.offset(x, TOWER_F2 + 2, -4), Blocks.LANTERN);
            }
            for (int x = -6; x <= 6; x += 4) set(level, c.offset(x, TOWER_F1 + 3, 6), Blocks.LANTERN);
            // pièce secrète du 2e étage (angle sud-est) : on y entre par un faux mur (clic droit, il s'efface un instant)
            for (int x = 3; x <= 7; x++) for (int z = 3; z <= 7; z++) for (int y = 0; y <= 3; y++) {
                boolean edge = x == 3 || x == 7 || z == 3 || z == 7 || y == 3;
                set(level, c.offset(x, TOWER_F2 + y, z), edge ? Blocks.STONE_BRICKS : Blocks.AIR);
            }
            for (int y = 0; y < 2; y++) set(level, c.offset(3, TOWER_F2 + y, 5),
                    com.evandouard.hxh.registry.ModBlocks.ILLUSION_WALL.get());
            set(level, c.offset(5, TOWER_F2 + 2, 5), Blocks.LANTERN);
            set(level, c.offset(6, TOWER_F2, 6), Blocks.BARREL);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("lippo", c.offset(0, TOWER_ROOF, -5));
        npcs.put("tonpa", c.offset(2, TOWER_F1, 3));
        int[] r = TOWER_TRAPDOORS[real];
        List<BlockPos> route = List.of(c.offset(r[0], TOWER_F1 - 1, r[1]), c.offset(0, TOWER_F2, 2), c.offset(-h + 1, 0, -5),
                towerLongPath(c).get(0));
        return new ExamWorld.Site(c.offset(0, TOWER_ROOF, 4), 180f, npcs, route, List.of());
    }

    // ------------------------------------------------------------------ Forêt des Kiriko

    /** Forêt sombre de chênes noirs, clairière et cabane ; la femme blessée et les deux maris devant la porte. */
    private static ExamWorld.Site kirikoForest(ServerLevel level, BlockPos c, boolean place) {
        if (place) {
            for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
                set(level, c.offset(x, -1, z), ((x * 7 + z * 13) & 3) == 0 ? Blocks.PODZOL : Blocks.GRASS_BLOCK);
                set(level, c.offset(x, -2, z), Blocks.DIRT);
            }
            for (int x = -22; x <= 22; x += 5) for (int z = -22; z <= 22; z += 5) {
                int jx = x + ((x * 31 + z * 17) & 3) - 1, jz = z + ((x * 13 + z * 29) & 3) - 1;
                if (jx * jx + jz * jz < 9 * 9) continue; // clairière
                darkTree(level, c.offset(jx, 0, jz), 6 + ((x + z) & 3));
            }
            // cabane
            BlockPos h = c.offset(0, 0, -6);
            room(level, h, 3, 3, 3, Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.DARK_OAK_PLANKS);
            for (int y = 0; y < 2; y++) set(level, h.offset(0, y, 3), Blocks.AIR);
            set(level, c.offset(-2, 0, -1), Blocks.LANTERN);
            set(level, c.offset(2, 0, -1), Blocks.LANTERN);
            set(level, c.offset(0, 0, 1), Blocks.RED_CARPET); // sang sur le sol
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("kiriko_wife", c.offset(0, 0, -2));
        npcs.put("husband_1", c.offset(-3, 0, 0));
        npcs.put("husband_2", c.offset(3, 0, 0));
        return new ExamWorld.Site(c.offset(0, 0, 7), 180f, npcs, List.of(), List.of());
    }

    private static void darkTree(ServerLevel level, BlockPos base, int height) {
        for (int y = 0; y < height; y++) set(level, base.above(y), Blocks.DARK_OAK_LOG);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = height - 2; y <= height; y++)
            if (Math.abs(x) + Math.abs(z) < 4 && level.getBlockState(base.offset(x, y, z)).isAir())
                set(level, base.offset(x, y, z), Blocks.DARK_OAK_LEAVES);
    }

    // ------------------------------------------------------------------ Île-hôtel (hors-série 1999)

    /** Profondeur de la baie où repose l'épave. */
    static final int BAY_DEPTH = 9;

    /**
     * Île-hôtel : une île ronde avec l'hôtel (et son gérant) et un ponton ; au sud, une baie profonde où repose une
     * épave (coque de chêne noir, mât brisé) dont le pont porte des pièces d'or ; deux canons sur la plage.
     */
    private static ExamWorld.Site islandHotel(ServerLevel level, BlockPos c, boolean place) {
        int r = 16;
        BlockPos wreck = c.offset(0, -BAY_DEPTH + 1, 26);
        if (place) {
            for (int x = -40; x <= 40; x++) for (int z = -30; z <= 46; z++) {
                set(level, c.offset(x, -BAY_DEPTH - 1, z), Blocks.SAND);
                for (int y = -BAY_DEPTH; y <= -1; y++) set(level, c.offset(x, y, z), Blocks.WATER);
            }
            seal(level, c, -40, 40, -30, 46, -BAY_DEPTH - 1, -1);
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                int d2 = x * x + z * z;
                if (d2 > r * r) continue;
                for (int y = -BAY_DEPTH; y <= -2; y++) set(level, c.offset(x, y, z), Blocks.SANDSTONE);
                set(level, c.offset(x, -1, z), d2 > (r - 4) * (r - 4) ? Blocks.SAND : Blocks.GRASS_BLOCK);
            }
            BlockPos h = c.offset(0, 0, -5);
            room(level, h, 6, 4, 4, Blocks.WHITE_TERRACOTTA, Blocks.WHITE_CONCRETE, Blocks.BLUE_TERRACOTTA);
            for (int y = 0; y < 2; y++) set(level, h.offset(0, y, 4), Blocks.AIR);
            set(level, h.offset(-3, 0, -2), Blocks.BARREL);
            set(level, h.offset(3, 0, -2), Blocks.LANTERN);
            // ponton vers la baie
            for (int z = r - 2; z <= r + 5; z++) for (int x = -1; x <= 1; x++) set(level, c.offset(x, -1, z), Blocks.OAK_PLANKS);
            // canons sur la plage (provisoires : enclume sur un bloc de fer)
            for (int x : new int[]{-8, 8}) {
                set(level, c.offset(x, 0, r - 4), Blocks.IRON_BLOCK);
                set(level, c.offset(x, 1, r - 4), Blocks.ANVIL);
            }
            // épave : coque, pont, mât brisé
            for (int x = -3; x <= 3; x++) for (int z = -7; z <= 7; z++) {
                boolean edge = Math.abs(x) == 3 || Math.abs(z) == 7;
                set(level, wreck.offset(x, -1, z), Blocks.DARK_OAK_PLANKS);
                if (edge && ((x + z) & 3) != 0) set(level, wreck.offset(x, 0, z), Blocks.DARK_OAK_FENCE);
            }
            for (int y = 0; y < 4; y++) set(level, wreck.offset(0, y, -1), Blocks.STRIPPED_DARK_OAK_LOG);
            set(level, wreck.offset(1, 0, 3), Blocks.CHEST);
            set(level, wreck.offset(-2, 0, -4), Blocks.SEAGRASS);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("hotel_manager", c.offset(0, 0, -7));
        List<BlockPos> coins = List.of(wreck.offset(-2, 0, 4), wreck.offset(2, 0, -2), wreck.offset(0, 0, 6), wreck.offset(-1, 0, -5));
        return new ExamWorld.Site(c.offset(0, 0, 4), 180f, npcs, List.of(), coins);
    }

    // ------------------------------------------------------------------ 288e Examen (OVA)

    /** Esplanade d'ouverture : dalles, barrières tout autour, une estrade pour Beans. */
    private static ExamWorld.Site opening288(ServerLevel level, BlockPos c, boolean place) {
        int r = 16;
        if (place) {
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                set(level, c.offset(x, -1, z), ((x + z) & 1) == 0 ? Blocks.SMOOTH_STONE : Blocks.POLISHED_ANDESITE);
                for (int y = 0; y < 4; y++) set(level, c.offset(x, y, z), Blocks.AIR);
                if (Math.abs(x) == r || Math.abs(z) == r) set(level, c.offset(x, 0, z), Blocks.IRON_BARS);
            }
            for (int x = -2; x <= 2; x++) for (int z = -r + 1; z <= -r + 3; z++) set(level, c.offset(x, 0, z), Blocks.STONE_BRICK_SLAB);
            for (int x = -r; x <= r; x += 8) set(level, c.offset(x, 1, -r), Blocks.LANTERN);
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("beans", c.offset(0, 1, -r + 2));
        return new ExamWorld.Site(c.offset(0, 0, 4), 180f, npcs, List.of(), List.of());
    }
}
