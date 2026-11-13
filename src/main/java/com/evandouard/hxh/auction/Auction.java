package com.evandouard.hxh.auction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Une salle des ventes, décrite en données ({@code data/hxh/auctions/<id>.json}) :
 * <pre>
 * {
 *   "npc": "commissaire_priseur",     // PNJ qui tient la vente
 *   "lots_per_day": 4,                // lots mis en vente chaque jour de jeu
 *   "lots": [ { "item": "hxh:dumbbell_4", "start": 2500, "value": 4000, "weight": 10 } ]
 * }
 * </pre>
 * {@code start} = mise de départ, {@code value} = valeur estimée : les enchérisseurs PNJ montent jusqu'à environ
 * 75–125 % de cette valeur. {@code weight} = chance relative d'être tiré parmi les lots du jour.
 */
public record Auction(ResourceLocation id, String npc, int lotsPerDay, List<Lot> lots) {
    public record Lot(ItemStack stack, int start, int value, int weight) {}

    public static Auction parse(ResourceLocation id, JsonObject json) {
        List<Lot> lots = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "lots", new JsonArray())) {
            JsonObject o = e.getAsJsonObject();
            ResourceLocation rl = ResourceLocation.parse(GsonHelper.getAsString(o, "item"));
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item == Items.AIR) throw new IllegalArgumentException("objet inconnu : " + rl);
            int start = Math.max(1, GsonHelper.getAsInt(o, "start"));
            lots.add(new Lot(new ItemStack(item, Math.max(1, GsonHelper.getAsInt(o, "count", 1))), start,
                    Math.max(start, GsonHelper.getAsInt(o, "value", start * 2)), Math.max(1, GsonHelper.getAsInt(o, "weight", 1))));
        }
        if (lots.isEmpty()) throw new IllegalArgumentException("aucun lot");
        return new Auction(id, GsonHelper.getAsString(json, "npc"), Math.max(1, GsonHelper.getAsInt(json, "lots_per_day", 4)), List.copyOf(lots));
    }
}
