package com.evandouard.hxh.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Décor d'une tranche d'étages de la Tour Céleste, lu depuis {@code data/<ns>/arena_themes/*.json}.
 * <pre>
 * {
 *   "from": 1, "to": 19,                         // étages concernés (inclus)
 *   "floor": ["minecraft:stone_bricks", ...],     // blocs du sol, mélangés selon un motif
 *   "border": "minecraft:white_concrete",         // liseré du ring
 *   "accent": "minecraft:chiseled_stone_bricks",  // ligne centrale et cercle du milieu
 *   "under": "minecraft:smooth_stone",            // dessous de la plateforme
 *   "pillar": "minecraft:quartz_pillar",          // piliers des 4 coins (vide = aucun)
 *   "pillar_height": 4,
 *   "light": "minecraft:lantern"                  // posé en haut des piliers
 * }
 * </pre>
 * Le motif du sol change à chaque étage (damier, anneaux, rayures, croix…), donc deux étages voisins
 * d'une même tranche ne se ressemblent pas.
 */
public record ArenaTheme(String id, int from, int to, List<BlockState> floor, BlockState border, BlockState accent,
                         BlockState under, BlockState pillar, int pillarHeight, BlockState light) {

    public static final ArenaTheme DEFAULT = new ArenaTheme("default", 0, Integer.MAX_VALUE,
            List.of(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState()),
            Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
            Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), 0, Blocks.AIR.defaultBlockState());

    public boolean covers(int floorNumber) {
        return floorNumber >= from && floorNumber <= to;
    }

    private static BlockState block(JsonObject o, String key, BlockState fallback) {
        if (!o.has(key)) return fallback;
        ResourceLocation id = ResourceLocation.tryParse(o.get(key).getAsString());
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return fallback;
        Block b = BuiltInRegistries.BLOCK.get(id);
        return b.defaultBlockState();
    }

    public static ArenaTheme parse(ResourceLocation id, JsonElement json) {
        JsonObject o = json.getAsJsonObject();
        List<BlockState> floor = new ArrayList<>();
        if (o.has("floor")) {
            JsonElement f = o.get("floor");
            JsonArray array = f.isJsonArray() ? f.getAsJsonArray() : new JsonArray();
            if (!f.isJsonArray()) array.add(f);
            for (JsonElement e : array) {
                ResourceLocation b = ResourceLocation.tryParse(e.getAsString());
                if (b != null && BuiltInRegistries.BLOCK.containsKey(b)) floor.add(BuiltInRegistries.BLOCK.get(b).defaultBlockState());
            }
        }
        if (floor.isEmpty()) floor.addAll(DEFAULT.floor);
        return new ArenaTheme(id.toString(),
                o.has("from") ? o.get("from").getAsInt() : 0,
                o.has("to") ? o.get("to").getAsInt() : Integer.MAX_VALUE,
                List.copyOf(floor),
                block(o, "border", DEFAULT.border),
                block(o, "accent", DEFAULT.accent),
                block(o, "under", DEFAULT.under),
                block(o, "pillar", Blocks.AIR.defaultBlockState()),
                o.has("pillar_height") ? o.get("pillar_height").getAsInt() : 4,
                block(o, "light", Blocks.AIR.defaultBlockState()));
    }

    /** Nombre de motifs de sol différents. */
    public static final int PATTERNS = 6;

    /**
     * Index du bloc de sol (dans {@link #floor}) pour la case (x, z) relative au centre, selon le motif de l'étage.
     */
    public int floorIndex(int floorNumber, int x, int z) {
        int n = floor.size();
        if (n == 1) return 0;
        int ax = Math.abs(x), az = Math.abs(z);
        int v = switch (Math.floorMod(floorNumber, PATTERNS)) {
            case 0 -> (x + z) & 1;                       // damier
            case 1 -> Math.max(ax, az) / 2;              // anneaux carrés
            case 2 -> Math.floorMod(x + z, 4) / 2;       // rayures diagonales
            case 3 -> (ax <= 1 || az <= 1) ? 1 : 0;      // grande croix
            case 4 -> (ax + az) / 3;                     // losanges
            default -> ((x / 3) + (z / 3)) & 1;          // grand damier
        };
        return Math.floorMod(v, n);
    }
}
