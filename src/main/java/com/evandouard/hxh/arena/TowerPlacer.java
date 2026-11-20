package com.evandouard.hxh.arena;

import com.evandouard.hxh.HxHMod;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * La Tour Céleste est posée par le mod, une seule fois par monde, dans une plaine : on n'attend plus la génération
 * naturelle (qui ratait quand l'emplacement prévu tombait dans un biome interdit). Au démarrage du serveur, si
 * le monde n'a pas encore de tour, on cherche la plaine la plus proche d'un point tiré au hasard à 300–600 blocs
 * du spawn, et on y pose la structure {@code hxh:heavens_arena/tour}. Mondes existants compris.
 */
public final class TowerPlacer {
    public static final ResourceLocation TEMPLATE = HxHMod.id("heavens_arena/tour");
    private static final int MIN_DIST = 300, MAX_DIST = 600, SEARCH_RADIUS = 6400;

    private TowerPlacer() {}

    /** Mémoire du monde : la tour a-t-elle été posée, et où. */
    public static final class Data extends SavedData {
        boolean placed;
        BlockPos pos = BlockPos.ZERO;

        static Data load(CompoundTag tag, HolderLookup.Provider provider) {
            Data d = new Data();
            d.placed = tag.getBoolean("placed");
            d.pos = BlockPos.of(tag.getLong("pos"));
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putBoolean("placed", placed);
            tag.putLong("pos", pos.asLong());
            return tag;
        }

        static Data get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load), "hxh_heavens_tower");
        }
    }

    /** Position de la tour (coin de la structure), ou null si pas encore posée. */
    @Nullable
    public static BlockPos position(MinecraftServer server) {
        Data d = Data.get(server.overworld());
        return d.placed ? d.pos : null;
    }

    /** Centre de la base de la tour, pour y téléporter ou l'indiquer. */
    @Nullable
    public static BlockPos center(MinecraftServer server) {
        BlockPos p = position(server);
        if (p == null) return null;
        StructureTemplate t = server.overworld().getStructureManager().get(TEMPLATE).orElse(null);
        if (t == null) return p;
        return p.offset(t.getSize().getX() / 2, 0, t.getSize().getZ() / 2);
    }

    /** Au démarrage : pose la tour si ce monde n'en a pas encore. */
    public static void ensurePlaced(MinecraftServer server) {
        if (server instanceof net.minecraft.gametest.framework.GameTestServer) return; // pas pendant les GameTests
        ServerLevel level = server.overworld();
        Data data = Data.get(level);
        if (data.placed) return;
        BlockPos spot = findDrySpot(level, findSpot(level, level.getRandom()));
        BlockPos at = place(level, spot);
        if (at == null) return;
        data.placed = true;
        data.pos = at;
        data.setDirty();
        HxHMod.LOGGER.info("[HxH] Tour Céleste posée en {} {} {}", at.getX(), at.getY(), at.getZ());
    }

    /** Plaine la plus proche d'un point tiré au hasard à 300–600 blocs du spawn (sinon ce point lui-même). */
    public static BlockPos findSpot(ServerLevel level, RandomSource random) {
        BlockPos spawn = level.getSharedSpawnPos();
        double angle = random.nextDouble() * Math.PI * 2;
        int dist = MIN_DIST + random.nextInt(MAX_DIST - MIN_DIST);
        BlockPos start = spawn.offset((int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist)).atY(64);
        Pair<BlockPos, Holder<Biome>> plains = level.findClosestBiome3d(h -> h.is(Biomes.PLAINS), start, SEARCH_RADIUS, 32, 64);
        if (plains == null) {
            plains = level.findClosestBiome3d(h -> h.is(com.evandouard.hxh.arena.TowerPlacer.LAND), start, SEARCH_RADIUS, 32, 64);
        }
        return plains != null ? plains.getFirst() : start;
    }

    /** Demi-taille de l'emprise de la tour utilisée pour juger un terrain (en blocs, repli si la structure manque). */
    private static final int FOOTPRINT_HALF = 24;

    /**
     * Le terrain autour de (x, z) est-il de la terre ferme ? Sur toute l'emprise de la tour (grille de sondes), la
     * surface doit être du sol sec (ni eau, ni glace, ni lave, ni arbre qui flotte) au-dessus du niveau de la mer,
     * et le relief ne doit pas varier de plus de 8 blocs.
     */
    public static boolean isDryLand(ServerLevel level, int cx, int cz, int half) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -half; dx <= half; dx += half / 2 == 0 ? half : half / 2) {
            for (int dz = -half; dz <= half; dz += half / 2 == 0 ? half : half / 2) {
                int x = cx + dx, z = cz + dz;
                level.getChunk(x >> 4, z >> 4);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (top != floor) return false; // quelque chose (eau…) recouvre le sol
                BlockState ground = level.getBlockState(new BlockPos(x, top - 1, z));
                if (!ground.getFluidState().isEmpty() || ground.is(Blocks.ICE) || ground.is(Blocks.PACKED_ICE)
                        || ground.is(Blocks.BLUE_ICE) || ground.is(Blocks.MAGMA_BLOCK)) return false;
                if (top <= level.getSeaLevel()) return false; // au niveau de la mer ou sous l'eau
                min = Math.min(min, top);
                max = Math.max(max, top);
            }
        }
        return max - min <= 8;
    }

    /** Premier terrain sec en spirale autour de {@code around} (pas de 16, jusqu'à ~700 blocs) ; sinon le point de départ. */
    public static BlockPos findDrySpot(ServerLevel level, BlockPos around) {
        if (isDryLand(level, around.getX(), around.getZ(), FOOTPRINT_HALF)) return around;
        for (int ring = 1; ring <= 44; ring++) {
            int r = ring * 16;
            for (int i = 0; i < ring * 8; i++) {
                double a = Math.PI * 2 * i / (ring * 8);
                int x = around.getX() + (int) Math.round(Math.cos(a) * r);
                int z = around.getZ() + (int) Math.round(Math.sin(a) * r);
                if (isDryLand(level, x, z, FOOTPRINT_HALF)) return new BlockPos(x, around.getY(), z);
            }
        }
        HxHMod.LOGGER.warn("[HxH] Aucune terre ferme trouvée autour de {} : tour posée au point d'origine", around);
        return around;
    }

    /** Biomes de repli si aucune plaine n'existe (monde très particulier). */
    static final net.minecraft.tags.TagKey<Biome> LAND = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BIOME, HxHMod.id("has_heavens_arena"));

    /**
     * Pose la tour centrée sur {@code center} (x, z), posée sur le sol, et comble le vide sous sa base.
     * @return le coin de la structure, ou null si la structure est introuvable
     */
    @Nullable
    public static BlockPos place(ServerLevel level, BlockPos center) {
        StructureTemplate template = level.getStructureManager().get(TEMPLATE).orElse(null);
        if (template == null) {
            HxHMod.LOGGER.error("[HxH] Structure {} introuvable : tour non posée", TEMPLATE);
            return null;
        }
        int sx = template.getSize().getX(), sz = template.getSize().getZ();
        int x0 = center.getX() - sx / 2, z0 = center.getZ() - sz / 2;
        // Charge (et génère) les tronçons sous la tour, puis prend le sol le plus bas des coins et du centre
        for (int cx = (x0 >> 4) - 1; cx <= ((x0 + sx) >> 4) + 1; cx++) {
            for (int cz = (z0 >> 4) - 1; cz <= ((z0 + sz) >> 4) + 1; cz++) level.getChunk(cx, cz);
        }
        int ground = Integer.MAX_VALUE;
        int[][] probes = {{0, 0}, {sx - 1, 0}, {0, sz - 1}, {sx - 1, sz - 1}, {sx / 2, sz / 2}};
        for (int[] p : probes) {
            ground = Math.min(ground, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x0 + p[0], z0 + p[1]));
        }
        BlockPos corner = new BlockPos(x0, ground - 2, z0);
        template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        // Comble le vide sous la base (terrain en pente)
        for (int dx = 0; dx < sx; dx++) {
            for (int dz = 0; dz < sz; dz++) {
                BlockPos base = corner.offset(dx, 0, dz);
                if (level.getBlockState(base).isAir()) continue;
                BlockPos.MutableBlockPos m = base.mutable().move(0, -1, 0);
                for (int i = 0; i < 24 && canFill(level.getBlockState(m)); i++, m.move(0, -1, 0)) {
                    level.setBlock(m, Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return corner;
    }

    private static boolean canFill(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty();
    }
}
