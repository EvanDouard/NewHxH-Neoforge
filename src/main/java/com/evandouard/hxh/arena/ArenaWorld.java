package com.evandouard.hxh.arena;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * La dimension de la Tour Céleste et ce qu'on y construit par le code :
 * le hall d'accueil (guichet, ascenseur, sortie) et une arène par emplacement de combat.
 */
public final class ArenaWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, HxHMod.id("heavens_arena"));

    /** Sol du hall (les joueurs marchent à Y = 64). */
    public static final int FLOOR_Y = 64;
    public static final BlockPos LOBBY_SPAWN = new BlockPos(0, FLOOR_Y, 5);
    public static final BlockPos RECEPTIONIST = new BlockPos(0, FLOOR_Y, -8);
    public static final BlockPos ELEVATOR = new BlockPos(10, FLOOR_Y, 0);
    public static final BlockPos EXIT = new BlockPos(-10, FLOOR_Y, 0);
    private static final int LOBBY_HALF = 10;
    /** Bloc repère « hall construit » (dans un coin du sol). */
    private static final BlockPos LOBBY_MARKER = new BlockPos(-LOBBY_HALF, FLOOR_Y - 1, -LOBBY_HALF);
    private static final int LOBBY_HEIGHT = 7;

    /** Arènes : une plateforme de 19 × 19 dans le vide ; tomber en dessous = sortie de ring. */
    public static final int ARENA_HALF = 9;
    public static final int RING_OUT_Y = FLOOR_Y - 6;
    private static final int ARENA_SPACING = 100;
    private static final int ARENA_X0 = 1000;

    private ArenaWorld() {}

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIMENSION);
    }

    public static boolean isArenaLevel(Level level) {
        return level.dimension() == DIMENSION;
    }

    public static BlockPos arenaCenter(int slot) {
        return new BlockPos(ARENA_X0 + slot * ARENA_SPACING, FLOOR_Y, 0);
    }

    /** Vrai si la position est dans la zone des arènes (et pas dans le hall). */
    public static boolean inArenaZone(double x) {
        return x > ARENA_X0 - ARENA_SPACING / 2.0;
    }

    // ---------- Hall d'accueil ----------

    /** Construit le hall s'il n'existe pas encore. */
    public static void ensureLobby(ServerLevel level) {
        if (level.getBlockState(LOBBY_MARKER).is(Blocks.POLISHED_DEEPSLATE)) {
            ensureReceptionist(level);
            return;
        }
        buildLobby(level);
        ensureReceptionist(level);
    }

    private static void buildLobby(ServerLevel level) {
        BlockState floor = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState wall = Blocks.QUARTZ_BRICKS.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        BlockState light = Blocks.SEA_LANTERN.defaultBlockState();
        int y0 = FLOOR_Y - 1;
        for (int x = -LOBBY_HALF; x <= LOBBY_HALF; x++) {
            for (int z = -LOBBY_HALF; z <= LOBBY_HALF; z++) {
                boolean edge = Math.abs(x) == LOBBY_HALF || Math.abs(z) == LOBBY_HALF;
                boolean corner = Math.abs(x) == LOBBY_HALF && Math.abs(z) == LOBBY_HALF;
                // Sol : quartz avec un tapis rouge du guichet à l'entrée
                BlockState f = Math.abs(x) <= 1 && z > -7 ? Blocks.RED_WOOL.defaultBlockState() : floor;
                level.setBlock(new BlockPos(x, y0, z), f, 2);
                // Plafond avec des lampes en damier
                boolean lamp = !edge && Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 2;
                level.setBlock(new BlockPos(x, y0 + LOBBY_HEIGHT + 1, z), lamp ? light : wall, 2);
                for (int y = 1; y <= LOBBY_HEIGHT; y++) {
                    BlockPos p = new BlockPos(x, y0 + y, z);
                    if (corner) level.setBlock(p, pillar, 2);
                    else if (edge) {
                        boolean window = y >= 3 && y <= 5 && (Math.abs(x) + Math.abs(z)) % 3 != 0;
                        level.setBlock(p, window ? glass : wall, 2);
                    } else level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // Repère « hall construit »
        level.setBlock(LOBBY_MARKER, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);

        // Guichet : comptoir devant l'hôtesse
        for (int x = -3; x <= 3; x++) {
            level.setBlock(new BlockPos(x, FLOOR_Y, -7), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, FLOOR_Y + 1, -7), x == 0 ? Blocks.AIR.defaultBlockState()
                    : Blocks.DARK_OAK_SLAB.defaultBlockState(), 2);
        }
        for (int z = -9; z <= -7; z++) {
            level.setBlock(new BlockPos(-3, FLOOR_Y, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
            level.setBlock(new BlockPos(3, FLOOR_Y, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(0, FLOOR_Y, -7), Blocks.DARK_OAK_SLAB.defaultBlockState(), 2);

        // Ascenseur (mur est) dans un cadre en fer, sortie (mur ouest) dans un cadre sombre
        frame(level, ELEVATOR, Blocks.IRON_BLOCK.defaultBlockState());
        level.setBlock(ELEVATOR, ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
        level.setBlock(ELEVATOR.above(), ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
        frame(level, EXIT, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        level.setBlock(EXIT, ModBlocks.ARENA_EXIT.get().defaultBlockState(), 2);
        level.setBlock(EXIT.above(), ModBlocks.ARENA_EXIT.get().defaultBlockState(), 2);
    }

    /** Cadre de 3 × 4 autour d'une porte de 1 × 2 encastrée dans un mur est/ouest. */
    private static void frame(ServerLevel level, BlockPos door, BlockState state) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 2; dy++) {
                level.setBlock(door.offset(0, dy, dz), state, 2);
            }
        }
    }

    private static void ensureReceptionist(ServerLevel level) {
        AABB box = new AABB(RECEPTIONIST).inflate(4);
        if (!level.getEntitiesOfClass(NpcEntity.class, box, n -> n.npcId().equals("receptionist")).isEmpty()) return;
        NpcEntity npc = ModEntities.NPC.get().create(level);
        if (npc == null) return;
        npc.moveTo(RECEPTIONIST.getX() + 0.5, RECEPTIONIST.getY(), RECEPTIONIST.getZ() + 0.5, 0, 0);
        npc.setYHeadRot(0);
        npc.setNpcId("receptionist");
        npc.setHome(RECEPTIONIST);
        level.addFreshEntity(npc);
    }

    // ---------- Arènes ----------

    /** Construit (ou répare) l'arène d'un emplacement : plateforme, liseré, coins. */
    public static void buildArena(ServerLevel level, int slot) {
        BlockPos c = arenaCenter(slot);
        for (int x = -ARENA_HALF; x <= ARENA_HALF; x++) {
            for (int z = -ARENA_HALF; z <= ARENA_HALF; z++) {
                boolean edge = Math.abs(x) == ARENA_HALF || Math.abs(z) == ARENA_HALF;
                boolean centerLine = x == 0 && Math.abs(z) < ARENA_HALF;
                BlockState s = edge ? Blocks.WHITE_CONCRETE.defaultBlockState()
                        : centerLine ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                        : Blocks.STONE_BRICKS.defaultBlockState();
                level.setBlock(c.offset(x, -1, z), s, 2);
                level.setBlock(c.offset(x, -2, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                for (int y = 0; y <= 4; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        // Lampes aux quatre coins, sous la plateforme pour ne pas gêner le combat
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                level.setBlock(c.offset(sx * ARENA_HALF, -3, sz * ARENA_HALF), Blocks.SEA_LANTERN.defaultBlockState(), 2);
            }
        }
    }
}
