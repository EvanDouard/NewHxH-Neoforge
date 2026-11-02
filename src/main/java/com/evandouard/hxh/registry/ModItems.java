package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HxHMod.MODID);

    /** Carte Hunter : objet provisoire du squelette, sa logique viendra avec l'Examen Hunter. */
    public static final DeferredItem<Item> HUNTER_CARD =
            ITEMS.registerSimpleItem("hunter_card", new Item.Properties().stacksTo(1));

    private ModItems() {}
}
