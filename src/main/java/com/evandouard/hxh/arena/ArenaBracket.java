package com.evandouard.hxh.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Une tranche d'étages de la Tour Céleste (« étage spécial ») : tous les joueurs dont l'étage est dans
 * [from, to] partagent le même hall, avec son hôtesse, son arbitre et son ring central.
 * Lu depuis {@code data/<ns>/arena_brackets/*.json} :
 * <pre>
 * {
 *   "from": 1, "to": 50,
 *   "structure": "hxh:heavens_arena/tranche_1_50",   // optionnel : hall construit à la main
 *   "y_offset": 0,                                    // optionnel : décale la structure en hauteur
 *   "ring_radius": 9,                                 // demi-côté du ring (sortir du carré = sortie de ring)
 *   // Décor du hall généré (utilisé si pas de structure) :
 *   "floor": ["minecraft:stone_bricks", ...], "border": "...", "accent": "...", "under": "...",
 *   "lobby_floor": "minecraft:polished_andesite", "pillar": "...", "pillar_height": 4, "light": "..."
 * }
 * </pre>
 */
public record ArenaBracket(String id, int index, int from, int to, List<BlockState> floor, BlockState border,
                           BlockState accent, BlockState under, BlockState lobbyFloor, BlockState pillar,
                           int pillarHeight, BlockState light, @Nullable ResourceLocation structure, int yOffset,
                           int ringRadius, String signature) {

    public static final ArenaBracket DEFAULT = new ArenaBracket("default", 0, 1, ArenaRules.TOP_FLOOR,
            List.of(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState()),
            Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
            Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState(),
            Blocks.AIR.defaultBlockState(), 0, Blocks.AIR.defaultBlockState(), null, 0, 9, "default");

    public boolean covers(int floorNumber) {
        return floorNumber >= from && floorNumber <= to;
    }

    /** « 1–50 », ou « 251 » pour une tranche d'un seul étage. */
    public String label() {
        return from == to ? String.valueOf(from) : from + "–" + to;
    }

    public ArenaBracket withIndex(int i) {
        return new ArenaBracket(id, i, from, to, floor, border, accent, under, lobbyFloor, pillar, pillarHeight, light,
                structure, yOffset, ringRadius, signature);
    }

    private static BlockState block(JsonObject o, String key, BlockState fallback) {
        if (!o.has(key)) return fallback;
        ResourceLocation id = ResourceLocation.tryParse(o.get(key).getAsString());
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return fallback;
        return BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    }

    public static ArenaBracket parse(ResourceLocation id, JsonElement json) {
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
        return new ArenaBracket(id.toString(), 0,
                o.get("from").getAsInt(),
                o.has("to") ? o.get("to").getAsInt() : o.get("from").getAsInt(),
                List.copyOf(floor),
                block(o, "border", DEFAULT.border),
                block(o, "accent", DEFAULT.accent),
                block(o, "under", DEFAULT.under),
                block(o, "lobby_floor", DEFAULT.lobbyFloor),
                block(o, "pillar", Blocks.AIR.defaultBlockState()),
                o.has("pillar_height") ? o.get("pillar_height").getAsInt() : 4,
                block(o, "light", Blocks.AIR.defaultBlockState()),
                o.has("structure") ? ResourceLocation.tryParse(o.get("structure").getAsString()) : null,
                o.has("y_offset") ? o.get("y_offset").getAsInt() : 0,
                o.has("ring_radius") ? o.get("ring_radius").getAsInt() : 9,
                Integer.toHexString(json.toString().hashCode()));
    }

    /** Nombre de motifs de sol différents (le motif dépend du numéro de tranche). */
    public static final int PATTERNS = 6;

    public int floorIndex(int x, int z) {
        int n = floor.size();
        if (n == 1) return 0;
        int ax = Math.abs(x), az = Math.abs(z);
        int v = switch (Math.floorMod(index, PATTERNS)) {
            case 0 -> (x + z) & 1;
            case 1 -> Math.max(ax, az) / 2;
            case 2 -> Math.floorMod(x + z, 4) / 2;
            case 3 -> (ax <= 1 || az <= 1) ? 1 : 0;
            case 4 -> (ax + az) / 3;
            default -> ((x / 3) + (z / 3)) & 1;
        };
        return Math.floorMod(v, n);
    }
}
