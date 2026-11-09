package com.evandouard.hxh.arena;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * La dimension de la Tour Céleste et ce qu'on y construit :
 * <ul>
 *     <li><b>hall d'entrée</b> (lieu 0) : où l'on arrive, hôtesse d'inscription, ascenseur, sortie ;
 *         structure {@code hxh:heavens_arena/lobby} si elle existe ;</li>
 *     <li><b>un grand hall par tranche d'étages</b> (lieu = n° de tranche + 1) : ring central visible de tous,
 *         hôtesse, arbitre ; structure indiquée par la tranche si elle existe, sinon hall généré ;</li>
 *     <li><b>une chambre par joueur</b> : structure {@code hxh:heavens_arena/room} si elle existe.</li>
 * </ul>
 * Dans une structure, des blocs repères disent où placer les choses ; ils sont retirés une fois la structure posée.
 * Un lieu n'est reconstruit que si sa définition change (ou par {@code /hunter arena rebuild}).
 */
public final class ArenaWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, HxHMod.id("heavens_arena"));
    public static final ResourceLocation LOBBY_STRUCTURE = HxHMod.id("heavens_arena/lobby");
    public static final ResourceLocation ROOM_STRUCTURE = HxHMod.id("heavens_arena/room");

    /** On marche à Y = 64. */
    public static final int FLOOR_Y = 64;
    public static final int ENTRY = 0;
    public static final BlockPos LOBBY_SPAWN = new BlockPos(0, FLOOR_Y, 5);
    public static final BlockPos RECEPTIONIST = new BlockPos(0, FLOOR_Y, -8);
    public static final BlockPos ELEVATOR = new BlockPos(10, FLOOR_Y, 0);
    public static final BlockPos EXIT = new BlockPos(-10, FLOOR_Y, 0);
    private static final int LOBBY_HALF = 10;
    private static final BlockPos LOBBY_MARKER = new BlockPos(-LOBBY_HALF, FLOOR_Y - 1, -LOBBY_HALF);
    private static final int LOBBY_HEIGHT = 7;

    /** Tomber de plus de 6 blocs sous son point de départ = sortie de ring. */
    public static final int RING_OUT_DROP = 6;

    /** Halls de tranche alignés le long de Z ; un hall construit peut faire jusqu'à ~300 × 300 blocs. */
    private static final int HALLS_X = 5000;
    private static final int HALL_SPACING = 320;
    /** Hall généré : 45 × 45. */
    private static final int GEN_HALF = 22;

    /** Chambres alignées le long de Z, de l'autre côté. */
    private static final int ROOMS_X = -5000;
    private static final int ROOM_SPACING = 64;
    /** Zone de chaque chambre où son propriétaire peut construire. */
    public static final int ROOM_BUILD_RADIUS = 24;

    /**
     * Ce qu'on trouve dans un lieu une fois construit.
     * @param ringCenter centre du ring (sortir du carré de demi-côté ringRadius = sortie de ring)
     */
    public record Layout(BlockPos arrival, float arrivalYaw, BlockPos playerStart, BlockPos opponentStart,
                         BlockPos ringCenter, int ringRadius,
                         List<BlockPos> receptionists, List<BlockPos> referees) {
        @Nullable
        public BlockPos receptionist() {
            return receptionists.isEmpty() ? null : receptionists.get(0);
        }

        @Nullable
        public BlockPos referee() {
            return referees.isEmpty() ? null : referees.get(0);
        }

        public int ringOutY() {
            return Math.min(playerStart.getY(), opponentStart.getY()) - RING_OUT_DROP;
        }

        public boolean inRing(double x, double z) {
            double cx = ringCenter.getX() + 0.5, cz = ringCenter.getZ() + 0.5;
            return Math.abs(x - cx) <= ringRadius + 0.5 && Math.abs(z - cz) <= ringRadius + 0.5;
        }
    }

    private static final Map<Integer, Layout> LAYOUTS = new HashMap<>();
    private static final Map<UUID, BlockPos> ROOM_ARRIVALS = new HashMap<>();

    private ArenaWorld() {}

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIMENSION);
    }

    public static boolean isArenaLevel(Level level) {
        return level.dimension() == DIMENSION;
    }

    // ---------- Lieux ----------

    public static int locationOfBracket(int bracketIndex) {
        return bracketIndex + 1;
    }

    public static BlockPos hallCenter(int bracketIndex) {
        return new BlockPos(HALLS_X, FLOOR_Y, locationOfBracket(bracketIndex) * HALL_SPACING);
    }

    /** Lieu d'une position : 0 = hall d'entrée, n = hall de la tranche n − 1, −1 ailleurs (chambres…). */
    public static int locationAt(BlockPos pos) {
        if (Math.abs(pos.getX() - HALLS_X) <= HALL_SPACING / 2) {
            int loc = Math.round(pos.getZ() / (float) HALL_SPACING);
            return loc >= 1 ? loc : -1;
        }
        if (Math.abs(pos.getX()) <= 150 && Math.abs(pos.getZ()) <= 150) return ENTRY;
        return -1;
    }

    public static boolean inRoomZone(double x) {
        return Math.abs(x - ROOMS_X) <= 200;
    }

    /** Disposition connue d'un lieu (celle du hall généré si on ne l'a pas encore construit). */
    public static Layout layout(int location) {
        Layout l = LAYOUTS.get(location);
        if (l != null) return l;
        return location == ENTRY ? entryLayout() : generatedLayout(hallCenter(location - 1), 9);
    }

    /** Force la reconstruction de tous les lieux (après avoir modifié une structure). */
    public static void forgetBuilds(ServerLevel level) {
        ArenaSavedData d = ArenaSavedData.get(level);
        d.signatures.clear();
        d.setDirty();
    }

    // ---------- Hall d'entrée (lieu 0) ----------

    public static Layout ensureLobby(ServerLevel level) {
        ArenaSavedData data = ArenaSavedData.get(level);
        Optional<StructureTemplate> template = level.getStructureManager().get(LOBBY_STRUCTURE);
        String signature = template.map(t -> "structure:" + t.getSize()).orElse("generated");
        boolean rebuild = !signature.equals(data.signatures.get(ENTRY)) || !LAYOUTS.containsKey(ENTRY);
        if (!rebuild) return LAYOUTS.get(ENTRY);
        Layout layout;
        if (template.isPresent()) {
            if (!data.signatures.containsKey(ENTRY) || "generated".equals(data.signatures.get(ENTRY))) {
                clearBox(level, new BlockPos(-LOBBY_HALF - 1, FLOOR_Y - 2, -LOBBY_HALF - 1),
                        new BlockPos(LOBBY_HALF + 1, FLOOR_Y + LOBBY_HEIGHT + 2, LOBBY_HALF + 1));
            }
            boolean place = !signature.equals(data.signatures.get(ENTRY));
            Placed p = placeStructure(level, template.get(), new BlockPos(0, FLOOR_Y, 0), 0, place);
            BlockPos arrival = p.first(ModBlocks.MARKER_ARRIVAL.get(), new BlockPos(0, FLOOR_Y, 0));
            List<BlockPos> recs = p.all(ModBlocks.MARKER_RECEPTIONIST.get());
            layout = new Layout(arrival, yawToward(arrival, recs.isEmpty() ? null : recs.get(0)), arrival, arrival, arrival, 0,
                    recs, List.of());
        } else {
            boolean legacy = !data.lobbyBuilt && level.getBlockState(LOBBY_MARKER).is(Blocks.POLISHED_DEEPSLATE);
            if (!legacy && !signature.equals(data.signatures.get(ENTRY)) && !data.lobbyBuilt) buildEntryHall(level);
            layout = entryLayout();
        }
        data.lobbyBuilt = true;
        data.signatures.put(ENTRY, signature);
        data.setDirty();
        ensureNpcs(level, "receptionist", ENTRY, layout.receptionists());
        // Wing, le maître de Nen (comme dans l'anime, il enseigne à la Tour Céleste) : à côté de l'hôtesse
        if (!layout.receptionists().isEmpty()) {
            BlockPos spot = standSpot(level, layout.receptionists().get(0));
            if (spot != null) ensureNpcs(level, "wing", ENTRY, List.of(spot));
            // Le marchand (gamme III, rachat des butins) : une autre place libre près de l'hôtesse
            BlockPos shop = standSpotExcept(level, layout.receptionists().get(0), spot);
            if (shop != null) ensureNpcs(level, "merchant", ENTRY, List.of(shop));
        }
        LAYOUTS.put(ENTRY, layout);
        return layout;
    }

    private static Layout entryLayout() {
        return new Layout(LOBBY_SPAWN, 180f, LOBBY_SPAWN, LOBBY_SPAWN, LOBBY_SPAWN, 0, List.of(RECEPTIONIST), List.of());
    }

    private static void buildEntryHall(ServerLevel level) {
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

    // ---------- Halls de tranche ----------

    /**
     * Prépare le hall d'une tranche (construit seulement si nouveau ou modifié) et pose son hôtesse et son arbitre.
     */
    public static Layout ensureHall(ServerLevel level, ArenaBracket bracket) {
        int loc = locationOfBracket(bracket.index());
        ArenaSavedData data = ArenaSavedData.get(level);
        Optional<StructureTemplate> template = bracket.structure() == null ? Optional.empty()
                : level.getStructureManager().get(bracket.structure());
        if (bracket.structure() != null && template.isEmpty()) {
            HxHMod.LOGGER.warn("[HxH] Structure {} introuvable pour la tranche {} : hall généré à la place",
                    bracket.structure(), bracket.label());
        }
        String signature = bracket.signature() + template.map(t -> ":" + t.getSize()).orElse(":gen");
        boolean place = !signature.equals(data.signatures.get(loc));
        if (!place && LAYOUTS.containsKey(loc)) return LAYOUTS.get(loc);
        BlockPos c = hallCenter(bracket.index());
        Layout layout;
        if (template.isPresent()) {
            if (place) clearBox(level, c.offset(-GEN_HALF - 1, -3, -GEN_HALF - 1), c.offset(GEN_HALF + 1, 8, GEN_HALF + 1));
            Placed p = placeStructure(level, template.get(), c, bracket.yOffset(), place);
            Layout def = generatedLayout(c, bracket.ringRadius());
            BlockPos ps = p.first(ModBlocks.MARKER_PLAYER.get(), def.playerStart());
            BlockPos os = p.first(ModBlocks.MARKER_OPPONENT.get(), def.opponentStart());
            BlockPos arrival = p.first(ModBlocks.MARKER_ARRIVAL.get(), def.arrival());
            BlockPos ringCenter = new BlockPos((ps.getX() + os.getX()) / 2, ps.getY(), (ps.getZ() + os.getZ()) / 2);
            layout = new Layout(arrival, yawToward(arrival, ringCenter), ps, os, ringCenter, bracket.ringRadius(),
                    p.all(ModBlocks.MARKER_RECEPTIONIST.get()),
                    p.all(ModBlocks.MARKER_REFEREE.get(), ModBlocks.MARKER_NPC.get()));
        } else {
            if (place) buildGeneratedHall(level, c, bracket);
            layout = generatedLayout(c, bracket.ringRadius());
        }
        data.signatures.put(loc, signature);
        data.setDirty();
        ensureNpcs(level, "receptionist", loc, layout.receptionists());
        ensureNpcs(level, "referee", loc, layout.referees());
        LAYOUTS.put(loc, layout);
        return layout;
    }

    /** Disposition du hall généré : ring au centre, arrivée et guichet au nord, arbitre au nord-ouest. */
    private static Layout generatedLayout(BlockPos c, int ringRadius) {
        BlockPos arrival = c.offset(0, 0, -GEN_HALF + 3);
        return new Layout(arrival, 0f, c.offset(-6, 0, 0), c.offset(6, 0, 0), c, ringRadius,
                List.of(c.offset(8, 0, -GEN_HALF + 2)), List.of(c.offset(-ringRadius - 3, 0, -ringRadius - 3)));
    }

    private static void buildGeneratedHall(ServerLevel level, BlockPos c, ArenaBracket b) {
        int r = b.ringRadius();
        for (int x = -GEN_HALF; x <= GEN_HALF; x++) {
            for (int z = -GEN_HALF; z <= GEN_HALF; z++) {
                boolean hallEdge = Math.abs(x) == GEN_HALF || Math.abs(z) == GEN_HALF;
                boolean inRing = Math.abs(x) <= r && Math.abs(z) <= r;
                boolean ringEdge = inRing && (Math.abs(x) == r || Math.abs(z) == r);
                int d2 = x * x + z * z;
                boolean accent = inRing && ((x == 0 && Math.abs(z) < r) || (d2 >= 6 && d2 <= 10));
                BlockState s = !inRing ? b.lobbyFloor() : ringEdge ? b.border() : accent ? b.accent()
                        : b.floor().get(b.floorIndex(x, z));
                level.setBlock(c.offset(x, -1, z), s, 2);
                level.setBlock(c.offset(x, -2, z), b.under(), 2);
                for (int y = 0; y <= 8; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                // Garde-corps autour du hall
                if (hallEdge) level.setBlock(c.offset(x, 0, z), b.border(), 2);
            }
        }
        // Piliers et lumières aux coins du ring (côté spectateurs)
        if (!b.pillar().isAir() && b.pillarHeight() > 0) {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    BlockPos base = c.offset(sx * (r + 2), 0, sz * (r + 2));
                    for (int y = 0; y < b.pillarHeight(); y++) level.setBlock(base.above(y), b.pillar(), 2);
                    if (!b.light().isAir()) level.setBlock(base.above(b.pillarHeight()), b.light(), 2);
                }
            }
        }
        // Guichet : comptoir devant l'hôtesse
        for (int x = 5; x <= 11; x++) level.setBlock(c.offset(x, 0, -GEN_HALF + 3), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
        // Ascenseur
        BlockPos elevator = c.offset(-3, 0, -GEN_HALF + 1);
        level.setBlock(elevator, ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
        level.setBlock(elevator.above(), ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
    }

    // ---------- Chambres ----------

    /** Prépare la chambre d'un joueur (construite une seule fois : il peut la décorer) et renvoie son entrée. */
    public static BlockPos ensureRoom(ServerLevel level, UUID player) {
        ArenaSavedData data = ArenaSavedData.get(level);
        Integer slot = data.roomSlots.get(player);
        boolean build = slot == null;
        if (build) {
            slot = data.roomSlots.size();
            data.roomSlots.put(player, slot);
            data.setDirty();
        }
        BlockPos c = roomCenter(slot);
        if (!build) {
            BlockPos known = ROOM_ARRIVALS.get(player);
            if (known != null) return known;
            if (data.roomArrivals.containsKey(player)) return BlockPos.of(data.roomArrivals.get(player));
        }
        BlockPos arrival;
        Optional<StructureTemplate> template = level.getStructureManager().get(ROOM_STRUCTURE);
        if (template.isPresent()) {
            Placed p = placeStructure(level, template.get(), c, 0, true);
            arrival = p.first(ModBlocks.MARKER_ARRIVAL.get(), c);
        } else {
            buildGeneratedRoom(level, c);
            arrival = c.offset(0, 0, 2);
        }
        data.roomArrivals.put(player, arrival.asLong());
        data.setDirty();
        ROOM_ARRIVALS.put(player, arrival);
        return arrival;
    }

    public static BlockPos roomCenter(int slot) {
        return new BlockPos(ROOMS_X, FLOOR_Y, slot * ROOM_SPACING);
    }

    /** Le joueur peut-il modifier ce bloc (dans sa propre chambre) ? */
    public static boolean inOwnRoom(ServerLevel level, UUID player, BlockPos pos) {
        Integer slot = ArenaSavedData.get(level).roomSlots.get(player);
        if (slot == null) return false;
        BlockPos c = roomCenter(slot);
        return Math.abs(pos.getX() - c.getX()) <= ROOM_BUILD_RADIUS && Math.abs(pos.getZ() - c.getZ()) <= ROOM_BUILD_RADIUS
                && pos.getY() >= FLOOR_Y - 8 && pos.getY() <= FLOOR_Y + 24;
    }

    private static void buildGeneratedRoom(ServerLevel level, BlockPos c) {
        int half = 5, height = 5;
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                boolean edge = Math.abs(x) == half || Math.abs(z) == half;
                level.setBlock(c.offset(x, -1, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                level.setBlock(c.offset(x, height, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
                for (int y = 0; y < height; y++) {
                    BlockState s = !edge ? Blocks.AIR.defaultBlockState()
                            : (y >= 1 && y <= 3 && z == -half && Math.abs(x) <= 3) ? Blocks.GLASS.defaultBlockState()
                            : Blocks.QUARTZ_BRICKS.defaultBlockState();
                    level.setBlock(c.offset(x, y, z), s, 2);
                }
            }
        }
        level.setBlock(c.offset(0, height, 0), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        // Tapis, lit, coffre, ascenseur
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            level.setBlock(c.offset(x, 0, z), Blocks.RED_CARPET.defaultBlockState(), 2);
        BlockState foot = Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT);
        level.setBlock(c.offset(-4, 0, -3), foot, 2);
        level.setBlock(c.offset(-4, 0, -4), foot.setValue(BedBlock.PART, BedPart.HEAD), 2);
        level.setBlock(c.offset(4, 0, -4), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 2);
        level.setBlock(c.offset(3, 0, -4), Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        BlockPos elevator = c.offset(0, 0, half - 1);
        level.setBlock(elevator, ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
        level.setBlock(elevator.above(), ModBlocks.ARENA_ELEVATOR.get().defaultBlockState(), 2);
    }

    // ---------- PNJ fixes (hôtesses, arbitres) ----------

    /**
     * Pose les PNJ d'un rôle dans un lieu (une hôtesse ou un arbitre par repère), seulement à des places pas encore
     * enregistrées. On ne cherche pas les entités (elles ne sont peut-être pas encore chargées) : on se fie au registre.
     */
    /** Case libre (deux blocs d'air sur un sol) à 2–6 blocs de {@code base}, ou null. */
    static BlockPos standSpot(ServerLevel level, BlockPos base) {
        for (int r = 3; r <= 6; r++) {
            for (int[] d : new int[][]{{r, 0}, {-r, 0}, {0, r}, {0, -r}, {r, r}, {-r, r}, {r, -r}, {-r, -r}}) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = base.offset(d[0], dy, d[1]);
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                            && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) return p;
                }
            }
        }
        return null;
    }

    /** Comme {@link #standSpot}, mais pas sur {@code taken} ni juste à côté. */
    static BlockPos standSpotExcept(ServerLevel level, BlockPos base, BlockPos taken) {
        for (int r = 3; r <= 7; r++) {
            for (int[] d : new int[][]{{-r, 0}, {0, -r}, {r, 0}, {0, r}, {-r, -r}, {r, -r}, {-r, r}, {r, r}}) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = base.offset(d[0], dy, d[1]);
                    if (taken != null && p.distManhattan(taken) < 3) continue;
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                            && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) return p;
                }
            }
        }
        return null;
    }

    static void ensureNpcs(ServerLevel level, String id, int location, List<BlockPos> places) {
        ArenaSavedData data = ArenaSavedData.get(level);
        String prefix = id + "@" + location + "#";
        String legacy = id + "@" + location;
        java.util.Set<Long> registered = new java.util.HashSet<>();
        data.npcHomes.entrySet().removeIf(e -> {
            boolean mine = e.getKey().startsWith(prefix) || e.getKey().equals(legacy);
            if (mine) registered.add(e.getValue());
            return mine;
        });
        java.util.Set<Long> wanted = new java.util.HashSet<>();
        for (int i = 0; i < places.size(); i++) {
            BlockPos p = places.get(i);
            wanted.add(p.asLong());
            data.npcHomes.put(prefix + i, p.asLong());
            if (!registered.contains(p.asLong())) spawnNpc(level, id, p);
        }
        data.setDirty();
        for (NpcEntity old : level.getEntities(ModEntities.NPC.get(), n -> n.npcId().equals(id)
                && locationAt(n.home()) == location && !wanted.contains(n.home().asLong()))) old.discard();
    }

    private static void spawnNpc(ServerLevel level, String id, BlockPos pos) {
        NpcEntity npc = ModEntities.NPC.get().create(level);
        if (npc == null) return;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        npc.setNpcId(id);
        npc.setHome(pos);
        level.addFreshEntity(npc);
    }

    /** Supprime les PNJ en double (même rôle, même place). */
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

    /** Ménage : doublons, et hôtesses ou arbitres qui ne sont plus à la place enregistrée (anciens étages…). */
    public static void cleanNpcs(ServerLevel level) {
        removeDuplicateNpcs(level);
        ArenaSavedData data = ArenaSavedData.get(level);
        for (NpcEntity npc : level.getEntities(ModEntities.NPC.get(),
                n -> n.npcId().equals("receptionist") || n.npcId().equals("referee"))) {
            String prefix = npc.npcId() + "@" + locationAt(npc.home()) + "#";
            long home = npc.home().asLong();
            boolean registered = data.npcHomes.entrySet().stream()
                    .anyMatch(e -> e.getKey().startsWith(prefix) && e.getValue() == home);
            if (!registered) npc.discard();
        }
    }

    // ---------- Structures ----------

    private record Placed(Map<Block, List<BlockPos>> markers) {
        List<BlockPos> all(Block... kinds) {
            List<BlockPos> out = new java.util.ArrayList<>();
            for (Block k : kinds) out.addAll(markers.getOrDefault(k, List.of()));
            return out;
        }

        BlockPos first(Block marker, @Nullable BlockPos fallback) {
            List<BlockPos> l = markers.get(marker);
            return l == null || l.isEmpty() ? fallback : l.get(0);
        }
    }

    /**
     * Pose (si {@code place}) une structure centrée en X/Z sur {@code center}, sa couche du bas à
     * {@code center.y - 1 + yOffset}, et renvoie la position de ses repères (remplacés par de l'air).
     */
    private static Placed placeStructure(ServerLevel level, StructureTemplate template, BlockPos center, int yOffset, boolean place) {
        Vec3i size = template.getSize();
        BlockPos origin = center.offset(-size.getX() / 2, -1 + yOffset, -size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        Map<Block, List<BlockPos>> markers = new HashMap<>();
        for (Block marker : List.of(ModBlocks.MARKER_PLAYER.get(), ModBlocks.MARKER_OPPONENT.get(),
                ModBlocks.MARKER_RECEPTIONIST.get(), ModBlocks.MARKER_REFEREE.get(), ModBlocks.MARKER_NPC.get(),
                ModBlocks.MARKER_ARRIVAL.get())) {
            markers.put(marker, template.filterBlocks(origin, settings, marker).stream().map(i -> i.pos()).toList());
        }
        if (place) {
            template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
            for (List<BlockPos> list : markers.values()) {
                for (BlockPos pos : list) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        return new Placed(markers);
    }

    private static float yawToward(BlockPos from, @Nullable BlockPos to) {
        if (to == null || to.equals(from)) return 0f;
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
    }

    private static void clearBox(ServerLevel level, BlockPos a, BlockPos b) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
