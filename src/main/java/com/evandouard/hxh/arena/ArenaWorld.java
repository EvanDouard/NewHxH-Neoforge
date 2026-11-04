package com.evandouard.hxh.arena;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La dimension de la Tour Céleste et ce qu'on y construit :
 * <ul>
 *     <li>le hall d'accueil (étage 0) : construit par le code, ou la structure {@code hxh:heavens_arena/lobby} si elle existe ;</li>
 *     <li>un étage par numéro : ring généré selon le décor de l'étage, ou la structure indiquée par ce décor ;</li>
 *     <li>des arènes de débordement quand le ring d'un étage est occupé.</li>
 * </ul>
 * Dans une structure, des blocs repères disent où placer le joueur, l'adversaire, le PNJ et l'arrivée de
 * l'ascenseur ; ils sont retirés une fois la structure posée.
 */
public final class ArenaWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, HxHMod.id("heavens_arena"));
    public static final ResourceLocation LOBBY_STRUCTURE = HxHMod.id("heavens_arena/lobby");

    /** Sol du hall et des étages (on marche à Y = 64). */
    public static final int FLOOR_Y = 64;
    public static final BlockPos LOBBY_SPAWN = new BlockPos(0, FLOOR_Y, 5);
    public static final BlockPos RECEPTIONIST = new BlockPos(0, FLOOR_Y, -8);
    public static final BlockPos ELEVATOR = new BlockPos(10, FLOOR_Y, 0);
    public static final BlockPos EXIT = new BlockPos(-10, FLOOR_Y, 0);
    private static final int LOBBY_HALF = 10;
    /** Bloc repère « hall construit » (dans un coin du sol). */
    private static final BlockPos LOBBY_MARKER = new BlockPos(-LOBBY_HALF, FLOOR_Y - 1, -LOBBY_HALF);
    private static final int LOBBY_HEIGHT = 7;

    /** Ring généré : 19 × 19 dans le vide. */
    public static final int ARENA_HALF = 9;
    /** Tomber de plus de 6 blocs sous son point de départ = sortie de ring. */
    public static final int RING_OUT_DROP = 6;
    private static final int ARENA_SPACING = 200;
    private static final int ARENA_X0 = 1000;

    /** Étages alignés le long de Z ; une structure d'étage peut faire jusqu'à ~150 × 150 blocs. */
    private static final int FLOORS_X = 5000;
    private static final int FLOOR_SPACING = 160;
    private static final int LANDING_NEAR = ARENA_HALF + 5;
    private static final int LANDING_FAR = ARENA_HALF + 9;
    private static final int LANDING_HALF_WIDTH = 4;

    /**
     * Où se trouvent les choses d'un étage une fois construit.
     * @param ring zone de combat (sert à savoir si le ring est occupé et à le nettoyer)
     */
    public record FloorLayout(BlockPos arrival, float arrivalYaw, BlockPos playerStart, BlockPos opponentStart,
                              @Nullable BlockPos npc, AABB ring) {
        public int ringOutY() {
            return Math.min(playerStart.getY(), opponentStart.getY()) - RING_OUT_DROP;
        }
    }

    /** Dernière disposition construite de chaque étage (0 = hall). */
    private static final Map<Integer, FloorLayout> LAYOUTS = new HashMap<>();

    private ArenaWorld() {}

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIMENSION);
    }

    public static boolean isArenaLevel(Level level) {
        return level.dimension() == DIMENSION;
    }

    // ---------- Hall d'accueil (étage 0) ----------

    /**
     * Prépare le hall et renvoie où faire apparaître le joueur.
     * Avec la structure {@code hxh:heavens_arena/lobby}, elle est reposée à chaque entrée (une modification
     * du fichier se voit tout de suite) ; sinon le hall du code est construit une seule fois par monde.
     */
    public static FloorLayout ensureLobby(ServerLevel level) {
        ArenaSavedData data = ArenaSavedData.get(level);
        Optional<StructureTemplate> template = level.getStructureManager().get(LOBBY_STRUCTURE);
        if (template.isPresent()) {
            if (!data.lobbyBuilt || !data.lobbyFromStructure) clearBox(level, new BlockPos(-LOBBY_HALF - 1, FLOOR_Y - 2, -LOBBY_HALF - 1),
                    new BlockPos(LOBBY_HALF + 1, FLOOR_Y + LOBBY_HEIGHT + 2, LOBBY_HALF + 1));
            data.lobbyBuilt = true;
            data.lobbyFromStructure = true;
            data.setDirty();
            Placed p = placeStructure(level, template.get(), new BlockPos(0, FLOOR_Y, 0), 0);
            BlockPos arrival = p.first(ModBlocks.MARKER_ARRIVAL.get(), new BlockPos(0, FLOOR_Y, 0));
            BlockPos npc = p.first(ModBlocks.MARKER_NPC.get(), null);
            if (npc != null) ensureNpc(level, 0, "receptionist", npc);
            FloorLayout layout = new FloorLayout(arrival, yawToward(arrival, npc), arrival, arrival, npc, new AABB(arrival));
            LAYOUTS.put(0, layout);
            return layout;
        }
        if (!data.lobbyBuilt || data.lobbyFromStructure) {
            boolean legacy = !data.lobbyBuilt && level.getBlockState(LOBBY_MARKER).is(Blocks.POLISHED_DEEPSLATE);
            data.lobbyBuilt = true;
            data.lobbyFromStructure = false;
            data.setDirty();
            if (!legacy) buildLobby(level);
            ensureNpc(level, 0, "receptionist", RECEPTIONIST);
        }
        FloorLayout layout = new FloorLayout(LOBBY_SPAWN, 180f, LOBBY_SPAWN, LOBBY_SPAWN, RECEPTIONIST, new AABB(LOBBY_SPAWN));
        LAYOUTS.put(0, layout);
        return layout;
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

    // ---------- PNJ fixes (hôtesse, arbitres) ----------

    /**
     * Pose le PNJ d'un étage s'il n'est pas déjà enregistré à cette place. On ne cherche pas l'entité
     * (elle n'est peut-être pas encore chargée) : on se fie au registre du monde.
     */
    static void ensureNpc(ServerLevel level, int floor, String id, BlockPos pos) {
        ArenaSavedData data = ArenaSavedData.get(level);
        Long known = data.npcHomes.get(floor);
        if (known != null && known == pos.asLong()) return;
        data.npcHomes.put(floor, pos.asLong());
        data.setDirty();
        // L'ancien PNJ (si chargé) part tout de suite ; sinon le ménage périodique s'en charge
        for (NpcEntity old : level.getEntities(ModEntities.NPC.get(), n -> n.npcId().equals(id) && !n.home().equals(pos)
                && npcFloor(n) == floor)) old.discard();
        spawnNpc(level, id, pos, 0f);
    }

    private static int npcFloor(NpcEntity npc) {
        return npc.npcId().equals("receptionist") ? 0 : floorAt(npc.home());
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
     * Supprime les PNJ en double (même rôle, même place).
     * @return le nombre de doublons retirés
     */
    public static int removeDuplicateNpcs(ServerLevel level) {
        Map<String, NpcEntity> kept = new HashMap<>();
        int removed = 0;
        for (NpcEntity npc : level.getEntities(ModEntities.NPC.get(), NpcEntity::isAlive)) {
            String key = npc.npcId() + "@" + npc.home().asLong();
            if (kept.putIfAbsent(key, npc) != null) {
                npc.discard();
                removed++;
            }
        }
        return removed;
    }

    /** Ménage de la Tour : doublons, et hôtesse ou arbitres qui ne sont plus à la place enregistrée. */
    public static void cleanNpcs(ServerLevel level) {
        removeDuplicateNpcs(level);
        ArenaSavedData data = ArenaSavedData.get(level);
        for (NpcEntity npc : level.getEntities(ModEntities.NPC.get(), n -> n.npcId().equals("receptionist") || n.npcId().equals("referee"))) {
            Long home = data.npcHomes.get(npcFloor(npc));
            if (home == null || home != npc.home().asLong()) npc.discard();
        }
    }

    // ---------- Étages ----------

    public static BlockPos floorCenter(int floor) {
        return new BlockPos(FLOORS_X, FLOOR_Y, floor * FLOOR_SPACING);
    }

    /** Étage auquel correspond une position de la zone des étages (−1 hors de cette zone). */
    public static int floorAt(BlockPos pos) {
        if (Math.abs(pos.getX() - FLOORS_X) > FLOOR_SPACING / 2) return -1;
        return Math.round(pos.getZ() / (float) FLOOR_SPACING);
    }

    public static boolean inFloorZone(double x) {
        return Math.abs(x - FLOORS_X) <= FLOOR_SPACING / 2.0;
    }

    /** Zone des arènes de débordement (quand le ring d'un étage est déjà occupé). */
    public static boolean inOverflowZone(double x) {
        return x > ARENA_X0 - ARENA_SPACING / 2.0 && x < FLOORS_X - FLOOR_SPACING;
    }

    public static BlockPos overflowCenter(int slot) {
        return new BlockPos(ARENA_X0 + slot * ARENA_SPACING, FLOOR_Y, 0);
    }

    /** Disposition connue d'un étage (celle par défaut s'il n'a jamais été construit depuis le démarrage). */
    public static FloorLayout layout(int floor) {
        FloorLayout l = LAYOUTS.get(floor);
        return l != null ? l : generatedLayout(floorCenter(floor));
    }

    /** Construit (ou remet à neuf) un étage selon son décor, et pose son arbitre s'il n'y est pas encore. */
    public static FloorLayout buildFloor(ServerLevel level, int floor) {
        return buildFloor(level, floor, ArenaThemes.forFloor(floor));
    }

    public static FloorLayout buildFloor(ServerLevel level, int floor, ArenaTheme theme) {
        FloorLayout layout = build(level, floorCenter(floor), floor, theme, true);
        if (layout.npc() != null) ensureNpc(level, floor, "referee", layout.npc());
        LAYOUTS.put(floor, layout);
        return layout;
    }

    /** Arène de débordement au décor de l'étage {@code floor} (sans palier ni arbitre). */
    public static FloorLayout buildOverflow(ServerLevel level, int slot, int floor) {
        return build(level, overflowCenter(slot), floor, ArenaThemes.forFloor(floor), false);
    }

    private static FloorLayout build(ServerLevel level, BlockPos c, int floor, ArenaTheme theme, boolean withLanding) {
        Optional<StructureTemplate> template = theme.structure() == null ? Optional.empty()
                : level.getStructureManager().get(theme.structure());
        if (template.isEmpty()) {
            if (theme.structure() != null) {
                HxHMod.LOGGER.warn("[HxH] Structure {} introuvable pour l'étage {} : décor généré à la place", theme.structure(), floor);
            }
            buildRing(level, c, floor, theme);
            if (withLanding) buildLanding(level, c, theme);
            return generatedLayout(c);
        }
        Placed p = placeStructure(level, template.get(), c, theme.yOffset());
        FloorLayout def = generatedLayout(c);
        BlockPos playerStart = p.first(ModBlocks.MARKER_PLAYER.get(), def.playerStart());
        BlockPos opponentStart = p.first(ModBlocks.MARKER_OPPONENT.get(), def.opponentStart());
        BlockPos arrival = p.first(ModBlocks.MARKER_ARRIVAL.get(), playerStart);
        BlockPos npc = withLanding ? p.first(ModBlocks.MARKER_NPC.get(), null) : null;
        AABB ring = new AABB(playerStart).minmax(new AABB(opponentStart)).inflate(4, 3, 4);
        return new FloorLayout(arrival, yawToward(arrival, playerStart.equals(arrival) ? opponentStart : playerStart),
                playerStart, opponentStart, npc, ring);
    }

    /** Disposition du ring généré : joueur à l'ouest, adversaire à l'est, palier au nord. */
    private static FloorLayout generatedLayout(BlockPos c) {
        BlockPos arrival = c.offset(0, 0, -(LANDING_NEAR + 2));
        return new FloorLayout(arrival, 0f, c.offset(-6, 0, 0), c.offset(6, 0, 0),
                c.offset(3, 0, -LANDING_NEAR - 1),
                new AABB(c).inflate(ARENA_HALF + 0.5, 6, ARENA_HALF + 0.5));
    }

    private static float yawToward(BlockPos from, @Nullable BlockPos to) {
        if (to == null || to.equals(from)) return 0f;
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
    }

    private static void buildLanding(ServerLevel level, BlockPos c, ArenaTheme theme) {
        for (int x = -LANDING_HALF_WIDTH; x <= LANDING_HALF_WIDTH; x++) {
            for (int z = -LANDING_FAR; z <= -LANDING_NEAR; z++) {
                boolean edge = Math.abs(x) == LANDING_HALF_WIDTH || z == -LANDING_FAR || z == -LANDING_NEAR;
                level.setBlock(c.offset(x, -1, z), edge ? theme.border() : theme.floor().get(0), 2);
                level.setBlock(c.offset(x, -2, z), theme.under(), 2);
                for (int y = 0; y <= 3; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        BlockPos elevator = c.offset(-LANDING_HALF_WIDTH, 0, -LANDING_FAR);
        level.setBlock(elevator, ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
        level.setBlock(elevator.above(), ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
    }

    // ---------- Structures ----------

    /** Repères trouvés dans une structure posée (positions dans le monde). */
    private record Placed(Map<Block, List<BlockPos>> markers) {
        BlockPos first(Block marker, @Nullable BlockPos fallback) {
            List<BlockPos> l = markers.get(marker);
            return l == null || l.isEmpty() ? fallback : l.get(0);
        }
    }

    /**
     * Pose une structure centrée (en X/Z) sur {@code center}, sa couche du bas à {@code center.y - 1 + yOffset},
     * puis remplace ses blocs repères par de l'air.
     */
    private static Placed placeStructure(ServerLevel level, StructureTemplate template, BlockPos center, int yOffset) {
        Vec3i size = template.getSize();
        BlockPos origin = center.offset(-size.getX() / 2, -1 + yOffset, -size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        Map<Block, List<BlockPos>> markers = new HashMap<>();
        for (Block marker : List.of(ModBlocks.MARKER_PLAYER.get(), ModBlocks.MARKER_OPPONENT.get(),
                ModBlocks.MARKER_NPC.get(), ModBlocks.MARKER_ARRIVAL.get())) {
            markers.put(marker, template.filterBlocks(origin, settings, marker).stream().map(i -> i.pos()).toList());
        }
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
        for (List<BlockPos> list : markers.values()) {
            for (BlockPos pos : list) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        return new Placed(markers);
    }

    private static void clearBox(ServerLevel level, BlockPos a, BlockPos b) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
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
