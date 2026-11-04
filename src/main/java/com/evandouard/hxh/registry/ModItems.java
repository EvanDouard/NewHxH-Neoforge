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

    public static final DeferredItem<BlockItem> HEAVENS_ARENA_GATE =
            ITEMS.registerSimpleBlockItem("heavens_arena_gate", ModBlocks.HEAVENS_ARENA_GATE);

    public static final DeferredItem<BlockItem> ARENA_ELEVATOR =
            ITEMS.registerSimpleBlockItem("arena_elevator", ModBlocks.ARENA_ELEVATOR);
    public static final DeferredItem<BlockItem> ARENA_EXIT =
            ITEMS.registerSimpleBlockItem("arena_exit", ModBlocks.ARENA_EXIT);
    public static final DeferredItem<BlockItem> MARKER_PLAYER =
            ITEMS.registerSimpleBlockItem("arena_marker_player", ModBlocks.MARKER_PLAYER);
    public static final DeferredItem<BlockItem> MARKER_OPPONENT =
            ITEMS.registerSimpleBlockItem("arena_marker_opponent", ModBlocks.MARKER_OPPONENT);
    public static final DeferredItem<BlockItem> MARKER_NPC =
            ITEMS.registerSimpleBlockItem("arena_marker_npc", ModBlocks.MARKER_NPC);
    public static final DeferredItem<BlockItem> MARKER_RECEPTIONIST =
            ITEMS.registerSimpleBlockItem("arena_marker_receptionist", ModBlocks.MARKER_RECEPTIONIST);
    public static final DeferredItem<BlockItem> MARKER_REFEREE =
            ITEMS.registerSimpleBlockItem("arena_marker_referee", ModBlocks.MARKER_REFEREE);
    public static final DeferredItem<BlockItem> MARKER_ARRIVAL =
            ITEMS.registerSimpleBlockItem("arena_marker_arrival", ModBlocks.MARKER_ARRIVAL);

    private ModItems() {}
}
