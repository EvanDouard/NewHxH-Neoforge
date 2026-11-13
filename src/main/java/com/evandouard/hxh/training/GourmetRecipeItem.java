package com.evandouard.hxh.training;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Carnet de recettes du Hunter gourmet : récompense de sa quête finale. Il entre dans la recette des plats nutritifs
 * III et IV mais n'est jamais consommé (il revient dans la grille d'artisanat) : c'est lui qui « débloque » la
 * cuisine des plats de gourmet.
 */
public class GourmetRecipeItem extends Item {
    public GourmetRecipeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hxh.gourmet_recipe").withStyle(ChatFormatting.GRAY));
    }
}
