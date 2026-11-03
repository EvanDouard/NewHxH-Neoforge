package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
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
        ServerLevel arena = ArenaWorld.level(helper.getLevel().getServer());
        helper.assertTrue(arena != null, "La dimension hxh:heavens_arena doit exister");
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
        ArenaWorld.buildArena(arena, 3);
        helper.assertTrue(arena.getBlockState(ArenaWorld.arenaCenter(3).below()).is(Blocks.CHISELED_STONE_BRICKS),
                "Plateforme de l'arène construite");
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
