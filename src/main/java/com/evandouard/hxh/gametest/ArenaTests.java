package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.arena.ArenaBracket;
import com.evandouard.hxh.arena.ArenaBrackets;
import com.evandouard.hxh.arena.ArenaFighterEntity;
import com.evandouard.hxh.arena.ArenaManager;
import com.evandouard.hxh.arena.ArenaRules;
import com.evandouard.hxh.arena.ArenaWorld;
import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.hatsu.Hatsu;
import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.quest.QuestRegistry;
import com.evandouard.hxh.registry.ModBlocks;
import com.evandouard.hxh.registry.ModEntities;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Tour Céleste : tranches d'étages, règles de montée et de descente, halls, PNJ uniques, adversaires,
 * inscription par tranche, quête du guichet.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class ArenaTests {

    @GameTest(template = "empty")
    public static void bracketsGroupFloors(GameTestHelper helper) {
        helper.assertTrue(ArenaBrackets.count() == 15, "15 tranches chargées, obtenu " + ArenaBrackets.count());
        helper.assertTrue(ArenaBrackets.indexOf(35) == ArenaBrackets.indexOf(25), "Les étages 25 et 35 partagent la tranche 1–50");
        helper.assertTrue(ArenaBrackets.indexOf(50) != ArenaBrackets.indexOf(51), "50 et 51 sont dans deux tranches");
        helper.assertTrue("1–50".equals(ArenaBrackets.forFloor(1).label()), "Libellé de la 1re tranche");
        helper.assertTrue("251".equals(ArenaBrackets.forFloor(251).label()), "Le 251e est une tranche à lui seul");
        helper.assertTrue(ArenaBrackets.indexOf(228) == ArenaBrackets.indexOf(226), "Tranche 226–230");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void floorRules(GameTestHelper helper) {
        helper.assertTrue(ArenaRules.floorAfterWin(1, false) == 6, "Victoire : +5 étages");
        helper.assertTrue(ArenaRules.floorAfterWin(1, true) == 9, "Victoire rapide : +8 étages");
        helper.assertTrue(ArenaRules.floorAfterWin(197, true) == 200, "On ne saute pas le 200e étage");
        helper.assertTrue(ArenaRules.floorAfterWin(203, true) == 204, "Au-delà du 200e : +1 étage");
        helper.assertTrue(ArenaRules.floorAfterWin(250, false) == 251, "251e = Maître d'étage");
        helper.assertTrue(ArenaRules.floorAfterLoss(30) == 25, "1re tranche : −5 étages");
        helper.assertTrue(ArenaRules.floorAfterLoss(3) == 1, "Jamais sous le 1er étage");
        helper.assertTrue(ArenaRules.floorAfterLoss(73) == 50, "Défaite : retour en haut de la tranche d'en dessous");
        helper.assertTrue(ArenaRules.floorAfterLoss(203) == 200, "Défaite au 203e : retour au 200e");
        helper.assertTrue(ArenaRules.jennyForWin(200) == 0, "Plus de Jenny au-delà du 200e");
        helper.assertTrue(ArenaRules.opponentNenLevel(199) == 0 && ArenaRules.opponentNenLevel(200) > 0,
                "Le Nen apparaît au 200e étage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fighterBelow200HasNoNen(GameTestHelper helper) {
        ArenaFighterEntity f = helper.spawn(ModEntities.ARENA_FIGHTER.get(), new BlockPos(1, 1, 1));
        f.configure(50, null);
        HunterData d = HunterStats.get(f);
        helper.assertTrue(!d.nenUnlocked(), "Pas de Nen sous le 200e étage");
        helper.assertTrue(d.level(Stat.STRENGTH) > 5 && d.level(Stat.STRENGTH) < 30,
                "Force d'un adversaire du 50e ≈ 15, obtenu " + d.level(Stat.STRENGTH));
        f.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fighterFrom200HasNenAndHatsu(GameTestHelper helper) {
        ArenaFighterEntity f = helper.spawn(ModEntities.ARENA_FIGHTER.get(), new BlockPos(1, 1, 1));
        f.configure(210, null);
        HunterData d = HunterStats.get(f);
        helper.assertTrue(d.nenUnlocked(), "Nen éveillé au 210e étage");
        helper.assertTrue(Hatsu.kitOf(d) != null, "Un Hatsu jouable est attribué");
        helper.assertTrue(Hatsu.kitOf(d).type() == d.nenType(), "Le Hatsu correspond au type de Nen");
        f.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void entryHallHasOneReceptionist(GameTestHelper helper) {
        // Le serveur de GameTests n'a pas la dimension de la Tour : on construit dans le monde de test
        ServerLevel level = ArenaWorld.level(helper.getLevel().getServer());
        if (level == null) level = helper.getLevel();
        ArenaWorld.Layout layout = ArenaWorld.ensureLobby(level);
        helper.assertTrue(level.getBlockState(ArenaWorld.ELEVATOR).is(ModBlocks.ARENA_ELEVATOR.get()), "Ascenseur dans le hall");
        helper.assertTrue(level.getBlockState(ArenaWorld.EXIT).is(ModBlocks.ARENA_EXIT.get()), "Sortie dans le hall");
        ArenaWorld.ensureLobby(level);
        ServerLevel l = level;
        int count = l.getEntities(ModEntities.NPC.get(), n -> n.npcId().equals("receptionist")
                && n.home().equals(layout.receptionist())).size();
        helper.assertTrue(count == 1, "Une seule hôtesse même si le hall est revérifié, obtenu " + count);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void evansArenaHasFourReceptionistsAndReferees(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArenaBracket bracket = ArenaBrackets.forFloor(120);
        helper.assertTrue(bracket.structure() != null && bracket.structure().getPath().equals("heavens_arena/arene"),
                "Les tranches utilisent l'arène d'Evan");
        ArenaWorld.ensureHall(level, bracket);
        ArenaWorld.Layout layout = ArenaWorld.ensureHall(level, bracket);
        helper.assertTrue(layout.receptionists().size() == 4 && layout.referees().size() == 4,
                "4 hôtesses et 4 arbitres lus sur les repères, obtenu " + layout.receptionists().size() + "/" + layout.referees().size());
        helper.assertTrue(layout.ringRadius() == 10, "Ring de 21 × 21");
        helper.assertTrue(layout.inRing(layout.playerStart().getX(), layout.playerStart().getZ())
                && layout.inRing(layout.opponentStart().getX(), layout.opponentStart().getZ()), "Les deux combattants démarrent dans le ring");
        helper.assertTrue(!layout.inRing(layout.arrival().getX(), layout.arrival().getZ()), "On arrive hors du ring (côté spectateurs)");
        helper.assertTrue(!level.getBlockState(layout.playerStart().below()).isAir(), "Sol sous le départ du joueur");
        helper.assertTrue(level.getBlockState(layout.playerStart()).isAir(), "Repère retiré après la pose");
        for (String id : List.of("receptionist", "referee")) {
            List<BlockPos> homes = id.equals("referee") ? layout.referees() : layout.receptionists();
            for (BlockPos home : homes) {
                int n = level.getEntities(ModEntities.NPC.get(), e -> e.npcId().equals(id) && e.home().equals(home)).size();
                helper.assertTrue(n == 1, "Un seul PNJ « " + id + " » en " + home.toShortString() + ", obtenu " + n);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overworldTowerIsAvailable(GameTestHelper helper) {
        var tower = helper.getLevel().getStructureManager().get(ResourceLocation.parse("hxh:heavens_arena/tour"));
        helper.assertTrue(tower.isPresent(), "La structure de la tour est livrée avec le mod");
        helper.assertTrue(tower.get().getSize().getY() == 253, "Tour de 253 blocs de haut");
        int gates = tower.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                ModBlocks.HEAVENS_ARENA_GATE.get()).size();
        helper.assertTrue(gates == 2, "Deux portails d'entrée, obtenu " + gates);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void handBuiltHallUsesMarkers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Tranche fictive qui utilise la structure d'exemple livrée avec le mod
        ArenaBracket base = ArenaBrackets.get(13);
        ArenaBracket bracket = new ArenaBracket("test", 13, base.from(), base.to(), base.floor(), base.border(), base.accent(),
                base.under(), base.lobbyFloor(), base.pillar(), base.pillarHeight(), base.light(),
                ResourceLocation.parse("hxh:heavens_arena/exemple"), 0, 7, "test-exemple");
        ArenaWorld.Layout layout = ArenaWorld.ensureHall(level, bracket);
        helper.assertTrue(layout.receptionist() != null && layout.referee() != null, "Repères de l'hôtesse et de l'arbitre trouvés");
        helper.assertTrue(level.getBlockState(layout.playerStart()).isAir(), "Le repère du joueur est retiré après la pose");
        helper.assertTrue(!level.getBlockState(layout.playerStart().below()).isAir(), "Il y a un sol sous le départ du joueur");
        helper.assertTrue(layout.inRing(layout.opponentStart().getX(), layout.opponentStart().getZ()), "L'adversaire démarre dans le ring");
        int referees = level.getEntities(ModEntities.NPC.get(),
                n -> n.npcId().equals("referee") && n.home().equals(layout.referee())).size();
        helper.assertTrue(referees == 1, "L'arbitre est posé sur son repère, obtenu " + referees);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bigStructureCanBeSaved(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(0, 1, 0));
        // Plus grand que la limite de 48 du bloc de structure, en hauteur
        BlockPos b = a.offset(2, 59, 2);
        level.setBlock(a, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        level.setBlock(b, net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        ResourceLocation id = ResourceLocation.parse("hxh:test/grande_structure");
        net.minecraft.core.Vec3i size = com.evandouard.hxh.arena.StructureSaver.save(level, id, b, a);
        helper.assertTrue(size != null && size.getY() == 60, "Zone de 3 × 60 × 3 sauvegardée, obtenu " + size);
        level.setBlock(a, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        var template = level.getStructureManager().get(id);
        helper.assertTrue(template.isPresent() && template.get().getSize().getY() == 60, "La structure relue fait 60 de haut");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void duplicateNpcsAreRemoved(GameTestHelper helper) {
        BlockPos home = helper.absolutePos(new BlockPos(1, 1, 1)).offset(0, 1000, 0);
        for (int i = 0; i < 3; i++) {
            NpcEntity npc = helper.spawn(ModEntities.NPC.get(), new BlockPos(1, 1, 1));
            npc.setNpcId("receptionist");
            npc.setHome(home);
            npc.setNoGravity(true);
        }
        int removed = ArenaWorld.removeDuplicateNpcs(helper.getLevel());
        helper.assertTrue(removed >= 2, "Les hôtesses en double sont retirées, obtenu " + removed);
        int left = helper.getLevel().getEntities(ModEntities.NPC.get(), n -> n.isAlive() && n.home().equals(home)).size();
        helper.assertTrue(left == 1, "Il en reste une, obtenu " + left);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void elevatorAndRegistrationRules(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        helper.assertTrue(ArenaManager.maxLocation(player) == 0, "Sans inscription, seul le hall d'entrée est accessible");
        d.setArenaRegistered(true);
        d.setRegisteredBracket(0);
        d.setArenaFloor(73);
        helper.assertTrue(ArenaManager.maxLocation(player) == ArenaWorld.locationOfBracket(1), "Jusqu'au hall de sa tranche (51–100)");
        helper.assertTrue("message.hxh.arena_not_inside".equals(ArenaManager.goTo(player, 1)), "L'ascenseur ne marche que dans la Tour");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void counterQuestAndJenny(GameTestHelper helper) {
        Quest quest = QuestRegistry.get(QuestRegistry.HEAVENS_ARENA);
        helper.assertTrue(quest != null, "La quête hxh:heavens_arena doit être chargée");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NpcEntity hostess = helper.spawn(ModEntities.NPC.get(), new BlockPos(1, 1, 1));
        hostess.setNpcId("receptionist");
        hostess.setNoGravity(true);
        QuestData d = QuestManager.data(player);

        QuestManager.onTalk(player, hostess);
        helper.assertTrue(d.arenaRegistered() && d.registeredBracket() == 0, "L'hôtesse inscrit le joueur à la 1re tranche");
        helper.assertTrue(QuestRegistry.HEAVENS_ARENA.toString().equals(d.active()) && d.step() == 1,
                "La quête du guichet démarre et la 1re étape est validée");

        int floor = ArenaManager.applyResult(player, 1, true, false);
        helper.assertTrue(floor == 6 && d.arenaFloor() == 6, "Victoire au 1er étage → 6e");
        helper.assertTrue(d.jenny() == ArenaRules.jennyForWin(1), "Jenny de la victoire");
        helper.assertTrue(d.step() == 2, "Étape « gagner un combat » validée");

        QuestManager.onTalk(player, hostess);
        helper.assertTrue(d.jenny() == ArenaRules.jennyForWin(1) + 500, "Prime de 500 Jenny au guichet");

        ArenaManager.applyResult(player, 6, false, false);
        helper.assertTrue(d.arenaFloor() == 1 && d.arenaLosses() == 1, "Défaite dans la 1re tranche : −5 étages");
        ArenaManager.applyResult(player, 47, true, false);
        helper.assertTrue(d.arenaFloor() == 52, "Victoire au 47e → 52e");
        helper.assertTrue(d.step() == 4, "Le 50e étage atteint valide l'étape");
        helper.assertTrue(d.registeredBracket() == 0, "Nouvelle tranche : pas encore inscrit, il faut voir l'hôtesse");
        helper.succeed();
    }
}
