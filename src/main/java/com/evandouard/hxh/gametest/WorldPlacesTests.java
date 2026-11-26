package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.world.WorldPlaces;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Lieux du monde HxH décrits en données : le village de l'île de la Baleine se bâtit avec ses maisons, portes et lits. */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class WorldPlacesTests {

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void whaleIslandVillageIsBuilt(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:whale_island_village");
        helper.assertTrue(place != null, "Le village de l'île de la Baleine est chargé");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        int ground = WorldPlaces.build(helper.getLevel(), place, origin.getX(), origin.getZ());
        var level = helper.getLevel();
        BlockState door = level.getBlockState(new BlockPos(origin.getX() + 4, ground, origin.getZ()));
        helper.assertTrue(door.is(Blocks.OAK_DOOR), "La maison de Mito a sa porte : " + door);
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 1, ground, origin.getZ() + 1)).is(Blocks.RED_BED), "Un lit dans la maison de Mito");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 4, ground + 1, origin.getZ() + 3)).isAir(), "L'intérieur est dégagé");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 11, ground, origin.getZ() + 11)).is(Blocks.WATER), "Le puits contient de l'eau");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void kukurooEstateHasGateKeeperAndMike(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:kukuroo_domaine_zoldyck");
        helper.assertTrue(place != null, "Le domaine Zoldyck est chargé");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() - 9, ground, origin.getZ() + 1)).is(Blocks.IRON_DOOR), "Porte des visiteurs");
        for (int n = 1; n <= 7; n++) {
            BlockState door = level.getBlockState(new BlockPos(origin.getX(), ground + 1, origin.getZ() + 2 * n - 1));
            helper.assertTrue(door.is(com.evandouard.hxh.registry.ModBlocks.HEAVY_DOOR.get()) && door.getValue(com.evandouard.hxh.block.HeavyDoorBlock.DOOR) == n, "Porte n°" + n + " : " + door);
        }
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 19, ground, origin.getZ() - 11)).is(Blocks.BOOKSHELF), "Maison de Zebro meublée");
        var box = new net.minecraft.world.phys.AABB(origin.getX() - 40, ground - 5, origin.getZ() - 60, origin.getX() + 70, ground + 20, origin.getZ() + 130);
        var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
        for (String id : new String[]{"zebro", "gotoh", "canary", "majordome_zoldyck"})
            helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals(id)), "PNJ présent : " + id);
        var mike = level.getEntitiesOfClass(com.evandouard.hxh.beast.MikeEntity.class, box).stream().findFirst();
        helper.assertTrue(mike.isPresent(), "Mike, le chien géant, est là");
        helper.assertTrue(mike.get().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) > 2.0, "Mike est géant : échelle " + mike.get().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE));
        helper.assertTrue(mike.get().getMaxHealth() >= 100, "Mike est coriace : " + mike.get().getMaxHealth() + " PV");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void yorknewCityHasAuctionHouseAndTroupeHideout(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:yorknew_city");
        helper.assertTrue(place != null, "Yorknew City est chargée");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 47, ground, origin.getZ() + 31)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable sur la place");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 10, ground - 1, origin.getZ() + 19)).is(Blocks.BLACK_CONCRETE), "Avenue goudronnée");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 43, ground + 17, origin.getZ() + 30)).is(Blocks.QUARTZ_BRICKS), "Maison de ventes en quartz");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 43, ground + 2, origin.getZ() + 26)).is(Blocks.LECTERN), "Pupitre du commissaire-priseur");
        int seats = 0;
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++)
            if (level.getBlockState(new BlockPos(origin.getX() + 75 + dx, ground, origin.getZ() + 66 + dz)).is(Blocks.DARK_OAK_STAIRS)) seats++;
        helper.assertTrue(seats >= 12, "Treize sièges pour la Brigade (trouvés : " + seats + ")");
        var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 90, ground + 20, origin.getZ() + 85);
        var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
        helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals("commissaire_priseur")), "Commissaire-priseur présent");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void heavensArenaTowerAndSquare(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:heavens_arena");
        helper.assertTrue(place != null, "La Tour Céleste est chargée");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 40, ground, origin.getZ() + 45)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable sur la place");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 26, ground + 48, origin.getZ() + 20)).is(Blocks.STONE_BRICKS), "Mur du 2e étage de la tour");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 40, ground + 3, origin.getZ() + 20)).isAir(), "Hall dégagé");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 40, ground - 1, origin.getZ() + 20)).is(Blocks.SAND), "Ring de sable");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 40, ground + 3, origin.getZ() + 35)).is(Blocks.LECTERN), "Guichet d'inscription");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 40, ground + 99, origin.getZ() + 26)).is(Blocks.LIGHTNING_ROD), "Paratonnerre au sommet");
        var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 95, ground + 20, origin.getZ() + 95);
        var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
        helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals("receptionniste_arene")), "Réceptionniste présente");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void blackWhalePortHasShipAndCranes(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:black_whale_port");
        helper.assertTrue(place != null, "Le port du Black Whale est chargé");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 45, ground, origin.getZ() + 50)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable sur le quai");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 98, ground + 2, origin.getZ() + 30)).is(Blocks.BLACK_CONCRETE), "Coque noire du Black Whale");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 91, ground + 15, origin.getZ() + 14)).is(Blocks.RED_CONCRETE), "Cheminée du navire");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 64, ground + 10, origin.getZ() + 14)).is(Blocks.IRON_BARS), "Grue du quai");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 36, ground, origin.getZ() + 50)).is(Blocks.LECTERN), "Pupitre de la billetterie");
        var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 100, ground + 20, origin.getZ() + 65);
        var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
        helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals("capitaine_baleine_noire")), "Capitaine présent");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void examHotelHasLobbyAndPresidentRoom(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:hunter_exam_hotel");
        helper.assertTrue(place != null, "L'hôtel de l'examen est chargé");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 32, ground, origin.getZ() + 46)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable devant l'hôtel");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 15, ground + 20, origin.getZ() + 20)).is(Blocks.WHITE_CONCRETE), "Façade blanche à 20 blocs");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 25, ground + 3, origin.getZ() + 20)).isAir(), "Hall dégagé");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 35, ground, origin.getZ() - 7)).is(Blocks.DARK_OAK_SLAB), "Table basse du Président");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 30, ground, origin.getZ() - 7)).is(Blocks.GREEN_CARPET), "Tatamis");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 35, ground, origin.getZ() + 7)).is(Blocks.GREEN_CARPET), "Passage hall → pavillon");
        var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ() - 20, origin.getX() + 75, ground + 20, origin.getZ() + 70);
        var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
        for (String id : new String[]{"netero", "beans", "receptionniste_hotel"})
            helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals(id)), "PNJ présent : " + id);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void greedIslandSketch(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:greed_island");
        helper.assertTrue(place != null, "Greed Island est chargée");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 27, ground, origin.getZ() + 35)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 25, ground + 1, origin.getZ() + 35)).is(Blocks.LECTERN), "Pupitre du Livre");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 4, ground + 3, origin.getZ() + 6)).is(Blocks.OAK_PLANKS), "Maison");
        var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 55, ground + 20, origin.getZ() + 75);
        helper.assertTrue(level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box).stream().anyMatch(n -> n.npcId().equals("guide_greed_island")), "Guide présent");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void kiteForestCampSketch(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:kite_forest");
        helper.assertTrue(place != null, "Le camp de la forêt de Kite est chargé");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 21, ground, origin.getZ() + 24)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 20, ground, origin.getZ() + 20)).is(Blocks.CAMPFIRE), "Feu de camp");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 30, ground, origin.getZ() + 8)).is(Blocks.HAY_BLOCK), "Nid de bête");
        var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 45, ground + 20, origin.getZ() + 45);
        helper.assertTrue(level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box).stream().anyMatch(n -> n.npcId().equals("guide_foret_kite")), "Guide présent");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void queenArmSiteSketch(GameTestHelper helper) {
        var place = WorldPlaces.get("hxh:chimera_ant_queen_arm");
        helper.assertTrue(place != null, "Le site du Bras de la Reine est chargé");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 28, ground, origin.getZ() + 52)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 28, ground + 6, origin.getZ() + 28)).is(Blocks.BONE_BLOCK), "Bras d'os au centre");
        helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 28, ground + 20, origin.getZ() + 28)).is(Blocks.NETHER_WART_BLOCK), "Sommet du bras");
        var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 60, ground + 20, origin.getZ() + 60);
        helper.assertTrue(level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box).stream().anyMatch(n -> n.npcId().equals("survivant_atterrissage")), "Survivant présent");
        helper.succeed();
    }

    /** Force (quelques ticks) les tronçons d'un grand lieu pour que ses PNJ soient bien actifs et trouvables. */
    private static void withForcedChunks(GameTestHelper helper, BlockPos origin, int size, Runnable body) {
        var level = helper.getLevel();
        java.util.List<net.minecraft.world.level.ChunkPos> cps = new java.util.ArrayList<>();
        for (int cx = origin.getX() >> 4; cx <= (origin.getX() + size) >> 4; cx++)
            for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + size) >> 4; cz++) cps.add(new net.minecraft.world.level.ChunkPos(cx, cz));
        for (var c : cps) level.setChunkForced(c.x, c.z, true);
        helper.runAfterDelay(30, () -> {
            try { body.run(); } finally { helper.runAfterDelay(80, () -> { for (var c : cps) level.setChunkForced(c.x, c.z, false); }); }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void kakinPalaceSketch(GameTestHelper helper) {
        BlockPos origin0 = helper.absolutePos(new BlockPos(2, 1, 2));
        withForcedChunks(helper, origin0, 105, () -> {
            var place = WorldPlaces.get("hxh:kakin_palace");
            helper.assertTrue(place != null, "Le palais royal de Kakin est chargé");
            BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
            var level = helper.getLevel();
            int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 52, ground, origin.getZ() + 88)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable devant le palais");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 30, ground + 10, origin.getZ() + 40)).is(Blocks.RED_CONCRETE), "Mur de la salle du trône");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 50, ground + 5, origin.getZ() + 23)).is(Blocks.GOLD_BLOCK), "Trône doré");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 50, ground + 27, origin.getZ() + 40)).is(Blocks.GOLD_BLOCK), "Flèche dorée du toit");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 4, ground + 4, origin.getZ() + 50)).is(Blocks.RED_CONCRETE), "Mur d'enceinte");
            helper.runAfterDelay(20, () -> {
                var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 105, ground + 20, origin.getZ() + 105);
                var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
                helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals("chambellan_kakin")), "Chambellan présent");
                helper.assertTrue(npcs.stream().filter(n -> n.npcId().equals("garde_royal_kakin")).count() >= 4, "Gardes royaux présents");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void meteorCitySketch(GameTestHelper helper) {
        BlockPos origin0 = helper.absolutePos(new BlockPos(2, 1, 2));
        withForcedChunks(helper, origin0, 115, () -> {
            var place = WorldPlaces.get("hxh:meteor_city");
            helper.assertTrue(place != null, "Meteor City est chargée");
            BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
            var level = helper.getLevel();
            int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 57, ground, origin.getZ() + 98)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable");
            helper.assertTrue(!level.getBlockState(new BlockPos(origin.getX() + 55, ground + 30, origin.getZ() + 30)).isAir(), "Tour d'ordures");
            helper.assertTrue(!level.getBlockState(new BlockPos(origin.getX() + 14, ground + 8, origin.getZ() + 14)).isAir(), "Montagne de déchets");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 38, ground, origin.getZ() + 80)).is(Blocks.BARREL) || level.getBlockState(new BlockPos(origin.getX() + 37, ground, origin.getZ() + 80)).is(Blocks.CAULDRON), "Étal du marché");
            helper.runAfterDelay(20, () -> {
                var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 115, ground + 20, origin.getZ() + 115);
                var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
                helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals("ferrailleur_meteor")), "Ferrailleur présent");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void nglVillageSketch(GameTestHelper helper) {
        BlockPos origin0 = helper.absolutePos(new BlockPos(2, 1, 2));
        withForcedChunks(helper, origin0, 85, () -> {
            var place = WorldPlaces.get("hxh:ngl_village");
            helper.assertTrue(place != null, "Le village NGL est chargé");
            BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
            var level = helper.getLevel();
            int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 44, ground, origin.getZ() + 66)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 6, ground + 2, origin.getZ() + 6)).is(Blocks.OAK_PLANKS), "Mur d'une cabane");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 40, ground + 6, origin.getZ() + 16)).is(Blocks.HAY_BLOCK), "Toit de paille de la halle");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 10, ground, origin.getZ() + 50)).is(Blocks.WHEAT), "Champ de blé");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 71, ground + 8, origin.getZ() + 30)).is(Blocks.OAK_LOG), "Tour de guet");
            helper.runAfterDelay(20, () -> {
                var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 85, ground + 20, origin.getZ() + 85);
                var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
                helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals("ancien_ngl")), "Ancien présent");
                helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals("fermier_ngl")), "Fermier présent");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void mafiaHqSketch(GameTestHelper helper) {
        BlockPos origin0 = helper.absolutePos(new BlockPos(2, 1, 2));
        withForcedChunks(helper, origin0, 90, () -> {
            var place = WorldPlaces.get("hxh:mafia_hq");
            helper.assertTrue(place != null, "Le QG de la Mafia est chargé");
            BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
            var level = helper.getLevel();
            int ground = WorldPlaces.build(level, place, origin.getX(), origin.getZ());
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 42, ground, origin.getZ() + 80)).is(com.evandouard.hxh.registry.ModBlocks.AIRSHIP_TERMINAL.get()), "Borne du dirigeable");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 4, ground + 2, origin.getZ() + 30)).is(Blocks.POLISHED_BLACKSTONE_BRICKS), "Mur d'enceinte");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 15, ground + 5, origin.getZ() + 20)).is(Blocks.BLACK_CONCRETE), "Bâtiment principal");
            helper.assertTrue(level.getBlockState(new BlockPos(origin.getX() + 35, ground + 33, origin.getZ() + 28)).is(Blocks.GOLD_BLOCK), "Flèche dorée");
            helper.runAfterDelay(20, () -> {
                var box = new net.minecraft.world.phys.AABB(origin.getX(), ground - 5, origin.getZ(), origin.getX() + 90, ground + 40, origin.getZ() + 90);
                var npcs = level.getEntitiesOfClass(com.evandouard.hxh.npc.NpcEntity.class, box);
                helper.assertTrue(npcs.stream().anyMatch(n -> n.npcId().equals("parrain_mafia")), "Parrain présent");
                helper.assertTrue(npcs.stream().filter(n -> n.npcId().equals("garde_mafia")).count() >= 4, "Gardes présents");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void heavyDoorOpensOnlyWithEnoughStrengthThenCloses(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = com.evandouard.hxh.registry.ModBlocks.HEAVY_DOOR.get();
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++)
            level.setBlock(base.offset(x, y, 0), block.defaultBlockState().setValue(com.evandouard.hxh.block.HeavyDoorBlock.DOOR, 5), 3);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(base), net.minecraft.core.Direction.NORTH, base, false);
        var weak = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.stats.HunterStats.setLevel(weak, com.evandouard.hxh.data.Stat.STRENGTH, 10);
        level.getBlockState(base).useWithoutItem(level, weak, hit);
        helper.assertTrue(!level.getBlockState(base).getValue(com.evandouard.hxh.block.HeavyDoorBlock.OPEN), "Force 10 : la porte n°5 (32 t) ne bouge pas");
        var strong = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.stats.HunterStats.setLevel(strong, com.evandouard.hxh.data.Stat.STRENGTH, 60);
        level.getBlockState(base).useWithoutItem(level, strong, hit);
        for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++)
            helper.assertTrue(level.getBlockState(base.offset(x, y, 0)).getValue(com.evandouard.hxh.block.HeavyDoorBlock.OPEN), "Toute la plaque s'ouvre (" + x + "," + y + ")");
        helper.runAfterDelay(160, () -> {
            for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++)
                helper.assertTrue(!level.getBlockState(base.offset(x, y, 0)).getValue(com.evandouard.hxh.block.HeavyDoorBlock.OPEN), "La plaque se referme seule");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void mikeAttacksIntrudersUntilFedThreeTimes(GameTestHelper helper) {
        var mike = helper.spawn(com.evandouard.hxh.registry.ModEntities.MIKE.get(), new BlockPos(2, 1, 2));
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(mike.getX() + 4, mike.getY(), mike.getZ(), 0f, 0f);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(com.evandouard.hxh.beast.MikeEntity.isIntruder(player), "Un inconnu est un intrus pour Mike");
            for (int i = 0; i < 3; i++) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF, 1));
                mike.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            helper.assertTrue(player.getPersistentData().getBoolean(com.evandouard.hxh.beast.MikeEntity.FRIEND_TAG), "Après 3 offrandes, le joueur est un ami");
            helper.assertTrue(!com.evandouard.hxh.beast.MikeEntity.isIntruder(player), "Mike n'attaque plus son ami");
            helper.succeed();
        });
    }
}
