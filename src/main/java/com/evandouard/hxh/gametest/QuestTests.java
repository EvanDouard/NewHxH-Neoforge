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
        helper.assertTrue(exam.steps().size() == 34, "34 étapes attendues, obtenu " + exam.steps().size());
        helper.assertTrue(exam.trials().size() == 12 && exam.minTrials() == 4 && exam.maxTrials() == 6, "12 épreuves, 4 à 6 tirées");
        helper.succeed();
    }

    /** Toutes les épreuves, dans l'ordre du fichier (le parcours est alors identique au fichier). */
    private static final java.util.List<String> ALL_TRIALS = java.util.List.of("dolle", "kiriko", "zaban", "numele", "ko288", "biska", "menchi", "balle", "tour", "ile", "zevil", "tournoi");

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
        for (int i = 0; i < 14 * 20 && d.step() == idx("granny"); i++) QuestManager.tick(player);
        helper.assertTrue(d.step() == idx("kiriko_intro"), "Le silence était la bonne réponse");
        // Forêt des Kiriko : examiner les deux maris, puis désigner la bête
        NpcEntity wife = npc(helper, "kiriko_wife", 0);
        NpcEntity husband1 = npc(helper, "husband_1", 1);
        NpcEntity husband2 = npc(helper, "husband_2", 2);
        QuestManager.onTalk(player, wife);
        QuestManager.onTalk(player, husband1);
        QuestManager.onTalk(player, husband2);
        helper.assertTrue(d.step() == idx("kiriko_accuse"), "Parler aux maris ne valide rien : il faut désigner");
        QuestManager.onTalk(player, wife);
        helper.assertTrue(QuestManager.answer(player, com.evandouard.hxh.exam.KirikoForest.fakeIndex(player)), "Le Kiriko démasqué");
        helper.assertTrue(d.step() == idx("cook"), "En route pour Zaban");

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
        // 288e Examen : Beans, puis assommer les dix candidats
        QuestManager.onTalk(player, beans);
        helper.assertTrue(d.step() == idx("ko"), "La meute arrive");
        for (int i = 0; i < 10; i++) QuestManager.knockedOut(player);
        helper.assertTrue(d.step() == idx("buhara"), "Plus personne debout");

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
        helper.assertTrue(d.step() == idx("tower_intro") && d.jenny() == jenny + 500, "Trois touches : prime de Netero");
        helper.assertTrue(!player.getInventory().contains(ModItems.NETERO_BALL.get().getDefaultInstance()), "La balle reste à Netero");
        // Tour de Trick : Lippo, la trappe, le vote de la majorité, un prisonnier, la sortie
        helper.assertTrue(d.step() == idx("tower_intro"), "Puis la Tour de Trick");
        NpcEntity lippo = npc(helper, "lippo", 0);
        NpcEntity tonpa = npc(helper, "tonpa", 1);
        QuestManager.onTalk(player, lippo);
        helper.assertTrue(d.step() == idx("tower_roof"), "Lippo annonce l'épreuve");
        QuestManager.advance(player); // la trappe (atteindre l'étage : voir trickTowerHasOneRealTrapdoor)
        QuestManager.onTalk(player, tonpa);
        helper.assertTrue(QuestManager.answer(player, 1), "Le vote est toujours accepté");
        helper.assertTrue(d.step() == idx("tower_fight"), "Puis le prisonnier");
        QuestManager.duelWon(player);
        helper.assertTrue(d.step() == idx("tower_exit"), "Prisonnier vaincu");
        QuestManager.advance(player);
        helper.assertTrue(d.step() == idx("island_manager"), "Sortie de la tour");
        // Île-hôtel : payer la chambre avec les pièces de l'épave, tenir face à la vague
        NpcEntity manager = npc(helper, "hotel_manager", 0);
        QuestManager.onTalk(player, manager);
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.SUNKEN_COIN.get(), 3));
        QuestManager.onTalk(player, manager);
        helper.assertTrue(d.step() == idx("island_wave"), "Chambre payée");
        for (int i = 0; i < 31 * 20 && d.step() == idx("island_wave"); i++) QuestManager.tick(player);
        QuestManager.onTalk(player, manager);
        helper.assertTrue(d.step() == idx("zevil"), "La vague passée");
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

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void followTrialDoesNotFailBeforeBoarding(GameTestHelper helper) {
        // Marais de Numele (suivre Satotz) : tant que le candidat marche vers son dirigeable, il n'est pas « distancé »
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("numele", "zevil", "tournoi");
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        QuestManager.advance(player);
        // le guide donne le point de départ (dirigeable loin d'ici)
        d.setExamDeparture(player.blockPosition().offset(80, 0, 0).asLong());
        helper.assertTrue(QuestManager.awaitingDeparture(player, d), "Candidat pas encore embarqué");
        int limit = com.evandouard.hxh.HxHConfig.EXAM_LOST_SECONDS.get() * 20;
        for (int i = 0; i < limit + 40; i++) QuestManager.tick(player);
        helper.assertTrue(d.examDeparture() != Long.MIN_VALUE && d.step() > 0, "Pas d'échec avant d'avoir pris le dirigeable");
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
        for (int i = 0; i < 36 * 20 && QuestManager.currentStep(d).id().equals("ball"); i++) QuestManager.tick(player);
        helper.assertTrue(QuestManager.currentStep(d).id().equals("zevil"), "Temps écoulé : on continue sans élimination");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trickTowerHasOneRealTrapdoor(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel exam = com.evandouard.hxh.exam.ExamWorld.level(helper.getLevel().getServer());
        if (exam == null) exam = helper.getLevel();
        com.evandouard.hxh.exam.ExamWorld.Site tower = com.evandouard.hxh.exam.ExamWorld.ensure(exam, com.evandouard.hxh.exam.ExamSite.TOWER);
        helper.assertTrue(tower.npcs().containsKey("lippo") && tower.npcs().containsKey("tonpa") && tower.route().size() == 4,
                "Lippo, Tonpa, et quatre points (étage, 2e étage, sortie, long chemin)");
        net.minecraft.core.BlockPos longStart = tower.route().get(3);
        helper.assertTrue(exam.getBlockState(longStart).isAir() && !exam.getBlockState(longStart.below()).isAir(), "Le long chemin est praticable");
        net.minecraft.core.BlockPos c = com.evandouard.hxh.exam.ExamWorld.center(com.evandouard.hxh.exam.ExamSite.TOWER);
        int open = 0;
        for (int[] t : new int[][]{{-4, -4}, {4, -4}, {0, 0}, {-4, 3}, {4, 3}}) {
            net.minecraft.core.BlockPos trap = c.offset(t[0], 39, t[1]);
            helper.assertTrue(exam.getBlockState(trap).getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock, "Trappe au toit");
            if (exam.getBlockState(trap.below()).isAir()) open++;
        }
        helper.assertTrue(open == 1, "Une seule trappe s'ouvre sur le vide : " + open);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.minecraft.core.BlockPos pool = tower.route().get(0);
        player.moveTo(pool.getX() + 0.5, pool.getY(), pool.getZ() + 0.5);
        if (player.level() == exam) helper.assertTrue(com.evandouard.hxh.exam.TrickTower.reached(player, 0), "Le bassin sous la trappe compte");
        helper.assertTrue(!com.evandouard.hxh.exam.TrickTower.reached(player, 2) || player.level() != exam, "La sortie est ailleurs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lerouteBetsTimeInsteadOfFighting(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("tour", "zevil", "tournoi");
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        while (!QuestManager.currentStep(d).id().equals("tower_roof")) QuestManager.advance(player);
        helper.assertTrue(com.evandouard.hxh.exam.TrickTower.elapsedTicks(player) >= 0, "L'horloge de la tour tourne depuis Lippo");
        while (!QuestManager.currentStep(d).id().equals("tower_fight")) QuestManager.advance(player);
        com.evandouard.hxh.exam.TrickTower.forcedForTests = com.evandouard.hxh.exam.TrickTower.Prisoner.LEROUTE;
        try {
            player.tickCount = 0;
            QuestManager.tick(player);
            helper.assertTrue(com.evandouard.hxh.exam.ExamTournament.opponent(player) == null, "Leroute ne se bat pas");
            for (int i = 0; i < 3; i++) {
                helper.assertTrue(QuestManager.currentStep(d).id().equals("tower_fight"), "Pari " + (i + 1));
                player.tickCount = 0;
                QuestManager.tick(player);
                com.evandouard.hxh.exam.TrickTower.bet(player, 1);
            }
            int lost = com.evandouard.hxh.exam.TrickTower.penaltySeconds(player);
            helper.assertTrue(QuestManager.currentStep(d).id().equals("tower_exit"), "Trois paris : Leroute laisse passer");
            helper.assertTrue(lost % 25 == 0 && lost <= 75, "Chaque pari perdu coûte 25 s : " + lost);
        } finally {
            com.evandouard.hxh.exam.TrickTower.forcedForTests = null;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void accusingTheRealHusbandEliminates(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        QuestData d = QuestManager.data(player);
        NpcEntity wife = npc(helper, "kiriko_wife", 0);
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("dolle", "kiriko", "zevil", "tournoi");
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        while (!QuestManager.currentStep(d).id().equals("kiriko_accuse")) QuestManager.advance(player);
        QuestManager.onTalk(player, wife);
        int real = 3 - com.evandouard.hxh.exam.KirikoForest.fakeIndex(player);
        helper.assertTrue(!QuestManager.answer(player, real), "Désigner le vrai mari est une erreur");
        helper.assertTrue(QuestManager.currentStep(d).id().equals("guide"), "Éliminé : retour au guide");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exam288PackMustAllFallInTime(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        QuestData d = QuestManager.data(player);
        com.evandouard.hxh.quest.TrialPlan.forcedForTests = java.util.List.of("ko288", "zevil", "tournoi");
        QuestManager.start(player, QuestRegistry.HUNTER_EXAM);
        while (!QuestManager.currentStep(d).id().equals("ko")) QuestManager.advance(player);
        QuestManager.tick(player);
        helper.assertTrue(com.evandouard.hxh.exam.Exam288.remaining(player) == 10, "Dix candidats entrent");
        var one = player.serverLevel().getEntitiesOfClass(com.evandouard.hxh.exam.ExamCandidate.class,
                player.getBoundingBox().inflate(16), v -> v.getTags().contains(com.evandouard.hxh.exam.Exam288.TAG)).get(0);
        com.evandouard.hxh.exam.Exam288.knockOut(player, one);
        helper.assertTrue(d.progress() == 1 && com.evandouard.hxh.exam.Exam288.remaining(player) == 9, "Un assommé (pas tué)");
        for (int i = 0; i < 61 * 20 && QuestManager.currentStep(d).id().equals("ko"); i++) QuestManager.tick(player);
        helper.assertTrue(QuestManager.currentStep(d).id().equals("guide"), "Temps écoulé : éliminé");
        helper.assertTrue(com.evandouard.hxh.exam.Exam288.remaining(player) == 0, "La meute est repartie");
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
        com.evandouard.hxh.exam.ExamWorld.Site island = com.evandouard.hxh.exam.ExamWorld.ensure(exam, com.evandouard.hxh.exam.ExamSite.ISLAND_HOTEL);
        helper.assertTrue(island.prey().size() >= 3 && island.prey().get(0).getY() < com.evandouard.hxh.exam.ExamWorld.FLOOR_Y - 4,
                "Des pièces d'or au fond de la baie");
        com.evandouard.hxh.exam.ExamWorld.Site zevil = com.evandouard.hxh.exam.ExamWorld.ensure(exam, com.evandouard.hxh.exam.ExamSite.ZEVIL);
        helper.assertTrue(zevil.prey().size() >= 3, "Des candidats à traquer sur Zevil");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void neteroQuestUnlocksHealing(GameTestHelper helper) {
        // Quête de Netero : prière de gratitude (coups de base en Ten), paumes de Kannon (jamais fatales),
        // méditation blessé, puis le soin par le Nen
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var h = com.evandouard.hxh.stats.HunterStats.get(player);
        com.evandouard.hxh.nen.NenModes.unlock(player);
        h.setNenLevel(60);
        h.setAura(h.auraMax());
        helper.assertTrue(QuestRegistry.get(HxHMod.id("netero_soin")) != null, "Quête de Netero chargée");
        helper.assertTrue(QuestRegistry.get(HxHMod.id("netero_soin")).problems().isEmpty(), "Quête valide");
        QuestData d = QuestManager.data(player);
        QuestManager.start(player, HxHMod.id("netero_soin"));
        QuestManager.advance(player); // a écouté Netero
        // hors Ten, un coup de base ne compte pas
        QuestManager.onBaseStrike(player);
        helper.assertTrue(d.progress() == 0, "Prière seulement en Ten ou Ken");
        com.evandouard.hxh.nen.NenModes.trySetMode(player, com.evandouard.hxh.data.NenMode.TEN);
        for (int i = 0; i < 300; i++) QuestManager.onBaseStrike(player);
        helper.assertTrue(d.step() == 2, "300 coups en Ten : prière finie");
        QuestManager.advance(player); // Netero : « encaisse »
        // immobile sous les paumes : touché encore et encore, mais jamais tué, et l'épreuve repart
        for (int i = 0; i < 260; i++) QuestManager.tick(player);
        helper.assertTrue(player.isAlive() && player.getHealth() >= 1f, "Netero ne tue pas");
        helper.assertTrue(d.step() == 3, "Toujours sous les paumes (l'épreuve a recommencé)");
        QuestManager.advance(player);
        // méditation : blessé, accroupi, immobile, en Ten
        player.setHealth(player.getMaxHealth() * 0.4f);
        player.setShiftKeyDown(true);
        com.evandouard.hxh.nen.NenModes.trySetMode(player, com.evandouard.hxh.data.NenMode.TEN);
        helper.assertTrue(!h.healUnlocked(), "Pas encore de soin");
        for (int i = 0; i < 20 * 32; i++) {
            player.tickCount++;
            QuestManager.tick(player);
        }
        helper.assertTrue(d.step() == 5, "Méditation finie");
        QuestManager.advance(player);
        helper.assertTrue(h.healUnlocked(), "Soin par le Nen débloqué par Netero");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void airportsOpenWhenVisitedAndCostJenny(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var all = com.evandouard.hxh.travel.Airports.get(helper.getLevel().getServer());
        String dim = helper.getLevel().dimension().location().toString();
        var a = all.at(dim, new net.minecraft.core.BlockPos(100000, 70, 0), "A");
        var b = all.at(dim, new net.minecraft.core.BlockPos(102000, 70, 0), "B");
        helper.assertTrue(all.at(dim, new net.minecraft.core.BlockPos(100001, 70, 1), "X") == a, "Même guichet = même aéroport");
        helper.assertTrue(all.destinations(player.getUUID(), a).stream().noneMatch(x -> x == b), "B pas encore visité");
        all.visit(b, player.getUUID());
        helper.assertTrue(all.destinations(player.getUUID(), a).contains(b), "B visité : destination ouverte");
        helper.assertTrue(com.evandouard.hxh.travel.AirTravel.price(player, a, b) == 200, "2000 blocs = 200 Jenny");
        QuestManager.data(player).setHunter(true);
        helper.assertTrue(com.evandouard.hxh.travel.AirTravel.price(player, a, b) == 0, "Gratuit avec la licence");
        all.remove(a);
        all.remove(b);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void organizationsMakeEnemiesHittable(GameTestHelper helper) {
        // Brigade Fantôme contre Association : leurs techniques se touchent ; même organisation : jamais
        helper.assertTrue(com.evandouard.hxh.org.Organizations.get("ryodan") != null
                && com.evandouard.hxh.org.Organizations.get("association") != null, "Organisations chargées");
        for (String q : java.util.List.of("org_association", "org_ryodan", "org_bombers")) {
            var quest = QuestRegistry.get(HxHMod.id(q));
            helper.assertTrue(quest != null && quest.problems().isEmpty(), "Quête de recrutement valide : " + q);
        }
        ServerPlayer a = helper.makeMockServerPlayerInLevel();
        ServerPlayer b = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(com.evandouard.hxh.hatsu.Kinds.isAlly(a, b), "Sans organisation, les joueurs sont alliés");
        com.evandouard.hxh.org.Organizations.join(a, "association");
        com.evandouard.hxh.org.Organizations.join(b, "ryodan");
        helper.assertTrue(QuestManager.data(a).organization().equals("association"), "A a rejoint l'Association");
        helper.assertTrue(!com.evandouard.hxh.hatsu.Kinds.isAlly(a, b) && !com.evandouard.hxh.hatsu.Kinds.isAlly(b, a), "Ennemis : les techniques touchent");
        com.evandouard.hxh.org.Organizations.join(b, "zodiaques");
        helper.assertTrue(com.evandouard.hxh.hatsu.Kinds.isAlly(a, b), "Association et Zodiaques sont alliés");
        com.evandouard.hxh.org.Organizations.leave(b);
        helper.assertTrue(QuestManager.data(b).organization().isEmpty(), "B a quitté son organisation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void familiesGiveTheirInheritance(GameTestHelper helper) {
        // Zoldyck : poison refusé, foudre réduite ; Kurta : yeux écarlates sous 40 % de vie ; Freecss : XP de Force bonus
        helper.assertTrue(com.evandouard.hxh.family.Families.all().size() >= 5, "Familles chargées");
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        com.evandouard.hxh.family.Families.set(p, "zoldyck");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200));
        helper.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Zoldyck : immunisé au poison");
        com.evandouard.hxh.family.Families.set(p, "kurta");
        p.setHealth(p.getMaxHealth());
        helper.assertTrue(com.evandouard.hxh.family.Families.powerMultiplier(p) == 1f, "Kurta en pleine forme : puissance normale");
        p.setHealth(p.getMaxHealth() * 0.3f);
        helper.assertTrue(com.evandouard.hxh.family.Families.powerMultiplier(p) > 1.25f, "Kurta blessé : yeux écarlates");
        com.evandouard.hxh.family.Families.set(p, "freecss");
        helper.assertTrue(com.evandouard.hxh.family.Families.xpMultiplier(p, com.evandouard.hxh.data.Stat.STRENGTH) > 1.15, "Freecss : Force +20 %");
        int none = 0;
        java.util.Random r = new java.util.Random(42);
        for (int i = 0; i < 1000; i++) if (com.evandouard.hxh.family.Families.draw(r) == null) none++;
        helper.assertTrue(none > 600 && none < 800, "Environ 70 % sans famille au tirage (" + none + "/1000)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void familySecretMovesWork(GameTestHelper helper) {
        // Chaque famille donne son coup secret, placé juste avant les coups de base, lançable sans Nen
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        var d = com.evandouard.hxh.stats.HunterStats.get(p);
        com.evandouard.hxh.family.Families.set(p, "zoldyck");
        helper.assertTrue(d.familyMove().equals("family_zoldyck_claw"), "Zoldyck : Main-griffe donnée (" + d.familyMove() + ")");
        var ids = com.evandouard.hxh.hatsu.Hatsu.techniqueIds(d);
        helper.assertTrue(ids.indexOf("family_zoldyck_claw") == ids.indexOf("basic_attack") - 1, "Coup secret juste avant les coups de base");
        helper.assertTrue(!d.nenUnlocked(), "Pas de Nen pour ce test");
        // Main-griffe : une cible devant, 8 blocs
        net.minecraft.world.entity.monster.Zombie z = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.HUSK, new net.minecraft.core.BlockPos(5, 1, 1));
        p.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5)));
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
        com.evandouard.hxh.hatsu.Hatsu.select(p, "family_zoldyck_claw");
        float before = z.getHealth();
        helper.assertTrue(com.evandouard.hxh.hatsu.Hatsu.cast(p) == null, "Main-griffe lancée sans Nen");
        helper.assertTrue(z.getHealth() < before - 5, "Main-griffe : gros dégâts (" + before + " -> " + z.getHealth() + ")");
        helper.assertTrue("message.hxh.hatsu_cooldown".equals(com.evandouard.hxh.hatsu.Hatsu.cast(p)), "Recharge après le coup");
        // Ni copiable (Ging) ni volable (Chrollo)
        helper.assertTrue(com.evandouard.hxh.hatsu.Techniques.get("family_zoldyck_claw").needsNoNen(), "Sans Nen");
        // Kurta : Colère écarlate
        com.evandouard.hxh.family.Families.set(p, "kurta");
        helper.assertTrue(d.familyMove().equals("family_kurta_scarlet"), "Kurta : Colère écarlate");
        helper.assertTrue(!com.evandouard.hxh.hatsu.Hatsu.techniqueIds(d).contains("family_zoldyck_claw"), "L'ancien coup disparaît");
        com.evandouard.hxh.hatsu.Hatsu.select(p, "family_kurta_scarlet");
        helper.assertTrue(com.evandouard.hxh.hatsu.Hatsu.cast(p) == null, "Colère écarlate lancée");
        helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "Colère écarlate : Force");
        // Paladiknight : Premiers soins sur soi
        com.evandouard.hxh.family.Families.set(p, "paladiknight");
        p.setHealth(6f);
        p.setXRot(-90f); // personne devant : on se soigne soi-même
        com.evandouard.hxh.hatsu.Hatsu.select(p, "family_paladiknight_first_aid");
        helper.assertTrue(com.evandouard.hxh.hatsu.Hatsu.cast(p) == null, "Premiers soins lancés");
        helper.assertTrue(p.getHealth() >= 9.9f, "Premiers soins : +4 PV au moins (" + p.getHealth() + ")");
        // Freecss : Instinct du chasseur repère le husk
        com.evandouard.hxh.family.Families.set(p, "freecss");
        com.evandouard.hxh.hatsu.Hatsu.select(p, "family_freecss_instinct");
        helper.assertTrue(com.evandouard.hxh.hatsu.Hatsu.cast(p) == null, "Instinct : une créature autour, réussi");
        // Sans famille : plus de coup secret
        com.evandouard.hxh.family.Families.set(p, "");
        helper.assertTrue(d.familyMove().isEmpty(), "Sans famille : pas de coup secret");
        helper.succeed();
    }
}
