package com.evandouard.hxh.gametest;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.quest.QuestRegistry;
import com.evandouard.hxh.registry.ModEntities;
import com.evandouard.hxh.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Examen Hunter v1 joué de bout en bout par un joueur factice : dialogues, plaque, marathon derrière Satotz,
 * chasse pour Buhara, licence remise par Netero.
 */
@GameTestHolder(HxHMod.MODID)
@PrefixGameTestTemplate(false)
public final class QuestTests {

    private static NpcEntity npc(GameTestHelper helper, String id, int x) {
        NpcEntity npc = helper.spawn(ModEntities.NPC.get(), new BlockPos(x, 1, 1));
        npc.setNpcId(id);
        npc.setNoGravity(true);
        npc.setHome(npc.blockPosition());
        return npc;
    }

    @GameTest(template = "empty")
    public static void examQuestIsLoaded(GameTestHelper helper) {
        Quest exam = QuestRegistry.get(QuestRegistry.HUNTER_EXAM);
        helper.assertTrue(exam != null, "La quête hxh:hunter_exam doit être chargée depuis le datapack");
        helper.assertTrue(exam.steps().size() == 8, "8 étapes attendues, obtenu " + exam.steps().size());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hunterExamFromStartToLicense(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NpcEntity navigator = npc(helper, "navigator", 0);
        NpcEntity beans = npc(helper, "beans", 1);
        NpcEntity satotz = npc(helper, "satotz", 2);
        NpcEntity buhara = npc(helper, "buhara", 0);
        NpcEntity netero = npc(helper, "netero", 2);
        Vec3 near = satotz.position();
        player.moveTo(near.x, near.y, near.z);
        QuestData d = QuestManager.data(player);

        QuestManager.onTalk(player, beans);
        helper.assertTrue(d.active().isEmpty(), "Parler à Beans d'abord ne lance rien");
        QuestManager.onTalk(player, navigator);
        helper.assertTrue(!d.active().isEmpty() && d.step() == 1, "Le Navigateur lance l'examen et valide la 1re étape");
        QuestManager.onTalk(player, beans);
        helper.assertTrue(player.getInventory().contains(ModItems.EXAM_BADGE.get().getDefaultInstance()), "Plaque de candidat reçue");
        QuestManager.onTalk(player, satotz);
        helper.assertTrue(d.step() == 3, "Étape du marathon attendue");

        // Marathon : rester près de Satotz pendant 60 s
        for (int i = 0; i < 60 * 20 && d.step() == 3; i++) {
            player.moveTo(satotz.getX(), satotz.getY(), satotz.getZ());
            QuestManager.tick(player);
        }
        helper.assertTrue(d.step() == 4, "Le marathon aurait dû être réussi");

        QuestManager.onTalk(player, buhara);
        for (int i = 0; i < 2; i++) {
            Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
            QuestManager.onKill(player, pig);
        }
        helper.assertTrue(d.step() == 6, "La chasse aurait dû être validée");
        QuestManager.onTalk(player, buhara);
        QuestManager.onTalk(player, netero);
        helper.assertTrue(d.isHunter(), "Le joueur doit être Hunter");
        helper.assertTrue(d.isCompleted(QuestRegistry.HUNTER_EXAM.toString()) && d.active().isEmpty(), "Examen terminé");
        helper.assertTrue(player.getInventory().contains(ModItems.HUNTER_CARD.get().getDefaultInstance()), "Carte Hunter reçue");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fallingBehindResetsMarathon(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NpcEntity satotz = npc(helper, "satotz", 1);
        QuestData d = QuestManager.data(player);
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        for (int i = 0; i < 3; i++) QuestManager.advance(player); // jusqu'au marathon
        player.moveTo(satotz.getX(), satotz.getY(), satotz.getZ());
        for (int i = 0; i < 100; i++) QuestManager.tick(player);
        helper.assertTrue(d.progress() > 0, "La progression du marathon doit avancer");
        player.moveTo(satotz.getX() + 40, satotz.getY(), satotz.getZ());
        QuestManager.tick(player);
        helper.assertTrue(d.progress() == 0, "Distancé : le marathon repart de zéro");
        helper.succeed();
    }
}
