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

    /** Disposition à dessiner : grille 3×3 (ou une case pour la cuisson), résultat, sorte de recette. */
    public record Layout(List<Ingredient> grid, int width, ItemStack result, String kindKey, int cookSeconds) {
        public boolean cooking() {
            return cookSeconds >= 0;
        }
    }

    private static final Map<String, Layout> LAYOUTS = new java.util.HashMap<>();

    /** Disposition de la fiche {@code recipe_…}, ou null. */
    @org.jetbrains.annotations.Nullable
    public static Layout layout(String entryId) {
        return LAYOUTS.get(entryId);
    }

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
            String id = "recipe_" + h.id().getPath();
            LAYOUTS.put(id, layoutOf(r, result));
            out.add(new OpenComputerPayload.Entry(id, category, title, totals(r), true, ""));
        }
        return out;
    }

    private static String name(Ingredient ing) {
        ItemStack[] items = ing.getItems();
        if (items.length == 0) return "?";
        String n = items[0].getHoverName().getString();
        return items.length > 1 ? n + " " + Component.translatable("screen.hxh.computer.recipe_or_similar").getString() : n;
    }

    private static Layout layoutOf(Recipe<?> r, ItemStack result) {
        List<Ingredient> grid = new ArrayList<>();
        if (r instanceof ShapedRecipe shaped) {
            // recentrée dans une grille 3 × 3, comme à l'établi
            for (int i = 0; i < 9; i++) grid.add(Ingredient.EMPTY);
            for (int y = 0; y < shaped.getHeight(); y++) for (int x = 0; x < shaped.getWidth(); x++) {
                grid.set(y * 3 + x, shaped.getIngredients().get(y * shaped.getWidth() + x));
            }
            return new Layout(grid, 3, result, "screen.hxh.computer.recipe_shaped", -1);
        }
        if (r instanceof AbstractCookingRecipe cooking) {
            grid.add(cooking.getIngredients().get(0));
            String where = r instanceof SmokingRecipe ? "screen.hxh.computer.recipe_smoker"
                    : r instanceof CampfireCookingRecipe ? "screen.hxh.computer.recipe_campfire" : "screen.hxh.computer.recipe_furnace";
            return new Layout(grid, 1, result, where, cooking.getCookingTime() / 20);
        }
        for (Ingredient ing : r.getIngredients()) if (!ing.isEmpty() && grid.size() < 9) grid.add(ing);
        while (grid.size() < 9) grid.add(Ingredient.EMPTY);
        return new Layout(grid, 3, result, "screen.hxh.computer.recipe_shapeless", -1);
    }

    /** Sous la grille : la liste des ingrédients avec leur nombre (« 5 × Lingot de fer »). */
    private static String totals(Recipe<?> r) {
        Map<String, Integer> count = new LinkedHashMap<>();
        for (Ingredient ing : r.getIngredients()) if (!ing.isEmpty()) count.merge(name(ing), 1, Integer::sum);
        StringBuilder sb = new StringBuilder();
        for (var e : count.entrySet()) sb.append(e.getValue()).append(" × ").append(e.getKey()).append("\n");
        return sb.toString().stripTrailing();
    }
}
