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
        helper.assertTrue(exam.steps().size() == 9, "9 étapes attendues, obtenu " + exam.steps().size());
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
        NpcEntity cook = npc(helper, "cook", 1);
        Vec3 near = satotz.position();
        player.moveTo(near.x, near.y, near.z);
        QuestData d = QuestManager.data(player);

        QuestManager.onTalk(player, beans);
        helper.assertTrue(d.active().isEmpty(), "Parler à Beans d'abord ne lance rien");
        QuestManager.onTalk(player, navigator);
        helper.assertTrue(!d.active().isEmpty() && d.step() == 1, "Le Navigateur lance l'examen et valide la 1re étape");
        QuestManager.onTalk(player, cook);
        helper.assertTrue(d.step() == 2, "Le cuisinier de Zaban fait descendre au hall");
        QuestManager.onTalk(player, beans);
        helper.assertTrue(player.getInventory().contains(ModItems.EXAM_BADGE.get().getDefaultInstance()), "Plaque de candidat reçue");
        QuestManager.onTalk(player, satotz);
        helper.assertTrue(d.step() == 4, "Étape du marathon attendue");

        // Marathon : rester près de Satotz pendant 60 s
        for (int i = 0; i < 60 * 20 && d.step() == 4; i++) {
            player.moveTo(satotz.getX(), satotz.getY(), satotz.getZ());
            QuestManager.tick(player);
        }
        helper.assertTrue(d.step() == 5, "Le marathon aurait dû être réussi");

        QuestManager.onTalk(player, buhara);
        for (int i = 0; i < 2; i++) {
            Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
            QuestManager.onKill(player, pig);
        }
        helper.assertTrue(d.step() == 7, "La chasse aurait dû être validée");
        QuestManager.onTalk(player, buhara);
        QuestManager.onTalk(player, netero);
        helper.assertTrue(d.isHunter(), "Le joueur doit être Hunter");
        helper.assertTrue(d.isCompleted(QuestRegistry.HUNTER_EXAM.toString()) && d.active().isEmpty(), "Examen terminé");
        helper.assertTrue(player.getInventory().contains(ModItems.HUNTER_CARD.get().getDefaultInstance()), "Carte Hunter reçue");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fallingBehindFailsTheExam(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NpcEntity navigator = npc(helper, "navigator", 0);
        NpcEntity satotz = npc(helper, "satotz", 1);
        QuestData d = QuestManager.data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        QuestManager.onTalk(player, navigator);
        helper.assertTrue(d.isRunning(exam), "Le Navigateur lance l'examen");
        for (int i = 0; i < 3; i++) QuestManager.advance(player); // jusqu'au marathon
        helper.assertTrue(d.step() == 4, "Étape du marathon attendue");
        player.moveTo(satotz.getX(), satotz.getY(), satotz.getZ());
        for (int i = 0; i < 40; i++) QuestManager.tick(player);
        helper.assertTrue(d.isRunning(exam), "Tant qu'on suit, l'examen continue");
        // Distancé trop longtemps : échec
        player.moveTo(satotz.getX() + 60, satotz.getY(), satotz.getZ());
        int limit = com.evandouard.hxh.HxHConfig.EXAM_LOST_SECONDS.get() * 20;
        for (int i = 0; i < limit - 2; i++) QuestManager.tick(player);
        helper.assertTrue(d.isRunning(exam), "Un court retard est toléré");
        for (int i = 0; i < 4; i++) QuestManager.tick(player);
        helper.assertTrue(!d.isRunning(exam) && !d.isCompleted(exam), "Distancé trop longtemps : examen raté");
        helper.assertTrue(d.examRetryAt() > helper.getLevel().getGameTime(), "Il faut attendre avant de retenter");
        helper.assertTrue(d.examFailedCenter() == navigator.home().asLong(), "Le centre qui a recalé est retenu");

        // Trop tôt : le Navigateur refuse
        QuestManager.onTalk(player, navigator);
        helper.assertTrue(!d.isRunning(exam), "Pas de nouvel examen avant un jour Minecraft");
        // Un jour plus tard, le même centre refuse toujours, un autre accepte
        d.setExamRetryAt(0);
        QuestManager.onTalk(player, navigator);
        helper.assertTrue(!d.isRunning(exam), "Le même centre ne te reprend pas");
        NpcEntity otherCenter = npc(helper, "navigator", 3);
        QuestManager.onTalk(player, otherCenter);
        helper.assertTrue(d.isRunning(exam), "Un autre centre d'examen accepte");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void satotzFollowsHisCourse(GameTestHelper helper) {
        for (int x = 0; x <= 5; x++) for (int z = 0; z <= 6; z++)
            helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NpcEntity satotz = helper.spawn(ModEntities.NPC.get(), new BlockPos(1, 2, 1));
        satotz.setNpcId("satotz");
        satotz.setHome(satotz.blockPosition());
        satotz.setRoute(java.util.List.of(helper.absolutePos(new BlockPos(3, 2, 1)), helper.absolutePos(new BlockPos(3, 2, 4))));
        QuestData d = QuestManager.data(player);
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        for (int i = 0; i < 4; i++) QuestManager.advance(player); // marathon
        helper.onEachTick(() -> {
            if (d.step() != 4) return;
            player.moveTo(satotz.getX(), satotz.getY(), satotz.getZ());
            QuestManager.tick(player);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(d.step() == 5, "Satotz doit atteindre la fin de son parcours (point " + satotz.routeIndex() + ")");
        });
    }

    @GameTest(template = "empty")
    public static void questsRunInParallelAndCanBeTracked(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.quest.QuestData d = com.evandouard.hxh.quest.QuestManager.data(player);
        String exam = com.evandouard.hxh.quest.QuestRegistry.HUNTER_EXAM.toString();
        String arena = com.evandouard.hxh.quest.QuestRegistry.HEAVENS_ARENA.toString();
        com.evandouard.hxh.quest.QuestManager.start(player, com.evandouard.hxh.quest.QuestRegistry.HUNTER_EXAM);
        helper.assertTrue(com.evandouard.hxh.quest.QuestManager.start(player, com.evandouard.hxh.quest.QuestRegistry.HEAVENS_ARENA),
                "Une 2e quête peut démarrer pendant la 1re");
        helper.assertTrue(d.runningQuests().size() == 2, "Deux quêtes en cours");
        helper.assertTrue(exam.equals(d.active()), "La 1re quête reste suivie");

        // La quête de la Tour avance même si elle n'est pas suivie
        com.evandouard.hxh.npc.NpcEntity hostess = helper.spawn(com.evandouard.hxh.registry.ModEntities.NPC.get(), new net.minecraft.core.BlockPos(1, 1, 1));
        hostess.setNpcId("receptionist");
        hostess.setNoGravity(true);
        com.evandouard.hxh.quest.QuestManager.onTalk(player, hostess);
        helper.assertTrue(d.stateOf(arena)[0] == 1, "L'étape « parler à l'hôtesse » de la quête non suivie est validée");
        helper.assertTrue(exam.equals(d.active()) && d.step() == 0, "La quête suivie n'a pas bougé");

        helper.assertTrue(d.track(arena), "On peut suivre l'autre quête");
        helper.assertTrue(arena.equals(d.active()) && d.step() == 1, "Le curseur suit la quête de la Tour");
        helper.assertTrue(!d.track("hxh:inconnue"), "Impossible de suivre une quête qui n'est pas en cours");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void questFilesHaveNoProblems(GameTestHelper helper) {
        helper.assertTrue(QuestRegistry.problems().isEmpty(), "Erreurs dans les quêtes : " + QuestRegistry.problems());
        // Un fichier mal écrit est signalé
        Quest broken = Quest.parse(HxHMod.id("test_cassee"), com.google.gson.JsonParser.parseString(
                "{\"steps\":[{\"id\":\"a\",\"type\":\"voler\"},{\"id\":\"b\",\"type\":\"bring\",\"item\":\"minecraft:inexistant\"}]}"));
        helper.assertTrue(broken.problems().size() >= 3, "Problèmes repérés : " + broken.problems());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exampleQuestWithInlineTexts(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        net.minecraft.resources.ResourceLocation id = HxHMod.id("exemple_pecheur");
        Quest q = QuestRegistry.get(id);
        helper.assertTrue(q != null, "Quête d'exemple chargée");
        helper.assertTrue(q.title(player).getString().contains("pêcheur") || q.title(player).getString().contains("fisherman"),
                "Titre écrit dans le JSON : " + q.title(player).getString());
        helper.assertTrue(QuestRegistry.npcName("pecheur").getString().contains("cheur")
                || QuestRegistry.npcName("pecheur").getString().contains("isherman"), "Nom du PNJ déclaré dans la quête");

        // Parler au PNJ qui la donne la démarre
        NpcEntity fisher = npc(helper, "pecheur", 1);
        QuestManager.onTalk(player, fisher);
        helper.assertTrue(d.isRunning(id.toString()), "Le PNJ (start_npc) lance la quête");
        d.track(id.toString());

        // Apporter : pas assez → rien ; assez → objets pris et étape suivante
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD, 2));
        QuestManager.onTalk(player, fisher);
        helper.assertTrue(d.stateOf(id.toString())[0] == 0, "2 morues sur 3 : l'étape reste");
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COD, 2));
        QuestManager.onTalk(player, fisher);
        helper.assertTrue(d.stateOf(id.toString())[0] == 1, "3 morues apportées : étape suivante");
        helper.assertTrue(player.getInventory().countItem(net.minecraft.world.item.Items.COD) == 1, "Seules 3 morues sont prises");

        // Aller quelque part, puis étape « custom » validée par la commande / le futur code
        QuestManager.advance(player);
        helper.assertTrue(!QuestManager.completeCustom(player, HxHMod.id("inconnue")), "Quête absente : refus");
        helper.assertTrue(QuestManager.completeCustom(player, id), "Étape custom validée");
        helper.assertTrue(d.stateOf(id.toString())[0] == 3, "Dernière étape : revenir au pêcheur");
        QuestManager.onTalk(player, fisher);
        helper.assertTrue(d.isCompleted(id.toString()), "Quête terminée");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nenMustBeLearnedFromWing(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.data.HunterData h = com.evandouard.hxh.stats.HunterStats.get(player);
        com.evandouard.hxh.nen.NenModes.unlock(player);
        com.evandouard.hxh.nen.NenLearning.forceInTests = true;
        try {
            helper.assertTrue(com.evandouard.hxh.nen.NenModes.trySetMode(player, com.evandouard.hxh.data.NenMode.TEN) != null,
                    "Ten pas appris : refusé");
            helper.assertTrue(com.evandouard.hxh.nen.NenSenses.setGyo(player, true) != null, "Gyo pas appris : refusé");
            // Wing donne la première leçon ; l'éveil est déjà fait, l'étape suivante apprend le Ten
            NpcEntity wing = npc(helper, "wing", 1);
            QuestManager.onTalk(player, wing);
            QuestData d = QuestManager.data(player);
            net.minecraft.resources.ResourceLocation ten = HxHMod.id("nen_1_ten");
            helper.assertTrue(d.isRunning(ten.toString()), "Wing lance la leçon du Ten");
            d.track(ten.toString());
            player.tickCount = 20;
            QuestManager.tick(player); // éveil déjà fait : étape validée
            QuestManager.onTalk(player, wing);
            helper.assertTrue(h.learned("ten"), "Leçon écoutée : Ten appris");
            helper.assertTrue(com.evandouard.hxh.nen.NenModes.trySetMode(player, com.evandouard.hxh.data.NenMode.TEN) == null,
                    "Ten utilisable une fois appris");
            for (int s = 1; s <= 20; s++) {
                player.tickCount = 20 * (s + 1);
                QuestManager.tick(player);
            }
            QuestManager.onTalk(player, wing);
            helper.assertTrue(d.isCompleted(ten.toString()), "Ten tenu 20 s puis retour à Wing : leçon terminée");
            // /hunter max : tout est su
            com.evandouard.hxh.nen.NenLearning.learnAll(player);
            helper.assertTrue(com.evandouard.hxh.nen.NenSenses.setGyo(player, true) == null, "Tout appris : Gyo accepté");
            helper.assertTrue(QuestRegistry.problems().isEmpty(), "Leçons sans erreur : " + QuestRegistry.problems());
        } finally {
            com.evandouard.hxh.nen.NenLearning.forceInTests = false;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void examSitesAreBuiltWithNpcsAndCourse(GameTestHelper helper) {
        // Le serveur de tests n'a pas toujours les dimensions du mod : on construit alors dans son monde
        net.minecraft.server.level.ServerLevel exam = com.evandouard.hxh.exam.ExamWorld.level(helper.getLevel().getServer());
        if (exam == null) exam = helper.getLevel();
        com.evandouard.hxh.exam.ExamWorld.Site hall = com.evandouard.hxh.exam.ExamWorld.ensure(exam, com.evandouard.hxh.exam.ExamSite.HALL);
        helper.assertTrue(hall.npcs().containsKey("beans") && hall.npcs().containsKey("satotz"), "Beans et Satotz dans le hall");
        helper.assertTrue(hall.route().size() >= 5, "Parcours du marathon dans le tunnel : " + hall.route().size() + " points");
        helper.assertTrue(!exam.getBlockState(hall.arrival().below()).isAir(), "On arrive sur un sol");
        com.evandouard.hxh.exam.ExamWorld.Site biska = com.evandouard.hxh.exam.ExamWorld.ensure(exam, com.evandouard.hxh.exam.ExamSite.BISKA);
        helper.assertTrue(biska.npcs().containsKey("buhara") && !biska.prey().isEmpty(), "Buhara et des proies à Biska");
        for (com.evandouard.hxh.exam.ExamSite s : com.evandouard.hxh.exam.ExamSite.values())
            helper.assertTrue(!com.evandouard.hxh.exam.ExamWorld.ensure(exam, s).npcs().isEmpty(), "PNJ présents : " + s.id());
        helper.succeed();
    }
}
