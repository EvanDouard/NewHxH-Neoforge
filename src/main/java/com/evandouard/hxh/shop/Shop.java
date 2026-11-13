package com.evandouard.hxh.shop;

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
 * Un marchand, décrit en données ({@code data/hxh/shops/<id>.json}) :
 * <pre>
 * {
 *   "npc": "merchant",                                       // identifiant du PNJ qui tient la boutique
 *   "sells": [ { "item": "hxh:dumbbell_3", "price": 800 },   // ce qu'il vend (prix en Jenny)
 *              { "item": "minecraft:bread", "count": 8, "price": 20 } ],
 *   "buys":  [ { "item": "minecraft:leather", "price": 4 } ] // ce qu'il rachète (prix à l'unité)
 *   // une offre peut demander une quête terminée : "requires": "hxh:gourmet_plat_legendaire"
 * }
 * </pre>
 */
public record Shop(ResourceLocation id, String npc, List<Offer> sells, List<Buy> buys) {
    /** @param requires quête à avoir terminée pour pouvoir l'acheter (vide = toujours) */
    public record Offer(ItemStack stack, int price, String requires) {}

    public record Buy(Item item, int price) {}

    public static Shop parse(ResourceLocation id, JsonObject json) {
        String npc = GsonHelper.getAsString(json, "npc");
        List<Offer> sells = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "sells", new JsonArray())) {
            JsonObject o = e.getAsJsonObject();
            Item item = item(o);
            int count = GsonHelper.getAsInt(o, "count", 1);
            sells.add(new Offer(new ItemStack(item, Math.max(1, count)), Math.max(0, GsonHelper.getAsInt(o, "price")),
                    GsonHelper.getAsString(o, "requires", "")));
        }
        List<Buy> buys = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "buys", new JsonArray())) {
            JsonObject o = e.getAsJsonObject();
            buys.add(new Buy(item(o), Math.max(1, GsonHelper.getAsInt(o, "price"))));
        }
        return new Shop(id, npc, List.copyOf(sells), List.copyOf(buys));
    }

    private static Item item(JsonObject o) {
        ResourceLocation rl = ResourceLocation.parse(GsonHelper.getAsString(o, "item"));
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == Items.AIR) throw new IllegalArgumentException("objet inconnu : " + rl);
        return item;
    }
}
