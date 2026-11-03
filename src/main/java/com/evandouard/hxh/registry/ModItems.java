package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HxHMod.MODID);

    /** Carte Hunter : objet provisoire du squelette, sa logique viendra avec l'Examen Hunter. */
    public static final DeferredItem<Item> HUNTER_CARD =
            ITEMS.registerSimpleItem("hunter_card", new Item.Properties().stacksTo(1));

    /** Plaque numérotée reçue à l'inscription de l'Examen Hunter. */
    public static final DeferredItem<Item> EXAM_BADGE =
            ITEMS.registerSimpleItem("exam_badge", new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlockItem> WATER_DIVINATION =
            ITEMS.registerSimpleBlockItem("water_divination", ModBlocks.WATER_DIVINATION);

    private ModItems() {}
}
