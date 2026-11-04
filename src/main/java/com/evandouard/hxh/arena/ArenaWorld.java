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
import org.jetbrains.annotations.Nullable;

/**
 * La dimension de la Tour Céleste et ce qu'on y construit par le code :
 * le hall d'accueil (étage 0 : guichet, ascenseur, sortie), un étage par numéro (ring au décor de l'étage,
 * palier avec ascenseur et arbitre), et des arènes de débordement quand le ring d'un étage est occupé.
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

    // ---------- Hall d'accueil ----------

    /**
     * Construit le hall (étage 0) et pose l'hôtesse, une seule fois par monde.
     * Un hall bâti avant cette règle (repère présent) est gardé tel quel, sans nouvelle hôtesse.
     */
    public static void ensureLobby(ServerLevel level) {
        ArenaSavedData data = ArenaSavedData.get(level);
        if (data.lobbyBuilt) return;
        data.lobbyBuilt = true;
        data.setDirty();
        if (level.getBlockState(LOBBY_MARKER).is(Blocks.POLISHED_DEEPSLATE)) return;
        buildLobby(level);
        spawnNpc(level, "receptionist", RECEPTIONIST, 0f);
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

    private static void spawnNpc(ServerLevel level, String id, BlockPos pos, float yaw) {
        NpcEntity npc = ModEntities.NPC.get().create(level);
        if (npc == null) return;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setNpcId(id);
        npc.setHome(pos);
        level.addFreshEntity(npc);
    }

    /**
     * Supprime les PNJ en double (même rôle, même place) : répare les mondes où l'hôtesse
     * était recréée à chaque entrée.
     * @return le nombre de doublons retirés
     */
    public static int removeDuplicateNpcs(ServerLevel level) {
        java.util.Map<String, NpcEntity> kept = new java.util.HashMap<>();
        int removed = 0;
        for (NpcEntity npc : level.getEntities(ModEntities.NPC.get(), n -> n.isAlive())) {
            String key = npc.npcId() + "@" + npc.home().asLong();
            if (kept.putIfAbsent(key, npc) != null) {
                npc.discard();
                removed++;
            }
        }
        return removed;
    }

    // ---------- Étages ----------

    /** Étages : alignés le long de l'axe Z, loin du hall. */
    private static final int FLOORS_X = 3000;
    private static final int FLOOR_SPACING = 48;
    /** Le palier (arrivée de l'ascenseur, arbitre) est séparé du ring par 4 blocs de vide. */
    private static final int LANDING_NEAR = ARENA_HALF + 5;
    private static final int LANDING_FAR = ARENA_HALF + 9;
    private static final int LANDING_HALF_WIDTH = 4;

    public static BlockPos floorCenter(int floor) {
        return new BlockPos(FLOORS_X, FLOOR_Y, floor * FLOOR_SPACING);
    }

    /** Point d'arrivée de l'ascenseur à un étage (sur le palier, face au ring). */
    public static BlockPos floorArrival(int floor) {
        return floorCenter(floor).offset(0, 0, -(LANDING_NEAR + 2));
    }

    public static BlockPos floorElevator(int floor) {
        return floorCenter(floor).offset(-LANDING_HALF_WIDTH, 0, -LANDING_FAR);
    }

    public static BlockPos floorReferee(int floor) {
        return floorCenter(floor).offset(3, 0, -LANDING_NEAR - 1);
    }

    /** Étage auquel correspond une position de la zone des étages (−1 hors de cette zone). */
    public static int floorAt(BlockPos pos) {
        if (Math.abs(pos.getX() - FLOORS_X) > 40) return -1;
        return Math.round(pos.getZ() / (float) FLOOR_SPACING);
    }

    public static boolean inFloorZone(double x) {
        return x > FLOORS_X - 100;
    }

    /** Zone des arènes de débordement (quand le ring d'un étage est déjà occupé). */
    public static boolean inOverflowZone(double x) {
        return x > ARENA_X0 - ARENA_SPACING / 2.0 && x < FLOORS_X - 100;
    }

    /** Arène de débordement numéro {@code slot}. */
    public static BlockPos overflowCenter(int slot) {
        return new BlockPos(ARENA_X0 + slot * ARENA_SPACING, FLOOR_Y, 0);
    }

    /** Construit (ou remet à neuf) un étage : ring au décor de l'étage, palier, ascenseur, et l'arbitre une seule fois. */
    public static void buildFloor(ServerLevel level, int floor) {
        BlockPos c = floorCenter(floor);
        ArenaTheme theme = ArenaThemes.forFloor(floor);
        buildRing(level, c, floor, theme);
        // Palier
        for (int x = -LANDING_HALF_WIDTH; x <= LANDING_HALF_WIDTH; x++) {
            for (int z = -LANDING_FAR; z <= -LANDING_NEAR; z++) {
                boolean edge = Math.abs(x) == LANDING_HALF_WIDTH || z == -LANDING_FAR || z == -LANDING_NEAR;
                level.setBlock(c.offset(x, -1, z), edge ? theme.border() : theme.floor().get(0), 2);
                level.setBlock(c.offset(x, -2, z), theme.under(), 2);
            }
        }
        BlockPos elevator = floorElevator(floor);
        level.setBlock(elevator, ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
        level.setBlock(elevator.above(), ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);

        ArenaSavedData data = ArenaSavedData.get(level);
        if (data.floorsWithReferee.add(floor)) {
            data.setDirty();
            spawnNpc(level, "referee", floorReferee(floor), 0f);
        }
    }

    /** Construit une arène de débordement au décor de l'étage {@code floor}. */
    public static void buildOverflow(ServerLevel level, int slot, int floor) {
        buildRing(level, overflowCenter(slot), floor, ArenaThemes.forFloor(floor));
    }

    /** Le ring : sol à motif, liseré, ligne et cercle centraux, piliers et lumières aux coins. */
    public static void buildRing(ServerLevel level, BlockPos c, int floor, ArenaTheme theme) {
        for (int x = -ARENA_HALF; x <= ARENA_HALF; x++) {
            for (int z = -ARENA_HALF; z <= ARENA_HALF; z++) {
                boolean edge = Math.abs(x) == ARENA_HALF || Math.abs(z) == ARENA_HALF;
                int d2 = x * x + z * z;
                boolean accent = (x == 0 && Math.abs(z) < ARENA_HALF) || (d2 >= 6 && d2 <= 10);
                BlockState s = edge ? theme.border()
                        : accent ? theme.accent()
                        : theme.floor().get(theme.floorIndex(floor, x, z));
                level.setBlock(c.offset(x, -1, z), s, 2);
                level.setBlock(c.offset(x, -2, z), theme.under(), 2);
                for (int y = 0; y <= 6; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (!theme.pillar().isAir() && theme.pillarHeight() > 0) {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    BlockPos base = c.offset(sx * ARENA_HALF, 0, sz * ARENA_HALF);
                    for (int y = 0; y < theme.pillarHeight(); y++) level.setBlock(base.above(y), theme.pillar(), 2);
                    if (!theme.light().isAir()) level.setBlock(base.above(theme.pillarHeight()), theme.light(), 2);
                }
            }
        }
    }
}
