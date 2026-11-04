package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.arena.ArenaFighterEntity;
import com.evandouard.hxh.arena.ArenaManager;
import com.evandouard.hxh.arena.ArenaRules;
import com.evandouard.hxh.arena.ArenaTheme;
import com.evandouard.hxh.arena.ArenaThemes;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Tour Céleste : règles des étages, dimension et hall, adversaires (sans puis avec Nen), quête du guichet.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class ArenaTests {

    @GameTest(template = "empty")
    public static void floorRules(GameTestHelper helper) {
        helper.assertTrue(ArenaRules.floorAfterWin(1, false) == 11, "Victoire : +10 étages");
        helper.assertTrue(ArenaRules.floorAfterWin(1, true) == 21, "Victoire rapide : +20 étages");
        helper.assertTrue(ArenaRules.floorAfterWin(195, true) == 200, "On ne saute pas le 200e étage");
        helper.assertTrue(ArenaRules.floorAfterLoss(50) == 45, "Défaite sous le 200e : −5 étages");
        helper.assertTrue(ArenaRules.floorAfterLoss(3) == 1, "Jamais sous le 1er étage");
        helper.assertTrue(ArenaRules.floorAfterLoss(220) == 220, "Au-delà du 200e, une défaite ne fait pas redescendre");
        helper.assertTrue(ArenaRules.floorAfterWin(245, false) == 250, "Le sommet est au 250e");
        helper.assertTrue(ArenaRules.jennyForWin(100) > ArenaRules.jennyForWin(10), "Plus haut = plus de Jenny");
        helper.assertTrue(ArenaRules.jennyForWin(200) == 0, "Plus de Jenny au-delà du 200e");
        helper.assertTrue(ArenaRules.opponentNenLevel(199) == 0 && ArenaRules.opponentNenLevel(200) > 0,
                "Le Nen apparaît au 200e étage");
        helper.assertTrue(ArenaRules.opponentStatLevel(10) < ArenaRules.opponentStatLevel(150), "Les adversaires montent en force");
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
        helper.assertTrue(d.nenLevel() == ArenaRules.opponentNenLevel(210), "Niveau de Nen selon l'étage");
        f.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dimensionLobbyAndArenaAreBuilt(GameTestHelper helper) {
        // Le serveur de GameTests ne crée que les dimensions vanilla : on construit alors dans le monde de test
        // (la vraie dimension est vérifiée par le client de la CI).
        ServerLevel arena = ArenaWorld.level(helper.getLevel().getServer());
        if (arena == null) arena = helper.getLevel();
        ArenaWorld.ensureLobby(arena);
        helper.assertTrue(arena.getBlockState(ArenaWorld.ELEVATOR).is(ModBlocks.ARENA_ELEVATOR.get()), "Ascenseur dans le hall");
        helper.assertTrue(arena.getBlockState(ArenaWorld.EXIT).is(ModBlocks.ARENA_EXIT.get()), "Sortie dans le hall");
        helper.assertTrue(!arena.getBlockState(ArenaWorld.LOBBY_SPAWN.below()).isAir(), "Sol sous l'apparition");
        boolean receptionist = !arena.getEntitiesOfClass(NpcEntity.class, new AABB(ArenaWorld.RECEPTIONIST).inflate(4),
                n -> n.npcId().equals("receptionist")).isEmpty();
        helper.assertTrue(receptionist, "L'hôtesse attend au guichet");
        ArenaWorld.ensureLobby(arena);
        int count = arena.getEntitiesOfClass(NpcEntity.class, new AABB(ArenaWorld.RECEPTIONIST).inflate(4),
                n -> n.npcId().equals("receptionist")).size();
        helper.assertTrue(count == 1, "Une seule hôtesse même si le hall est revérifié, obtenu " + count);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void floorIsBuiltWithOneReferee(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArenaWorld.buildFloor(level, 7);
        ArenaWorld.FloorLayout layout = ArenaWorld.buildFloor(level, 7);
        helper.assertTrue(level.getBlockState(ArenaWorld.floorCenter(7).below())
                        .is(ArenaThemes.forFloor(7).accent().getBlock()),
                "Le centre du ring porte le bloc d'accent du décor");
        int referees = level.getEntities(ModEntities.NPC.get(),
                n -> n.npcId().equals("referee") && n.home().equals(layout.npc())).size();
        helper.assertTrue(referees == 1, "Un seul arbitre même si l'étage est reconstruit, obtenu " + referees);
        helper.assertTrue(ArenaWorld.floorAt(layout.arrival()) == 7, "On retrouve l'étage depuis le palier");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void handBuiltFloorUsesMarkers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Décor de démonstration : la structure d'exemple livrée avec le mod
        ArenaTheme theme = ArenaThemes.forFloor(251);
        helper.assertTrue(theme.structure() != null, "Le décor d'exemple (étage 251) pointe vers une structure");
        ArenaWorld.FloorLayout layout = ArenaWorld.buildFloor(level, 248, theme);
        BlockPos c = ArenaWorld.floorCenter(248);
        helper.assertTrue(!layout.playerStart().equals(c.offset(-6, 0, 0)), "Départ du joueur lu sur son repère");
        helper.assertTrue(layout.npc() != null, "Repère du PNJ trouvé");
        helper.assertTrue(level.getBlockState(layout.playerStart()).isAir(), "Le repère est retiré après la pose");
        helper.assertTrue(!level.getBlockState(layout.playerStart().below()).isAir(), "Il y a un sol sous le départ du joueur");
        int referees = level.getEntities(ModEntities.NPC.get(),
                n -> n.npcId().equals("referee") && n.home().equals(layout.npc())).size();
        helper.assertTrue(referees == 1, "L'arbitre est posé sur son repère, obtenu " + referees);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void themesChangeWithFloors(GameTestHelper helper) {
        helper.assertTrue(ArenaThemes.count() >= 5, "Décors chargés depuis data/hxh/arena_themes, obtenu " + ArenaThemes.count());
        helper.assertTrue(!ArenaThemes.forFloor(5).id().equals(ArenaThemes.forFloor(210).id()),
                "Décor différent entre le 5e et le 210e étage");
        // Deux étages voisins d'une même tranche n'ont pas le même motif de sol
        var t = ArenaThemes.forFloor(10);
        boolean differs = false;
        for (int x = -8; x <= 8 && !differs; x++)
            for (int z = -8; z <= 8; z++)
                if (t.floorIndex(10, x, z) != t.floorIndex(11, x, z)) { differs = true; break; }
        helper.assertTrue(differs, "Le motif change d'un étage à l'autre");
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
    public static void elevatorLimits(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ArenaManager.maxFloor(player) == 0, "Sans inscription, seul le hall est accessible");
        QuestManager.data(player).setArenaRegistered(true);
        QuestManager.data(player).setArenaFloor(31);
        helper.assertTrue(ArenaManager.maxFloor(player) == 31, "Inscrit : jusqu'à son étage de combat");
        helper.assertTrue("message.hxh.arena_not_inside".equals(ArenaManager.goToFloor(player, 5)),
                "L'ascenseur ne marche que dans la Tour");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void elevatorRefusesOutsideTheTower(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue("message.hxh.arena_not_inside".equals(ArenaManager.startFight(player)),
                "Pas de combat hors de la Tour");
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
        helper.assertTrue(d.arenaRegistered(), "L'hôtesse inscrit le joueur");
        helper.assertTrue(QuestRegistry.HEAVENS_ARENA.toString().equals(d.active()) && d.step() == 1,
                "La quête du guichet démarre et la 1re étape est validée");

        int floor = ArenaManager.applyResult(player, 1, true, false);
        helper.assertTrue(floor == 11 && d.arenaFloor() == 11, "Victoire au 1er étage → 11e");
        helper.assertTrue(d.jenny() == ArenaRules.jennyForWin(1), "Jenny de la victoire");
        helper.assertTrue(d.step() == 2, "Étape « gagner un combat » validée");

        QuestManager.onTalk(player, hostess);
        helper.assertTrue(d.jenny() == ArenaRules.jennyForWin(1) + 500, "Prime de 500 Jenny au guichet");
        helper.assertTrue(d.step() == 3, "Étape suivante : atteindre le 50e");

        ArenaManager.applyResult(player, 11, false, false);
        helper.assertTrue(d.arenaFloor() == 6 && d.arenaLosses() == 1, "Défaite : on redescend de 5");
        ArenaManager.applyResult(player, 45, true, false);
        helper.assertTrue(d.arenaFloor() == 55 && d.step() == 4, "Le 50e étage atteint valide l'étape");
        helper.succeed();
    }
}
