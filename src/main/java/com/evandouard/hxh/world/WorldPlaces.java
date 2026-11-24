package com.evandouard.hxh.world;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.airship.Airship;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Lieux du monde HxH décrits en données ({@code data/hxh/world_places/*.json}) : village, bâtiment… posés une seule fois
 * quand leur zone se charge. Coordonnées relatives à l'ancre (x, z) ; y = 0 est la première couche d'air au-dessus du sol.
 * <pre>
 * { "x": 4350, "z": 4136,
 *   "clear": [x1,y1,z1,x2,y2,z2],                       // dégagé (air) avant de bâtir
 *   "foundation": [x1,z1,x2,z2,"minecraft:cobblestone"], // colonnes remplies sous le sol
 *   "fills": [ { "box": [..6..], "block": "minecraft:oak_planks", "mode": "solid" | "walls" } ],
 *   "blocks": [ { "pos": [x,y,z], "state": "minecraft:oak_door[facing=north,half=lower]" } ] }
 * </pre>
 */
public final class WorldPlaces extends SimpleJsonResourceReloadListener {
    public record Place(ResourceLocation id, int x, int z, JsonObject json) {}

    private static volatile Map<ResourceLocation, Place> PLACES = Map.of();

    private WorldPlaces() {
        super(new Gson(), "world_places");
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new WorldPlaces());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Place> loaded = new TreeMap<>();
        files.forEach((id, el) -> {
            try {
                JsonObject o = el.getAsJsonObject();
                loaded.put(id, new Place(id, o.get("x").getAsInt(), o.get("z").getAsInt(), o));
            } catch (Exception e) {
                HxHMod.LOGGER.error("Lieu {} invalide : {}", id, e.getMessage());
            }
        });
        PLACES = Map.copyOf(loaded);
        HxHMod.LOGGER.info("{} lieu(x) du monde HxH chargé(s)", loaded.size());
    }

    public static Place get(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : PLACES.get(rl);
    }

    /** Quand un tronçon du monde HxH se charge : si un lieu s'y trouve et n'est pas encore posé, on le bâtit au prochain tick. */
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Airship.WORLD)) return;
        int cx = event.getChunk().getPos().x, cz = event.getChunk().getPos().z;
        for (Place p : PLACES.values()) {
            if ((p.x() >> 4) != cx || (p.z() >> 4) != cz) continue;
            Placed placed = Placed.get(level);
            if (!placed.add(p.id().toString())) continue;
            PENDING.add(new Pending(level, p));
        }
    }

    /** Lieux en attente : on force le chargement de leur zone, puis on bâtit quand tout est là (jamais d'attente bloquante dans un tick). */
    private static final java.util.List<Pending> PENDING = new java.util.ArrayList<>();

    private static final class Pending {
        final ServerLevel level;
        final Place place;
        final java.util.List<net.minecraft.world.level.ChunkPos> chunks = new java.util.ArrayList<>();
        int waited;
        boolean requested;

        Pending(ServerLevel level, Place place) {
            this.level = level;
            this.place = place;
            int[] c = place.json().has("clear") ? ints(place.json().getAsJsonArray("clear")) : new int[]{-16, 0, -16, 16, 0, 16};
            for (int cx = (place.x() + Math.min(c[0], c[3])) >> 4; cx <= (place.x() + Math.max(c[0], c[3])) >> 4; cx++)
                for (int cz = (place.z() + Math.min(c[2], c[5])) >> 4; cz <= (place.z() + Math.max(c[2], c[5])) >> 4; cz++)
                    chunks.add(new net.minecraft.world.level.ChunkPos(cx, cz));
        }
    }

    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        for (java.util.Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (!p.requested) {
                for (var c : p.chunks) p.level.setChunkForced(c.x, c.z, true);
                p.requested = true;
            }
            boolean ready = p.chunks.stream().allMatch(c -> p.level.getChunkSource().getChunkNow(c.x, c.z) != null);
            if (ready || ++p.waited > 2400) {
                if (ready) build(p.level, p.place, p.place.x(), p.place.z());
                else HxHMod.LOGGER.warn("Lieu {} : zone non chargée à temps, abandonné", p.place.id());
                for (var c : p.chunks) p.level.setChunkForced(c.x, c.z, false);
                it.remove();
            }
        }
    }

    /** Bâtit le lieu avec son ancre en (ax, az). @return la hauteur du sol (y = 0 relatif) */
    public static int build(ServerLevel level, Place p, int ax, int az) {
        level.getChunk(ax >> 4, az >> 4);
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ax, az);
        gy = Math.max(gy, level.getSeaLevel() + 1);
        JsonObject j = p.json();
        HolderLookup<Block> blocks = level.holderLookup(Registries.BLOCK);
        if (j.has("clear")) box(level, ax, gy, az, ints(j.getAsJsonArray("clear")), Blocks.AIR.defaultBlockState(), false);
        if (j.has("foundation")) {
            JsonArray f = j.getAsJsonArray("foundation");
            BlockState state = parse(blocks, f.get(4).getAsString());
            box(level, ax, gy, az, new int[]{f.get(0).getAsInt(), -6, f.get(1).getAsInt(), f.get(2).getAsInt(), -1, f.get(3).getAsInt()}, state, false);
        }
        if (j.has("fills")) for (JsonElement e : j.getAsJsonArray("fills")) {
            JsonObject o = e.getAsJsonObject();
            boolean walls = o.has("mode") && o.get("mode").getAsString().equals("walls");
            box(level, ax, gy, az, ints(o.getAsJsonArray("box")), parse(blocks, o.get("block").getAsString()), walls);
        }
        if (j.has("blocks")) for (JsonElement e : j.getAsJsonArray("blocks")) {
            JsonObject o = e.getAsJsonObject();
            int[] pos = ints(o.getAsJsonArray("pos"));
            level.setBlock(new BlockPos(ax + pos[0], gy + pos[1], az + pos[2]), parse(blocks, o.get("state").getAsString()), Block.UPDATE_CLIENTS);
        }
        if (j.has("entities")) for (JsonElement e : j.getAsJsonArray("entities")) {
            JsonObject o = e.getAsJsonObject();
            try {
                CompoundTag tag = o.has("nbt") ? net.minecraft.nbt.TagParser.parseTag(o.get("nbt").getAsString()) : new CompoundTag();
                tag.putString("id", o.get("type").getAsString());
                int[] pos = ints(o.getAsJsonArray("pos"));
                float yaw = o.has("yaw") ? o.get("yaw").getAsFloat() : 0f;
                double px = ax + pos[0] + 0.5, py = gy + pos[1], pz = az + pos[2] + 0.5;
                net.minecraft.world.entity.Entity ent = net.minecraft.world.entity.EntityType.loadEntityRecursive(tag, level, en -> {
                    en.moveTo(px, py, pz, yaw, 0f);
                    return en;
                });
                if (ent != null) level.addFreshEntityWithPassengers(ent);
                else HxHMod.LOGGER.warn("Lieu {} : entité inconnue {}", p.id(), o.get("type").getAsString());
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
                HxHMod.LOGGER.warn("Lieu {} : NBT invalide : {}", p.id(), ex.getMessage());
            }
        }
        return gy;
    }

    /**
     * Exporte une construction (boîte entre deux coins) en fichier de lieu : y = 0 est la couche du coin le plus bas.
     * Le fichier est écrit dans {@code hxh_exports/<nom>.json} (dossier du serveur) ; à copier dans {@code data/hxh/world_places/}
     * après avoir réglé « x » et « z » (là où le lieu doit apparaître dans le monde HxH).
     * @return le nombre de blocs exportés
     */
    public static int export(ServerLevel level, String name, BlockPos a, BlockPos b, java.nio.file.Path dir) throws java.io.IOException {
        int x0 = Math.min(a.getX(), b.getX()), y0 = Math.min(a.getY(), b.getY()), z0 = Math.min(a.getZ(), b.getZ());
        int x1 = Math.max(a.getX(), b.getX()), y1 = Math.max(a.getY(), b.getY()), z1 = Math.max(a.getZ(), b.getZ());
        JsonObject out = new JsonObject();
        out.addProperty("x", x0);
        out.addProperty("z", z0);
        JsonArray clear = new JsonArray();
        for (int v : new int[]{0, 0, 0, x1 - x0, y1 - y0, z1 - z0}) clear.add(v);
        out.add("clear", clear);
        JsonArray blocks = new JsonArray();
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            BlockState st = level.getBlockState(p);
            if (st.isAir()) continue;
            JsonObject o = new JsonObject();
            JsonArray pos = new JsonArray();
            pos.add(p.getX() - x0); pos.add(p.getY() - y0); pos.add(p.getZ() - z0);
            o.add("pos", pos);
            o.addProperty("state", BlockStateParser.serialize(st));
            blocks.add(o);
            count++;
        }
        out.add("blocks", blocks);
        JsonArray entities = new JsonArray();
        for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1), en -> !(en instanceof net.minecraft.world.entity.player.Player))) {
            CompoundTag tag = new CompoundTag();
            e.saveWithoutId(tag);
            for (String k : new String[]{"UUID", "Pos", "Motion", "Rotation", "Air", "FallDistance", "Fire", "OnGround", "PortalCooldown", "Brain"}) tag.remove(k);
            JsonObject o = new JsonObject();
            o.addProperty("type", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
            JsonArray pos = new JsonArray();
            pos.add((int) Math.floor(e.getX()) - x0); pos.add((int) Math.floor(e.getY()) - y0); pos.add((int) Math.floor(e.getZ()) - z0);
            o.add("pos", pos);
            o.addProperty("yaw", e.getYRot());
            o.addProperty("nbt", tag.toString());
            entities.add(o);
        }
        out.add("entities", entities);
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.writeString(dir.resolve(name + ".json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out));
        return count;
    }

    private static int[] ints(JsonArray a) {
        int[] r = new int[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsInt();
        return r;
    }

    private static BlockState parse(HolderLookup<Block> blocks, String s) {
        try {
            return BlockStateParser.parseForBlock(blocks, s, false).blockState();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
            throw new IllegalStateException("Bloc invalide « " + s + " » : " + ex.getMessage());
        }
    }

    /** Remplit une boîte [x1,y1,z1,x2,y2,z2] relative à l'ancre ; en mode « walls », seulement le contour vertical. */
    private static void box(ServerLevel level, int ax, int gy, int az, int[] b, BlockState state, boolean walls) {
        for (int x = Math.min(b[0], b[3]); x <= Math.max(b[0], b[3]); x++)
            for (int y = Math.min(b[1], b[4]); y <= Math.max(b[1], b[4]); y++)
                for (int z = Math.min(b[2], b[5]); z <= Math.max(b[2], b[5]); z++) {
                    if (walls && x != Math.min(b[0], b[3]) && x != Math.max(b[0], b[3]) && z != Math.min(b[2], b[5]) && z != Math.max(b[2], b[5])) continue;
                    level.setBlock(new BlockPos(ax + x, gy + y, az + z), state, Block.UPDATE_CLIENTS);
                }
    }

    /** Lieux déjà posés dans ce monde (pour ne bâtir qu'une fois). */
    private static final class Placed extends SavedData {
        private final Set<String> ids = new HashSet<>();

        static Placed get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Placed::new, Placed::load, null), "hxh_world_places");
        }

        static Placed load(CompoundTag tag, HolderLookup.Provider registries) {
            Placed p = new Placed();
            ListTag list = tag.getList("ids", 8);
            for (int i = 0; i < list.size(); i++) p.ids.add(list.getString(i));
            return p;
        }

        boolean add(String id) {
            boolean added = ids.add(id);
            if (added) setDirty();
            return added;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            ids.forEach(i -> list.add(StringTag.valueOf(i)));
            tag.put("ids", list);
            return tag;
        }
    }
}
