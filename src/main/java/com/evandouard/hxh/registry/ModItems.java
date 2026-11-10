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
    /** Zevil : plaque d'un autre candidat (1 point) et plaque de sa cible (3 points). */
    public static final DeferredItem<Item> EXAM_PLATE =
            ITEMS.registerSimpleItem("exam_plate", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> TARGET_PLATE =
            ITEMS.registerSimpleItem("target_plate", new Item.Properties().stacksTo(1));
    /** Mont Split : œuf d'aigle-araignée (Menchi le veut cuit, mais on le lui rapporte tel quel). */
    /** La balle du jeu de Netero dans le dirigeable (souvenir pour qui l'a fait plier). */
    public static final DeferredItem<Item> NETERO_BALL =
            ITEMS.registerSimpleItem("netero_ball", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SPIDER_EAGLE_EGG =
            ITEMS.registerSimpleItem("spider_eagle_egg", new Item.Properties().stacksTo(4));

    public static final DeferredItem<BlockItem> HUNTER_COMPUTER =
            ITEMS.registerSimpleBlockItem("hunter_computer", ModBlocks.HUNTER_COMPUTER);
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

    /** Haltères I à IV (Force). */
    public static final java.util.List<DeferredItem<com.evandouard.hxh.training.DumbbellItem>> DUMBBELLS =
            java.util.Arrays.stream(com.evandouard.hxh.training.TrainingTier.values())
                    .map(t -> ITEMS.register("dumbbell" + t.suffix(),
                            () -> new com.evandouard.hxh.training.DumbbellItem(t, new Item.Properties())))
                    .toList();

    /** Tapis de course I à IV (Vitesse ou Endurance). */
    public static final java.util.List<DeferredItem<BlockItem>> TREADMILLS = java.util.stream.IntStream
            .range(0, ModBlocks.TREADMILLS.size())
            .mapToObj(i -> ITEMS.registerSimpleBlockItem(ModBlocks.TREADMILLS.get(i).getId().getPath(), ModBlocks.TREADMILLS.get(i),
                    new Item.Properties().rarity(com.evandouard.hxh.training.TrainingTier.values()[i].rarity())))
            .toList();

    public static final DeferredItem<BlockItem> MARKER_NPC_CHOICE =
            ITEMS.registerSimpleBlockItem("marker_npc", ModBlocks.MARKER_NPC_CHOICE);
    public static final DeferredItem<BlockItem> MARKER_WAYPOINT =
            ITEMS.registerSimpleBlockItem("marker_waypoint", ModBlocks.MARKER_WAYPOINT);
    public static final DeferredItem<BlockItem> MARKER_PREY =
            ITEMS.registerSimpleBlockItem("marker_prey", ModBlocks.MARKER_PREY);

    private ModItems() {}
}
