package com.evandouard.hxh.exam;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.marker.MarkerBlockEntity;
import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * La dimension de l'Examen Hunter et ses lieux, chacun à 400 blocs du précédent :
 * <ul>
 *     <li>{@code hxh:exam/zaban_restaurant} : la gargote de Zaban, le cuisinier fait descendre la salle ;</li>
 *     <li>{@code hxh:exam/hall_inscription} : le hall souterrain (Beans, Satotz) et le tunnel du marathon ;</li>
 *     <li>{@code hxh:exam/biska} : la réserve de Biska (Buhara, Menchi, porcs à chasser) ;</li>
 *     <li>{@code hxh:exam/hotel} : la salle où Netero remet la licence.</li>
 * </ul>
 * Si Evan a construit le lieu (fichier de structure), on le pose et on lit ses repères : arrivée (E), PNJ (P, réglé
 * sur son identifiant), points de parcours numérotés (le plus grand = arrivée du marathon), apparition de proies (G).
 * Sinon on construit un lieu provisoire. Un lieu n'est construit qu'une fois par monde.
 */
public final class ExamWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, HxHMod.id("hunter_exam"));
    public static final int FLOOR_Y = 64;
    private static final int SPACING = 400;

    /** Ce qu'il y a dans un lieu une fois posé. */
    public record Site(BlockPos arrival, float yaw, Map<String, BlockPos> npcs, List<BlockPos> route, List<BlockPos> prey) {}

    private static final Map<ExamSite, Site> SITES = new EnumMap<>(ExamSite.class);

    private ExamWorld() {}

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIMENSION);
    }

    public static boolean isExamLevel(Level level) {
        return level.dimension().equals(DIMENSION);
    }

    public static BlockPos center(ExamSite site) {
        return new BlockPos(site.ordinal() * SPACING, FLOOR_Y, 0);
    }

    /** Pose le lieu s'il ne l'est pas encore (une fois par monde) et renvoie ce qu'il contient. */
    public static Site ensure(ServerLevel level, ExamSite site) {
        Site cached = SITES.get(site);
        ExamSavedData data = ExamSavedData.get(level);
        if (cached != null && data.built.contains(site.id())) return cached;
        Optional<StructureTemplate> template = level.getStructureManager().get(site.template());
        Site built = template.filter(t -> t.getSize().getX() > 0)
                .map(t -> placeTemplate(level, t, center(site), !data.built.contains(site.id())))
                .orElseGet(() -> ExamPlaceholders.build(level, site, center(site), !data.built.contains(site.id())));
        if (!data.built.contains(site.id())) {
            spawnNpcs(level, built);
            spawnPrey(level, built);
            data.built.add(site.id());
            data.setDirty();
        }
        SITES.put(site, built);
        return built;
    }

    /** Oublie ce qui a été posé (tests, /hunter exam rebuild) : tout sera reposé à la prochaine visite. */
    public static void forget(ServerLevel level) {
        SITES.clear();
        ExamSavedData d = ExamSavedData.get(level);
        d.built.clear();
        d.setDirty();
    }

    private static Site placeTemplate(ServerLevel level, StructureTemplate template, BlockPos center, boolean place) {
        Vec3i size = template.getSize();
        BlockPos origin = center.offset(-size.getX() / 2, -1, -size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        BlockPos arrival = null;
        Map<String, BlockPos> npcs = new LinkedHashMap<>();
        TreeMap<Integer, BlockPos> route = new TreeMap<>();
        List<BlockPos> prey = new ArrayList<>();
        List<BlockPos> toClear = new ArrayList<>();
        for (var info : template.filterBlocks(origin, settings, ModBlocks.MARKER_ARRIVAL.get())) {
            arrival = info.pos();
            toClear.add(info.pos());
        }
        for (var info : template.filterBlocks(origin, settings, ModBlocks.MARKER_NPC_CHOICE.get())) {
            CompoundTag nbt = info.nbt();
            String npc = nbt == null ? "" : nbt.getString("npc");
            if (!npc.isEmpty()) npcs.putIfAbsent(npc, info.pos());
            toClear.add(info.pos());
        }
        for (var info : template.filterBlocks(origin, settings, ModBlocks.MARKER_WAYPOINT.get())) {
            CompoundTag nbt = info.nbt();
            int index = nbt == null || !nbt.contains("index") ? route.size() + 1 : nbt.getInt("index");
            route.put(index, info.pos());
            toClear.add(info.pos());
        }
        for (var info : template.filterBlocks(origin, settings, ModBlocks.MARKER_PREY.get())) {
            prey.add(info.pos());
            toClear.add(info.pos());
        }
        if (place) {
            template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
            for (BlockPos p : toClear) {
                level.removeBlockEntity(p);
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (arrival == null) arrival = center;
        return new Site(arrival, 0f, npcs, List.copyOf(route.values()), prey);
    }

    static void spawnNpcs(ServerLevel level, Site site) {
        for (Map.Entry<String, BlockPos> e : site.npcs().entrySet()) {
            BlockPos pos = e.getValue();
            NpcEntity npc = ModEntities.NPC.get().create(level);
            if (npc == null) continue;
            npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            npc.setNpcId(e.getKey());
            npc.setHome(pos);
            if (e.getKey().equals("satotz") && !site.route().isEmpty()) npc.setRoute(site.route());
            level.addFreshEntity(npc);
        }
    }

    /** Des porcs à chasser sur chaque repère de proie (deux par repère). */
    static void spawnPrey(ServerLevel level, Site site) {
        for (BlockPos p : site.prey()) {
            for (int i = 0; i < 2; i++) {
                Pig pig = EntityType.PIG.create(level);
                if (pig == null) continue;
                pig.moveTo(p.getX() + 0.5 + i, p.getY(), p.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
                pig.setPersistenceRequired();
                level.addFreshEntity(pig);
            }
        }
    }

    /** Remet des porcs si la réserve n'en a presque plus (la chasse doit rester possible pour le suivant). */
    public static void restockPrey(ServerLevel level) {
        Site biska = SITES.get(ExamSite.BISKA);
        if (biska == null || biska.prey().isEmpty()) return;
        BlockPos c = biska.prey().get(0);
        int pigs = level.getEntitiesOfClass(Pig.class, new net.minecraft.world.phys.AABB(c).inflate(48)).size();
        if (pigs < 3) spawnPrey(level, biska);
    }

    static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2);
    }

    static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    static boolean isMarker(BlockState state) {
        return state.getBlock() instanceof com.evandouard.hxh.marker.MarkerBlock;
    }

    /** Pour les messages : le repère PNJ d'une structure, s'il y en a. */
    static String npcOf(MarkerBlockEntity be) {
        return be.npc();
    }
}
