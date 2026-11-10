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

    /** Rang d'une étape de l'Examen (les épreuves s'ajoutent au fil du temps : on ne compte pas en dur). */
    private static int idx(String id) {
        java.util.List<Quest.Step> steps = QuestRegistry.get(QuestRegistry.HUNTER_EXAM).steps();
        for (int i = 0; i < steps.size(); i++) if (steps.get(i).id().equals(id)) return i;
        throw new IllegalStateException("Étape inconnue : " + id);
    }

    @GameTest(template = "empty")
    public static void examQuestIsLoaded(GameTestHelper helper) {
        Quest exam = QuestRegistry.get(QuestRegistry.HUNTER_EXAM);
        helper.assertTrue(exam != null, "La quête hxh:hunter_exam doit être chargée depuis le datapack");
        helper.assertTrue(exam.steps().size() == 21, "21 étapes attendues, obtenu " + exam.steps().size());
        helper.assertTrue(exam.trials().size() == 8 && exam.minTrials() == 4 && exam.maxTrials() == 6, "8 épreuves, 4 à 6 tirées");
        helper.succeed();
    }

    /** Toutes les épreuves, dans l'ordre du fichier (le parcours est alors identique au fichier). */
    private static final java.util.List<String> ALL_TRIALS = java.util.List.of("dolle", "zaban", "numele", "biska", "menchi", "balle", "zevil", "tournoi");

    @GameTest(template = "empty")
    public static void trialDrawsStayCoherent(GameTestHelper helper) {
        Quest exam = QuestRegistry.get(QuestRegistry.HUNTER_EXAM);
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(42);
        java.util.Set<java.util.List<String>> seen = new java.util.HashSet<>();
        for (int n = 0; n < 300; n++) {
            java.util.List<String> t = com.evandouard.hxh.quest.TrialPlan.draw(exam, random);
            seen.add(t);
            helper.assertTrue(t.size() >= 4 && t.size() <= 6, "4 à 6 épreuves : " + t);
            helper.assertTrue(new java.util.HashSet<>(t).size() == t.size(), "Pas deux fois la même épreuve : " + t);
            helper.assertTrue(t.get(t.size() - 1).equals("tournoi") && t.get(t.size() - 2).equals("zevil"),
                    "Toujours les badges de Zevil puis le tournoi final : " + t);
            int lastPhase = -1;
            for (int i = 0; i < t.size(); i++) {
                Quest.Trial tr = exam.trials().get(t.get(i));
                if (!tr.follows().isEmpty())
                    helper.assertTrue(i > 0 && t.get(i - 1).equals(tr.follows()), tr.id() + " doit suivre " + tr.follows() + " : " + t);
                int phase = tr.follows().isEmpty() ? tr.phase() : exam.trials().get(tr.follows()).phase();
                helper.assertTrue(phase >= lastPhase, "Phases dans l'ordre : " + t);
                lastPhase = phase;
            }
        }
        helper.assertTrue(seen.size() >= 5, "Les tirages varient : " + seen.size() + " parcours différents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void firstQuestIsBecomingAHunter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        QuestManager.startFirstQuest(player);
        helper.assertTrue(d.isRunning(QuestRegistry.HUNTER_EXAM.toString()), "L'Examen démarre dès l'arrivée");
        helper.assertTrue(QuestManager.currentStep(d).id().equals("guide"), "Première étape : trouver un guide");
        int trials = d.planTrials(QuestRegistry.HUNTER_EXAM.toString()).size();
        helper.assertTrue(trials >= 4 && trials <= 6, "Parcours tiré : " + trials + " épreuves");
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
        NpcEntity captain = npc(helper, "captain", 0);
        NpcEntity granny = npc(helper, "quiz_granny", 1);
        NpcEntity menchi = npc(helper, "menchi", 2);
        Vec3 near = satotz.position();
        player.moveTo(near.x, near.y, near.z);
        QuestData d = QuestManager.data(player);

        QuestManager.onTalk(player, beans);
        helper.assertTrue(d.active().isEmpty(), "Parler à Beans d'abord ne lance rien");
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = ALL_TRIALS;
        QuestManager.onTalk(player, navigator);
        helper.assertTrue(!d.active().isEmpty() && d.step() == idx("captain"), "Le guide envoie au bateau");
        helper.assertTrue(player.getInventory().contains(ModItems.EXAM_BADGE.get().getDefaultInstance()), "Le guide remet la plaque de candidat");
        // Le capitaine : toute réponse convient
        QuestManager.onTalk(player, captain);
        helper.assertTrue(QuestManager.answer(player, 2), "Le capitaine accepte toute réponse");
        helper.assertTrue(d.step() == idx("storm"), "Puis la tempête");
        for (int i = 0; i < 40 * 20 && d.step() == idx("storm"); i++) QuestManager.tick(player);
        helper.assertTrue(d.step() == idx("granny"), "La tempête passée, la Mamie de Dolle");
        // La Mamie : se taire 5 s, c'est gagner (répondre élimine : voir wrongAnswerAtTheExamEliminates)
        QuestManager.onTalk(player, granny);
        for (int i = 0; i < 6 * 20 && d.step() == idx("granny"); i++) QuestManager.tick(player);
        helper.assertTrue(d.step() == idx("cook"), "Le silence était la bonne réponse");

        QuestManager.onTalk(player, cook);
        helper.assertTrue(d.step() == idx("register"), "Le cuisinier de Zaban fait descendre au hall");
        QuestManager.onTalk(player, beans);
        helper.assertTrue(d.step() == idx("satotz"), "Inscription auprès de Beans");
        QuestManager.onTalk(player, satotz);
        helper.assertTrue(d.step() == idx("marathon"), "Étape du marathon attendue");
        // Marathon puis marais de Numele : rester près de Satotz
        for (String leg : java.util.List.of("marathon", "numele")) {
            for (int i = 0; i < 60 * 20 && d.step() == idx(leg); i++) {
                player.moveTo(satotz.getX(), satotz.getY(), satotz.getZ());
                QuestManager.tick(player);
            }
            helper.assertTrue(d.step() == idx(leg) + 1, "Étape réussie : " + leg);
        }

        QuestManager.onTalk(player, buhara);
        for (int i = 0; i < 2; i++) {
            Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
            QuestManager.onKill(player, pig);
        }
        helper.assertTrue(d.step() == idx("buhara_done"), "La chasse aurait dû être validée");
        QuestManager.onTalk(player, buhara);
        // Menchi : le sushi est refusé (pour tout le monde), Netero propose le mont Split
        player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SALMON));
        QuestManager.onTalk(player, menchi);
        helper.assertTrue(d.step() == idx("netero_split"), "Sushi apporté à Menchi");
        QuestManager.onTalk(player, netero);
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.SPIDER_EAGLE_EGG.get()));
        QuestManager.onTalk(player, menchi);
        helper.assertTrue(d.step() == idx("ball_intro"), "Œuf d'aigle-araignée accepté");
        // Jeu de la balle dans le dirigeable : Netero esquive, puis on le touche trois fois
        QuestManager.onTalk(player, netero);
        int jenny = d.jenny();
        com.evandouard.hxh.exam.NeteroBall.touch(player, netero, true);
        helper.assertTrue(d.progress() == 0, "Une esquive ne compte pas");
        for (int i = 0; i < 3; i++) com.evandouard.hxh.exam.NeteroBall.touch(player, netero, false);
        helper.assertTrue(d.step() == idx("zevil") && d.jenny() == jenny + 500, "Trois touches : prime de Netero");
        helper.assertTrue(player.getInventory().contains(ModItems.NETERO_BALL.get().getDefaultInstance()), "La balle en souvenir");
        // Zevil : sa plaque (3) + celle de sa cible (3)
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.TARGET_PLATE.get()));
        player.tickCount = 0;
        QuestManager.tick(player);
        helper.assertTrue(d.step() == idx("tournament"), "6 points de badges");
        // Tournoi final : Netero l'annonce, puis deux adversaires abandonnent
        QuestManager.onTalk(player, netero);
        helper.assertTrue(d.step() == idx("duels"), "Le tournoi commence");
        QuestManager.duelWon(player);
        helper.assertTrue(d.step() == idx("duels") && d.progress() == 1, "Une victoire sur deux");
        QuestManager.duelWon(player);
        helper.assertTrue(d.step() == idx("netero"), "Tournoi gagné");
        QuestManager.onTalk(player, netero);
        helper.assertTrue(d.isHunter(), "Le joueur doit être Hunter");
        helper.assertTrue(d.isCompleted(QuestRegistry.HUNTER_EXAM.toString()) && d.active().isEmpty(), "Examen terminé");
        helper.assertTrue(player.getInventory().contains(ModItems.HUNTER_CARD.get().getDefaultInstance()), "Carte Hunter reçue");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void platePointsCountLikeOnZevil(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var inv = player.getInventory();
        inv.add(new net.minecraft.world.item.ItemStack(ModItems.EXAM_PLATE.get(), 2));
        helper.assertTrue(com.evandouard.hxh.exam.ExamHazards.platePoints(player) == 2, "Deux plaques ordinaires : 2 points");
        inv.add(new net.minecraft.world.item.ItemStack(ModItems.EXAM_BADGE.get()));
        inv.add(new net.minecraft.world.item.ItemStack(ModItems.TARGET_PLATE.get()));
        helper.assertTrue(com.evandouard.hxh.exam.ExamHazards.platePoints(player) == 8, "Sa plaque et celle de sa cible valent 3");
        // Un candidat de Zevil lâche sa plaque à coup sûr
        var v = com.evandouard.hxh.exam.ExamHazards.candidateForTests(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), true);
        helper.assertTrue(v != null && v.getOffhandItem().is(ModItems.TARGET_PLATE.get()) && v.getTags().contains("hxh_target"),
                "La cible tient la plaque de cible");
        v.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fallingBehindFailsTheExam(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NpcEntity navigator = npc(helper, "navigator", 0);
        NpcEntity satotz = npc(helper, "satotz", 1);
        QuestData d = QuestManager.data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("zaban", "biska", "menchi", "zevil");
        QuestManager.onTalk(player, navigator);
        helper.assertTrue(d.isRunning(exam), "Le guide lance l'examen");
        helper.assertTrue(d.examInstance("hall") == 0, "Premier essai : premier exemplaire du hall");
        while (!QuestManager.currentStep(d).id().equals("marathon")) QuestManager.advance(player);
        player.moveTo(satotz.getX(), satotz.getY(), satotz.getZ());
        for (int i = 0; i < 40; i++) QuestManager.tick(player);
        helper.assertTrue(QuestManager.currentStep(d).id().equals("marathon"), "Tant qu'on suit, l'épreuve continue");
        // Distancé trop longtemps : échec, retour à « trouver un guide » avec un nouveau tirage
        player.moveTo(satotz.getX() + 60, satotz.getY(), satotz.getZ());
        int limit = com.evandouard.hxh.HxHConfig.EXAM_LOST_SECONDS.get() * 20;
        for (int i = 0; i < limit - 2; i++) QuestManager.tick(player);
        helper.assertTrue(QuestManager.currentStep(d).id().equals("marathon"), "Un court retard est toléré");
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("dolle", "zaban", "biska", "zevil");
        for (int i = 0; i < 4; i++) QuestManager.tick(player);
        helper.assertTrue(d.isRunning(exam) && QuestManager.currentStep(d).id().equals("guide"), "Échec : il faut retrouver un guide");
        helper.assertTrue(d.examInstance("hall") == 1 && d.examInstance("zaban") == 1, "Le tunnel déjà vu est remplacé par un autre exemplaire");
        helper.assertTrue(d.examInstance("boat") == 0, "Un lieu jamais vu : premier exemplaire");
        // Le guide reprend le candidat tout de suite, vers les nouvelles épreuves
        QuestManager.onTalk(player, navigator);
        helper.assertTrue(QuestManager.currentStep(d).id().equals("captain"), "Nouveau parcours : le bateau pour Dolle");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tournamentGivesASecondChanceThenEliminates(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("zevil", "tournoi");
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        while (!QuestManager.currentStep(d).id().equals("duels")) QuestManager.advance(player);
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        QuestManager.tick(player);
        player.tickCount = 0;
        QuestManager.tick(player);
        var opponent = com.evandouard.hxh.exam.ExamTournament.opponent(player);
        helper.assertTrue(opponent != null && opponent.getTags().contains("hxh_duel"), "Un adversaire entre en lice");
        com.evandouard.hxh.exam.ExamTournament.opponentGivesUp(player, opponent);
        helper.assertTrue(d.progress() == 1, "L'adversaire abandonne : une victoire");
        com.evandouard.hxh.exam.ExamTournament.playerGivesUp(player);
        helper.assertTrue(QuestManager.currentStep(d).id().equals("duels"), "Premier abandon : repêché");
        com.evandouard.hxh.exam.ExamTournament.playerGivesUp(player);
        helper.assertTrue(QuestManager.currentStep(d).id().equals("guide"), "Second abandon : Examen raté, retour au guide");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void guideGivesADepartureTheFerrymanBoards(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("dolle", "zevil", "tournoi");
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        QuestManager.advance(player);
        helper.assertTrue(com.evandouard.hxh.exam.ExamDeparture.pendingSite(d) == com.evandouard.hxh.exam.ExamSite.BOAT, "Départ vers le bateau");
        NpcEntity near = npc(helper, "passeur", 1);
        NpcEntity far = npc(helper, "passeur", 2);
        far.setHome(far.blockPosition().offset(100, 0, 0));
        d.setExamDeparture(near.home().asLong());
        helper.assertTrue(!com.evandouard.hxh.exam.ExamDeparture.talk(player, far), "Une autre hôtesse ne t'attend pas");
        helper.assertTrue(com.evandouard.hxh.exam.ExamDeparture.talk(player, near), "Ton hôtesse te propose d'embarquer");
        helper.assertTrue(com.evandouard.hxh.exam.ExamDeparture.board(player), "Embarquement accepté");
        helper.assertTrue(d.examDeparture() == Long.MIN_VALUE && d.examUses("boat") == 1, "Départ utilisé, bateau visité");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void neteroBallIsNotEliminatory(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("balle", "zevil", "tournoi");
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        while (!QuestManager.currentStep(d).id().equals("ball")) QuestManager.advance(player);
        for (int i = 0; i < 61 * 20 && QuestManager.currentStep(d).id().equals("ball"); i++) QuestManager.tick(player);
        helper.assertTrue(QuestManager.currentStep(d).id().equals("zevil"), "Temps écoulé : on continue sans élimination");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wrongAnswerAtTheExamEliminates(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NpcEntity navigator = npc(helper, "navigator", 0);
        NpcEntity captain = npc(helper, "captain", 1);
        NpcEntity granny = npc(helper, "quiz_granny", 2);
        QuestData d = QuestManager.data(player);
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("dolle", "zaban", "biska", "zevil");
        QuestManager.onTalk(player, navigator);
        QuestManager.onTalk(player, captain);
        QuestManager.answer(player, 3);
        while (!QuestManager.currentStep(d).id().equals("granny")) QuestManager.advance(player);
        QuestManager.onTalk(player, granny);
        helper.assertTrue(!QuestManager.answer(player, 1), "Répondre à la Mamie est une erreur");
        helper.assertTrue(QuestManager.currentStep(d).id().equals("guide"), "Éliminé : retour au guide");
        helper.assertTrue(d.examInstance("boat") == 1 || !d.planTrials(QuestRegistry.HUNTER_EXAM.toString()).contains("dolle"),
                "Un autre bateau si Dolle revient au tirage");
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
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = ALL_TRIALS;
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        while (d.step() < idx("marathon")) QuestManager.advance(player);
        int marathon = idx("marathon");
        helper.onEachTick(() -> {
            if (d.step() != marathon) return;
            player.moveTo(satotz.getX(), satotz.getY(), satotz.getZ());
            QuestManager.tick(player);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(d.step() == marathon + 1, "Satotz doit atteindre la fin de son parcours (point " + satotz.routeIndex() + ")");
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
        com.evandouard.hxh.exam.ExamWorld.Site split = com.evandouard.hxh.exam.ExamWorld.ensure(exam, com.evandouard.hxh.exam.ExamSite.MOUNT_SPLIT);
        helper.assertTrue(split.prey().size() >= 3, "Des nids d'œufs dans la faille du mont Split");
        helper.assertTrue(exam.getBlockState(split.prey().get(0).below()).is(net.minecraft.world.level.block.Blocks.COBWEB), "Les œufs pendent sur des fils");
        com.evandouard.hxh.exam.ExamWorld.Site zevil = com.evandouard.hxh.exam.ExamWorld.ensure(exam, com.evandouard.hxh.exam.ExamSite.ZEVIL);
        helper.assertTrue(zevil.prey().size() >= 3, "Des candidats à traquer sur Zevil");
        helper.succeed();
    }
}
