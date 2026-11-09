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
        }
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        npcs.put("buhara", c.offset(-3, 0, -6));
        npcs.put("menchi", c.offset(3, 0, -6));
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
}
