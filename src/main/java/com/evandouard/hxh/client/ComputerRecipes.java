package com.evandouard.hxh.client;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.network.OpenComputerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recettes du mod dans l'ordinateur Hunter (catégorie « Recettes ») : chaque craft du mod, lu dans le gestionnaire de
 * recettes du client (envoyé par le serveur, donc à jour avec les datapacks), avec sa grille et ses ingrédients.
 */
public final class ComputerRecipes {
    private ComputerRecipes() {}

    public static List<OpenComputerPayload.Entry> entries() {
        Minecraft mc = Minecraft.getInstance();
        List<OpenComputerPayload.Entry> out = new ArrayList<>();
        if (mc.level == null) return out;
        String category = Component.translatable("screen.hxh.computer.recipes").getString();
        List<RecipeHolder<?>> mine = new ArrayList<>();
        for (RecipeHolder<?> h : mc.level.getRecipeManager().getRecipes()) {
            if (h.id().getNamespace().equals(HxHMod.MODID)) mine.add(h);
        }
        mine.sort(Comparator.comparing(h -> h.id().getPath()));
        for (RecipeHolder<?> h : mine) {
            Recipe<?> r = h.value();
            ItemStack result = r.getResultItem(mc.level.registryAccess());
            if (result.isEmpty()) continue;
            String title = result.getHoverName().getString() + (result.getCount() > 1 ? " ×" + result.getCount() : "");
            out.add(new OpenComputerPayload.Entry("recipe_" + h.id().getPath(), category, title, text(r), true, ""));
        }
        return out;
    }

    private static String name(Ingredient ing) {
        ItemStack[] items = ing.getItems();
        if (items.length == 0) return "?";
        String n = items[0].getHoverName().getString();
        return items.length > 1 ? n + " " + Component.translatable("screen.hxh.computer.recipe_or_similar").getString() : n;
    }

    private static String text(Recipe<?> r) {
        StringBuilder sb = new StringBuilder();
        if (r instanceof ShapedRecipe shaped) {
            sb.append(Component.translatable("screen.hxh.computer.recipe_shaped").getString()).append("\n\n");
            // une lettre par ingrédient différent, puis la grille et la légende
            Map<String, Character> letters = new LinkedHashMap<>();
            var ings = shaped.getIngredients();
            for (int y = 0; y < shaped.getHeight(); y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < shaped.getWidth(); x++) {
                    Ingredient ing = ings.get(y * shaped.getWidth() + x);
                    if (ing.isEmpty()) {
                        row.append("[   ]");
                        continue;
                    }
                    String n = name(ing);
                    char c = letters.computeIfAbsent(n, k -> (char) ('A' + letters.size()));
                    row.append("[ ").append(c).append(" ]");
                }
                sb.append(row).append("\n");
            }
            sb.append("\n");
            for (var e : letters.entrySet()) sb.append(e.getValue()).append(" = ").append(e.getKey()).append("\n");
        } else if (r instanceof AbstractCookingRecipe cooking) {
            String where = r instanceof SmokingRecipe ? "screen.hxh.computer.recipe_smoker"
                    : r instanceof CampfireCookingRecipe ? "screen.hxh.computer.recipe_campfire" : "screen.hxh.computer.recipe_furnace";
            sb.append(Component.translatable(where).getString()).append("\n\n");
            sb.append(name(cooking.getIngredients().get(0))).append("  →  ")
                    .append(cooking.getResultItem(Minecraft.getInstance().level.registryAccess()).getHoverName().getString())
                    .append("\n").append(Component.translatable("screen.hxh.computer.recipe_time", cooking.getCookingTime() / 20).getString());
        } else {
            sb.append(Component.translatable("screen.hxh.computer.recipe_shapeless").getString()).append("\n\n");
            for (Ingredient ing : r.getIngredients()) if (!ing.isEmpty()) sb.append("• ").append(name(ing)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
