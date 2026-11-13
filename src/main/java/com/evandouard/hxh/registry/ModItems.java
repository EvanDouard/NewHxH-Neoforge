package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HxHMod.MODID);

    /** Carte Hunter : objet provisoire du squelette, sa logique viendra avec l'Examen Hunter. */
    /** Œuf d'apparition du renard-ours (créatif, tests). */
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> FOXBEAR_SPAWN_EGG =
            ITEMS.register("foxbear_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    ModEntities.FOXBEAR, 0x6B4423, 0xE0A060, new Item.Properties()));

    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> GREAT_STAMP_SPAWN_EGG =
            ITEMS.register("great_stamp_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    ModEntities.GREAT_STAMP, 0x8A5A44, 0x3A2A22, new Item.Properties()));

    /** Viande de renard-ours, crue et cuite : ingrédient des recettes du Hunter gourmet. */
    public static final DeferredItem<Item> RAW_FOXBEAR_MEAT = ITEMS.registerSimpleItem("raw_foxbear_meat",
            new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build()));
    public static final DeferredItem<Item> COOKED_FOXBEAR_MEAT = ITEMS.registerSimpleItem("cooked_foxbear_meat",
            new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(10).saturationModifier(0.9f).build()));
    /** Carnet de recettes du Hunter gourmet (jamais consommé en cuisinant). */
    public static final DeferredItem<com.evandouard.hxh.training.GourmetRecipeItem> GOURMET_RECIPE = ITEMS.register("gourmet_recipe",
            () -> new com.evandouard.hxh.training.GourmetRecipeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

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
    /** Pièce d'or de l'épave de l'île-hôtel (on paie sa chambre avec). */
    public static final DeferredItem<Item> SUNKEN_COIN =
            ITEMS.registerSimpleItem("sunken_coin", new Item.Properties().stacksTo(16));
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

    /** Plats nutritifs I à IV (Vitalité). */
    public static final java.util.List<DeferredItem<com.evandouard.hxh.training.NutritiousDishItem>> DISHES =
            java.util.Arrays.stream(com.evandouard.hxh.training.TrainingTier.values())
                    .map(t -> ITEMS.register("nutritious_dish" + t.suffix(),
                            () -> new com.evandouard.hxh.training.NutritiousDishItem(t, new Item.Properties())))
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
    public static final java.util.List<DeferredItem<BlockItem>> MEDITATION_STONES = ModBlocks.MEDITATION_STONES.stream()
            .map(b -> ITEMS.registerSimpleBlockItem(b.getId().getPath(), b)).toList();
    public static final DeferredItem<BlockItem> ILLUSION_WALL =
            ITEMS.registerSimpleBlockItem("illusion_wall", ModBlocks.ILLUSION_WALL);
    public static final DeferredItem<BlockItem> MARKER_PREY =
            ITEMS.registerSimpleBlockItem("marker_prey", ModBlocks.MARKER_PREY);

    private ModItems() {}
}
