package com.evandouard.hxh.quest;

import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.network.QuestStatusPayload;
import com.evandouard.hxh.npc.NpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fait avancer les quêtes : parler aux PNJ, tuer des mobs, suivre un PNJ. Côté serveur uniquement.
 */
public final class QuestManager {
    private QuestManager() {}

    public static QuestData data(ServerPlayer player) {
        return player.getData(ModAttachments.QUESTS);
    }

    @Nullable
    public static Quest activeQuest(QuestData d) {
        if (d.active.isEmpty()) return null;
        ResourceLocation id = ResourceLocation.tryParse(d.active);
        return id == null ? null : QuestRegistry.get(id);
    }

    @Nullable
    public static Quest.Step currentStep(QuestData d) {
        Quest q = activeQuest(d);
        if (q == null) return null;
        List<Quest.Step> steps = steps(d, q);
        return d.step >= steps.size() ? null : steps.get(d.step);
    }

    /** Étapes de la quête pour ce joueur : le parcours tiré au sort s'il y en a un, sinon celles du fichier. */
    public static List<Quest.Step> steps(QuestData d, Quest q) {
        int[] plan = q.randomTrials() ? d.plan(q.id().toString()) : null;
        if (plan == null) return q.steps();
        List<Quest.Step> out = new java.util.ArrayList<>(plan.length);
        for (int i : plan) if (i >= 0 && i < q.steps().size()) out.add(q.steps().get(i));
        return out;
    }

    /** Première quête de tout joueur : devenir Hunter (trouver un guide). */
    public static void startFirstQuest(ServerPlayer player) {
        QuestData d = data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        if (d.hunter || d.completed.contains(exam) || d.running.containsKey(exam)) return;
        start(player, QuestRegistry.HUNTER_EXAM);
    }

    /** Une touche sur Netero au jeu de la balle ; renvoie le nombre de touches. */
    public static int ballHit(ServerPlayer player) {
        QuestData d = data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        if (!d.running.containsKey(exam)) return 0;
        d.focus(exam);
        Quest.Step step = currentStep(d);
        int hits = 0;
        if (step != null && step.type().equals("ball")) {
            hits = ++d.progress;
            if (hits >= step.count()) {
                com.evandouard.hxh.exam.NeteroBall.won(player);
                advance(player);
            } else sync(player);
        }
        d.refocus();
        return hits;
    }

    /** Un candidat assommé au 288e Examen. */
    public static void knockedOut(ServerPlayer player) {
        QuestData d = data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        if (!d.running.containsKey(exam)) return;
        d.focus(exam);
        Quest.Step step = currentStep(d);
        if (step != null && step.type().equals("knockout")) {
            d.progress++;
            if (d.progress >= step.count()) {
                player.sendSystemMessage(Component.translatable("exam.hxh.ko_done").withStyle(ChatFormatting.GREEN));
                advance(player);
            } else sync(player);
        }
        d.refocus();
    }

    /** Un adversaire du tournoi final a abandonné. */
    public static void duelWon(ServerPlayer player) {
        QuestData d = data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        if (!d.running.containsKey(exam)) return;
        d.focus(exam);
        Quest.Step step = currentStep(d);
        if (step != null && step.type().equals("duel")) {
            d.progress++;
            if (d.progress >= step.count()) {
                com.evandouard.hxh.exam.ExamTournament.reset(player);
                advance(player);
            } else sync(player);
        }
        d.refocus();
    }

    /** Mort pendant l'Examen (réapparition hors de ses lieux) : l'épreuve est ratée. */
    public static void onRespawn(ServerPlayer player) {
        QuestData d = data(player);
        int[] st = d.stateOf(QuestRegistry.HUNTER_EXAM.toString());
        if (st != null && st[0] > 0 && d.examDeparture() == Long.MIN_VALUE
                && !com.evandouard.hxh.exam.ExamWorld.isExamLevel(player.level())
                && !d.examReturnDimension().isEmpty()) failExam(player);
    }

    /** Nouveau tirage de l'Examen en cours (commande) : retour à « trouver un guide ». */
    public static boolean redrawExam(ServerPlayer player) {
        QuestData d = data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        Quest q = QuestRegistry.get(QuestRegistry.HUNTER_EXAM);
        if (q == null || !d.running.containsKey(exam)) return false;
        d.commit();
        drawPlan(player, d, q);
        d.setExamDeparture(Long.MIN_VALUE);
        d.running.put(exam, new int[]{0, 0});
        if (d.active.equals(exam)) {
            d.step = 0;
            d.progress = 0;
        }
        d.refocus();
        sync(player);
        return true;
    }

    /** Tire un nouveau parcours d'épreuves (début de quête, ou nouvel essai après un échec). */
    static void drawPlan(ServerPlayer player, QuestData d, Quest q) {
        List<String> trials = TrialPlan.draw(q, player.getRandom());
        int[] steps = TrialPlan.steps(q, trials);
        java.util.Set<String> sites = new java.util.LinkedHashSet<>();
        for (int i : steps) {
            com.evandouard.hxh.exam.ExamSite site = com.evandouard.hxh.exam.ExamSite.byId(q.steps().get(i).site());
            if (site != null) sites.add(site.id());
        }
        d.setPlan(q.id().toString(), trials, steps, sites);
    }

    /** Applique {@code action} à chaque quête en cours (curseur placé dessus), puis revient sur la quête suivie. */
    private static void forEachQuest(ServerPlayer player, java.util.function.Consumer<QuestData> action) {
        QuestData d = data(player);
        for (String id : new java.util.ArrayList<>(d.running.keySet())) {
            if (!d.running.containsKey(id)) continue;
            d.focus(id);
            action.accept(d);
            d.commit();
        }
        d.refocus();
    }

    /** Démarre une quête si elle n'est ni en cours ni déjà terminée. Elle devient suivie si rien ne l'était. */
    public static boolean start(ServerPlayer player, ResourceLocation questId) {
        QuestData d = data(player);
        Quest q = QuestRegistry.get(questId);
        String id = questId.toString();
        if (q == null || d.running.containsKey(id) || d.completed.contains(id)) return false;
        d.commit();
        if (q.randomTrials()) drawPlan(player, d, q);
        d.running.put(id, new int[]{0, 0});
        if (d.tracked.isEmpty()) d.tracked = id;
        d.refocus();
        player.sendSystemMessage(Component.translatable("message.hxh.quest_started",
                q.title(player)).withStyle(ChatFormatting.GOLD));
        com.evandouard.hxh.network.Hud.toast(player, Component.translatable("hud.hxh.toast.new_quest"),
                q.title(player), "!", com.evandouard.hxh.network.Hud.GOLD);
        sync(player);
        return true;
    }

    private static void say(ServerPlayer player, NpcEntity npc, String line) {
        say(player, npc, Component.translatable(line));
    }

    private static void say(ServerPlayer player, NpcEntity npc, Component line) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                QuestRegistry.npcName(npc.npcId()).copy().withStyle(ChatFormatting.YELLOW), line));
    }

    /** Quiz posé et en attente de réponse : joueur → « quête#étape ». */
    private static final java.util.Map<java.util.UUID, String> QUIZ_ASKED = new java.util.HashMap<>();

    private static String quizKey(QuestData d) {
        return d.active + "#" + d.step;
    }

    /** Pose la question d'un quiz : réplique du PNJ et choix cliquables dans le chat. */
    private static void askQuiz(ServerPlayer player, NpcEntity npc, Quest q, Quest.Step step, QuestData d) {
        say(player, npc, q.question(step, player));
        for (int i = 0; i < step.options().size(); i++) {
            int n = i + 1;
            Component choice = Component.literal("  [" + n + "] ").withStyle(ChatFormatting.GOLD)
                    .append(q.option(step, i, player).copy().withStyle(ChatFormatting.WHITE))
                    .withStyle(st -> st.withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                    net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/hunter exam answer " + n))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("quest.hxh.quiz.click"))));
            player.sendSystemMessage(choice);
        }
        QUIZ_ASKED.put(player.getUUID(), quizKey(d));
        d.progress = 0;
        sync(player);
    }

    /**
     * Réponse au quiz en cours (choix n° {@code n}, à partir de 1). Une question « silence » se réussit en ne
     * répondant pas : toute réponse est fausse. Mauvaise réponse : il faut reparler au PNJ.
     */
    public static boolean answer(ServerPlayer player, int n) {
        QuestData d = data(player);
        for (String id : new java.util.ArrayList<>(d.runningQuests())) {
            d.focus(id);
            Quest q = activeQuest(d);
            Quest.Step step = currentStep(d);
            if (q == null || step == null || !step.type().equals("quiz")) continue;
            if (!quizKey(d).equals(QUIZ_ASKED.get(player.getUUID()))) continue;
            QUIZ_ASKED.remove(player.getUUID());
            if (step.answer().equals("kiriko")) {
                // Forêt des Kiriko : désigner la bête ; se tromper élimine
                if (com.evandouard.hxh.exam.KirikoForest.accuse(player, n)) {
                    advance(player);
                    d.refocus();
                    return true;
                }
                d.refocus();
                failExam(player);
                return false;
            }
            if (step.answer().equals("majority")) {
                // Tour de Trick, chemin de la majorité : les cinq votent, la majorité ouvre la porte
                com.evandouard.hxh.exam.TrickTower.vote(player, n);
                advance(player);
                d.refocus();
                return true;
            }
            boolean right = step.answer().equals("any") || step.answer().equals(String.valueOf(n));
            if (right) {
                player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                        QuestRegistry.npcName(step.npc()).copy().withStyle(ChatFormatting.YELLOW), q.dialog(step, player)));
                advance(player);
            } else if (q.id().equals(QuestRegistry.HUNTER_EXAM)) {
                // À l'Examen, une mauvaise réponse élimine
                player.sendSystemMessage(Component.translatable("quest.hxh.quiz.wrong_exam").withStyle(ChatFormatting.RED));
                d.refocus();
                failExam(player);
                return false;
            } else {
                player.sendSystemMessage(Component.translatable("quest.hxh.quiz.wrong").withStyle(ChatFormatting.RED));
                d.progress = 0;
                sync(player);
            }
            d.refocus();
            return right;
        }
        d.refocus();
        return false;
    }

    /** Un message dans le chat pendant un quiz « silence » : c'est une réponse, donc une mauvaise. */
    public static void onChat(ServerPlayer player) {
        if (!QUIZ_ASKED.containsKey(player.getUUID())) return;
        Quest.Step step = currentStep(data(player));
        if (step != null && step.type().equals("quiz") && step.answer().equals("silence")) answer(player, 0);
    }

    /** Ticks passés trop loin du PNJ à suivre (par joueur). */
    private static final java.util.Map<java.util.UUID, Integer> LOST_TICKS = new java.util.HashMap<>();

    /** Un tick passé trop loin du guide : avertit, et renvoie true quand la limite est atteinte. */
    private static boolean fallBehind(ServerPlayer player) {
        int lost = LOST_TICKS.merge(player.getUUID(), 1, Integer::sum);
        int limit = com.evandouard.hxh.HxHConfig.EXAM_LOST_SECONDS.get() * 20;
        if (lost >= limit) return true;
        if (lost % 20 == 1) {
            player.displayClientMessage(Component.translatable("message.hxh.quest_left_behind", (limit - lost) / 20 + 1)
                    .withStyle(ChatFormatting.RED), true);
        }
        return false;
    }

    /**
     * Épreuve ratée (distancé, passé par-dessus bord, mauvaise réponse…) : retour au point de départ, et l'examen
     * reprend à « trouver un guide », avec un nouveau tirage d'épreuves dans des lieux jamais vus.
     */
    public static void failExam(ServerPlayer player) {
        QuestData d = data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        Quest q = QuestRegistry.get(QuestRegistry.HUNTER_EXAM);
        if (!d.running.containsKey(exam) || q == null) return;
        d.commit();
        drawPlan(player, d, q);
        d.setExamDeparture(Long.MIN_VALUE);
        d.running.put(exam, new int[]{0, 0});
        if (d.active.equals(exam)) {
            d.step = 0;
            d.progress = 0;
        }
        d.examFailedCenter = d.examCenter;
        LOST_TICKS.remove(player.getUUID());
        QUIZ_ASKED.remove(player.getUUID());
        com.evandouard.hxh.exam.ExamTournament.reset(player);
        com.evandouard.hxh.exam.TrickTower.stopClock(player);
        com.evandouard.hxh.exam.Exam288.reset(player);
        player.sendSystemMessage(Component.translatable("message.hxh.exam_failed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("message.hxh.exam_failed_detail").withStyle(ChatFormatting.GRAY));
        d.refocus();
        com.evandouard.hxh.exam.ExamTrips.returnHome(player);
        sync(player);
    }

    /** Clic droit sur un PNJ. */
    public static void onTalk(ServerPlayer player, NpcEntity npc) {
        QuestData d = data(player);
        // Un guide (Navigateur) lance l'Examen Hunter s'il n'est pas déjà en cours
        if (npc.npcId().equals("navigator") && !d.hunter && !d.running.containsKey(QuestRegistry.HUNTER_EXAM.toString())
                && !d.completed.contains(QuestRegistry.HUNTER_EXAM.toString())) {
            if (start(player, QuestRegistry.HUNTER_EXAM)) d.examCenter = npc.home().asLong();
        }
        // Forêt des Kiriko : examiner un des deux maris
        if (npc.npcId().startsWith("husband_")) {
            com.evandouard.hxh.exam.KirikoForest.examine(player, npc);
            return;
        }
        // Le passeur d'un point de départ embarque le candidat pour son épreuve
        if (npc.npcId().equals("passeur")) {
            com.evandouard.hxh.exam.ExamDeparture.talk(player, npc);
            return;
        }
        // L'arbitre du hall d'une tranche de la Tour Céleste lance les combats
        if (npc.npcId().equals("referee")) {
            com.evandouard.hxh.arena.ArenaManager.talkToReferee(player, npc);
            return;
        }
        // L'hôtesse de la Tour Céleste inscrit (hall d'entrée, chaque tranche, 200e) et donne ses petites quêtes
        if (npc.npcId().equals("receptionist")) {
            com.evandouard.hxh.arena.ArenaManager.talkToReceptionist(player, npc);
        }
        // Une seule réplique par clic : la quête suivie d'abord, puis les autres quêtes en cours
        java.util.List<String> order = new java.util.ArrayList<>(d.runningQuests());
        if (order.remove(d.tracked)) order.add(0, d.tracked);
        for (String id : order) {
            d.focus(id);
            Quest q = activeQuest(d);
            Quest.Step step = currentStep(d);
            if (q != null && step != null && step.type().equals("quiz") && step.npc().equals(npc.npcId())) {
                askQuiz(player, npc, q, step, d);
                d.refocus();
                return;
            }
            if (q != null && step != null && step.type().equals("talk") && step.npc().equals(npc.npcId())) {
                say(player, npc, q.dialog(step, player));
                advance(player);
                d.refocus();
                return;
            }
            // Apporter des objets au PNJ : ils sont pris de l'inventaire s'il y en a assez
            if (q != null && step != null && step.type().equals("bring") && step.npc().equals(npc.npcId())) {
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(step.item()));
                int have = player.getInventory().countItem(item);
                if (have >= step.count()) {
                    int left = step.count();
                    for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
                        net.minecraft.world.item.ItemStack s = player.getInventory().getItem(i);
                        if (s.is(item)) {
                            int take = Math.min(left, s.getCount());
                            s.shrink(take);
                            left -= take;
                        }
                    }
                    say(player, npc, q.dialog(step, player));
                    advance(player);
                } else {
                    say(player, npc, Component.translatable("dialog.hxh.bring_missing", step.count() - have,
                            new net.minecraft.world.item.ItemStack(item).getHoverName()));
                }
                d.refocus();
                return;
            }
        }
        d.refocus();
        // Un PNJ qui donne une quête (start_npc) : elle démarre si ses prérequis sont faits
        for (Quest q : QuestRegistry.all()) {
            String id = q.id().toString();
            if (!q.startNpc().equals(npc.npcId()) || d.running.containsKey(id) || d.completed.contains(id)) continue;
            if (!q.requires().stream().allMatch(r -> d.completed.contains(ResourceLocation.parse(r).toString()))) continue;
            say(player, npc, q.introDialog(player));
            start(player, q.id());
            return;
        }
        String idle = com.evandouard.hxh.arena.ArenaManager.idleDialog(player, npc);
        say(player, npc, idle != null ? idle : "dialog.hxh." + npc.npcId() + ".idle");
    }

    /** La quête du guichet de la Tour Céleste démarre au premier passage. */
    public static void startArenaQuestIfNew(ServerPlayer player) {
        start(player, QuestRegistry.HEAVENS_ARENA);
    }

    /** Un mob tué par le joueur. */
    public static void onKill(ServerPlayer player, Entity killed) {
        forEachQuest(player, d -> killStep(player, d, killed));
    }

    private static void killStep(ServerPlayer player, QuestData d, Entity killed) {
        Quest.Step step = currentStep(d);
        if (step == null || !step.type().equals("kill")) return;
        ResourceLocation type = EntityType.getKey(killed.getType());
        if (!type.toString().equals(step.entity())) return;
        d.progress++;
        if (d.progress >= step.count()) advance(player);
        else sync(player);
    }

    /** Résultat d'un combat de la Tour Céleste (étapes « gagner N combats »). */
    public static void onArenaResult(ServerPlayer player, boolean win) {
        forEachQuest(player, d -> arenaStep(player, d, win));
    }

    private static void arenaStep(ServerPlayer player, QuestData d, boolean win) {
        Quest.Step step = currentStep(d);
        if (step == null) return;
        if (step.type().equals("arena_win") && win) {
            d.progress++;
            if (d.progress >= step.count()) advance(player);
            else sync(player);
        } else if (step.type().equals("arena_floor")) {
            checkFloor(player);
        }
    }

    /** Étape « atteindre l'étage N » : validée dès que l'étage est atteint. */
    private static void checkFloor(ServerPlayer player) {
        QuestData d = data(player);
        Quest.Step step = currentStep(d);
        if (step == null || !step.type().equals("arena_floor")) return;
        if (d.arenaFloor >= step.count()) advance(player);
        else sync(player);
    }

    /** Chaque tick : étape « suivre un PNJ » (et vérification de l'étage atteint, chaque seconde). */
    public static void tick(ServerPlayer player) {
        if (data(player).running.isEmpty()) return;
        forEachQuest(player, d -> tickStep(player, d));
    }

    private static void tickStep(ServerPlayer player, QuestData d) {
        Quest.Step step = currentStep(d);
        if (step == null) return;
        // Dangers des lieux de l'Examen (tempête, brouillard…)
        if (com.evandouard.hxh.exam.ExamWorld.isExamLevel(player.level()) && com.evandouard.hxh.exam.ExamHazards.tick(player, step, d)) return;
        // Quiz « silence » : la bonne réponse est de ne rien dire pendant le temps donné
        if (step.type().equals("quiz") && step.answer().equals("silence") && quizKey(d).equals(QUIZ_ASKED.get(player.getUUID()))) {
            d.progress++;
            if (d.progress >= Math.max(1, step.seconds()) * 20) {
                QUIZ_ASKED.remove(player.getUUID());
                player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                        QuestRegistry.npcName(step.npc()).copy().withStyle(ChatFormatting.YELLOW), activeQuest(d).dialog(step, player)));
                advance(player);
            } else if (player.tickCount % 20 == 0) sync(player);
            return;
        }
        // Atteindre un point de la route du lieu (Tour de Trick : trappe, sortie) ; avec « seconds », compte à rebours
        if (step.type().equals("waypoint")) {
            d.progress++;
            // l'horloge de la tour court depuis l'annonce de Lippo (paris perdus compris)
            int tower = com.evandouard.hxh.exam.TrickTower.elapsedTicks(player);
            if (tower >= 0 && step.seconds() > 0) d.progress = tower;
            if (com.evandouard.hxh.exam.TrickTower.reached(player, step.count())) {
                if (step.seconds() > 0) com.evandouard.hxh.exam.TrickTower.stopClock(player);
                advance(player);
            } else if (step.seconds() > 0 && d.progress >= step.seconds() * 20) {
                com.evandouard.hxh.exam.TrickTower.stopClock(player);
                player.sendSystemMessage(Component.translatable("exam.hxh.tower_time").withStyle(ChatFormatting.RED));
                failExam(player);
            } else if (d.progress % 20 == 0) sync(player);
            return;
        }
        // 288e Examen : assommer toute la meute avant la fin du temps
        if (step.type().equals("knockout")) {
            com.evandouard.hxh.exam.Exam288.tick(player, step, d);
            return;
        }
        // Jeu de la balle de Netero : le temps est compté
        if (step.type().equals("ball")) {
            com.evandouard.hxh.exam.NeteroBall.tick(player, step);
            return;
        }
        // Tournoi final : un adversaire après l'autre
        if (step.type().equals("duel")) {
            com.evandouard.hxh.exam.ExamTournament.tick(player, step, d);
            return;
        }
                // Tenir bon (tempête sur le pont…) : le compteur avance tant qu'on reste debout dans le lieu
        if (step.type().equals("survive")) {
            d.progress++;
            if (d.progress >= step.seconds() * 20) advance(player);
            else if (player.tickCount % 20 == 0) sync(player);
            return;
        }
        // Plaques de l'île de Zevil : 3 points pour sa plaque et celle de sa cible, 1 pour les autres
        if (step.type().equals("points") && player.tickCount % 20 == 0) {
            d.progress = com.evandouard.hxh.exam.ExamHazards.platePoints(player);
            if (d.progress >= step.count()) advance(player);
            else sync(player);
            return;
        }
        if (step.type().equals("arena_floor") && player.tickCount % 20 == 0 && d.arenaFloor >= step.count()) {
            advance(player);
            return;
        }
        // Inscription au 200e : validée même si elle a eu lieu avant d'arriver à cette étape
        if (step.type().equals("arena_register_200") && player.tickCount % 20 == 0 && d.registered200()) {
            advance(player);
            return;
        }
        // Entraînement au Nen : tenir une posture / un sens, ou attendre un état (éveil, divination, Hatsu)
        if (player.tickCount % 20 == 0 && (step.type().equals("nen_mode") || step.type().equals("nen_sense")
                || step.type().equals("nen_flag"))) {
            com.evandouard.hxh.data.HunterData h = com.evandouard.hxh.stats.HunterStats.get(player);
            boolean ok = switch (step.type()) {
                case "nen_mode" -> h.mode().id().equals(step.mode());
                case "nen_sense" -> step.mode().equals("gyo") ? h.gyo() : h.en();
                default -> switch (step.mode()) {
                    case "unlocked" -> h.nenUnlocked();
                    case "revealed" -> h.nenRevealed();
                    default -> h.hasKit();
                };
            };
            if (step.type().equals("nen_flag")) {
                if (ok) advance(player);
                return;
            }
            int[] st = d.running.get(d.active);
            if (ok && st != null) {
                d.progress++;
                if (d.progress >= step.seconds()) advance(player);
                else sync(player);
            }
            return;
        }
        // Aller à un endroit
        if (step.type().equals("reach") && step.pos() != null && player.tickCount % 10 == 0) {
            double[] p = step.pos();
            if (player.distanceToSqr(p[0], p[1], p[2]) <= step.radius() * step.radius()) advance(player);
            return;
        }
        // Apporter des objets : le compteur suit l'inventaire
        if (step.type().equals("bring") && player.tickCount % 20 == 0) {
            sync(player);
            return;
        }
        if (!step.type().equals("follow")) return;
        NpcEntity npc = findNpc(player, step.npc(), 96);
        if (npc == null) {
            // Hors de portée : c'est être distancé
            if (fallBehind(player)) failExam(player);
            return;
        }
        // Vitesse réglable (≈ Vitesse I par défaut) : le modificateur de déplacement agit au carré chez les mobs
        double attr = npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double speed = Math.sqrt(com.evandouard.hxh.HxHConfig.EXAM_MARATHON_SPEED.get()) / Math.max(0.01, attr);

        boolean arrived = false;
        if (!npc.route().isEmpty()) {
            // Parcours posé par des repères numérotés : le dernier point est l'arrivée
            int i = Math.min(npc.routeIndex(), npc.route().size() - 1);
            BlockPos target = npc.route().get(i);
            if (npc.blockPosition().distSqr(target) <= 2.5 * 2.5) {
                if (i >= npc.route().size() - 1) arrived = true;
                else npc.setRouteIndex(i + 1);
            }
            if (!arrived && (npc.getNavigation().isDone() || player.tickCount % 20 == 0)) {
                BlockPos t = npc.route().get(Math.min(npc.routeIndex(), npc.route().size() - 1));
                npc.walkTo(Vec3.atBottomCenterOf(t), speed);
            }
        } else if (npc.getNavigation().isDone()) {
            // Sans parcours : il avance vers des points successifs autour de chez lui
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double radius = 12 + player.getRandom().nextDouble() * 10;
            Vec3 home = Vec3.atBottomCenterOf(npc.home().equals(BlockPos.ZERO) ? npc.blockPosition() : npc.home());
            npc.walkTo(home.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), speed);
        }

        if (player.distanceTo(npc) <= step.maxDistance()) {
            LOST_TICKS.remove(player.getUUID());
            d.progress++;
            boolean done = npc.route().isEmpty() ? d.progress >= step.seconds() * 20 : arrived;
            if (done) {
                npc.getNavigation().stop();
                advance(player);
                return;
            }
        } else if (fallBehind(player)) {
            npc.resetRoute();
            failExam(player);
            return;
        }
        if (player.tickCount % 20 == 0) sync(player);
    }

    @Nullable
    public static NpcEntity findNpc(ServerPlayer player, String npcId, double range) {
        List<NpcEntity> list = player.level().getEntitiesOfClass(NpcEntity.class,
                player.getBoundingBox().inflate(range), n -> n.npcId().equals(npcId));
        NpcEntity best = null;
        for (NpcEntity n : list) if (best == null || n.distanceTo(player) < best.distanceTo(player)) best = n;
        return best;
    }

    /** Termine l'étape en cours (récompenses), passe à la suivante ou termine la quête. */
    public static void advance(ServerPlayer player) {
        QuestData d = data(player);
        Quest q = activeQuest(d);
        Quest.Step step = currentStep(d);
        if (q == null || step == null) return;
        giveRewards(player, d, step);
        if (step.type().equals("duel")) com.evandouard.hxh.exam.ExamTournament.reset(player);
        if (step.type().equals("ball")) com.evandouard.hxh.exam.NeteroBall.drop(player);
        if (step.type().equals("knockout")) com.evandouard.hxh.exam.Exam288.reset(player);
        if (step.trial().equals("tour") && step.npc().equals("lippo")) com.evandouard.hxh.exam.TrickTower.startClock(player);
        d.step++;
        d.progress = 0;
        List<Quest.Step> steps = steps(d, q);
        if (d.step >= steps.size()) {
            d.completed.add(d.active);
            d.running.remove(d.active);
            d.active = "";
            d.step = 0;
            player.sendSystemMessage(Component.translatable("message.hxh.quest_completed",
                    q.title(player)).withStyle(ChatFormatting.GOLD));
            // Fin d'un voyage (Examen) : retour là d'où l'on était parti
            com.evandouard.hxh.exam.ExamTrips.returnHome(player);
            com.evandouard.hxh.network.Hud.toast(player, Component.translatable("hud.hxh.toast.quest_done"),
                    q.title(player), "★", com.evandouard.hxh.network.Hud.GOLD);
        } else {
            Quest.Step next = steps.get(d.step);
            // Nouvelle épreuve : annoncée, et on y va par son dirigeable (à pied jusqu'à lui)
            boolean newTrial = !next.trial().isEmpty() && !next.trial().equals(step.trial());
            if (newTrial) announceTrial(player, d, q, steps, d.step);
            com.evandouard.hxh.exam.ExamSite site = com.evandouard.hxh.exam.ExamSite.byId(next.site());
            boolean real = com.evandouard.hxh.exam.ExamTrips.enabled(player);
            if (real && newTrial) {
                // Épreuve réussie : retour au dirigeable d'où l'on était parti, puis coordonnées du suivant
                com.evandouard.hxh.exam.ExamTrips.returnHome(player);
                BlockPos dep = com.evandouard.hxh.exam.ExamDeparture.create(player, player.blockPosition());
                if (dep != null) player.sendSystemMessage(Component.translatable("exam.hxh.departure", dep.getX(), dep.getZ(),
                        (int) Math.sqrt(dep.distSqr(player.blockPosition()))).withStyle(ChatFormatting.GOLD));
            } else if (site != null) {
                // Dans une même épreuve, on passe d'un lieu à l'autre par ses transports (bateau, ascenseur…)
                d.markExamVisited(site.id(), d.examInstance(site.id()));
                com.evandouard.hxh.exam.ExamTrips.sendTo(player, site, d.examInstance(site.id()));
            }
            com.evandouard.hxh.network.Hud.toast(player, Component.translatable("hud.hxh.toast.new_step"),
                    q.objective(next, player), "!", com.evandouard.hxh.network.Hud.GOLD);
            if (next.type().equals("follow")) {
                NpcEntity guide = findNpc(player, next.npc(), 96);
                if (guide != null) guide.resetRoute();
                LOST_TICKS.remove(player.getUUID());
            }
        }
        sync(player);
    }

    /** « Épreuve n/N : … — lieu (x, z) » au début de chaque épreuve tirée. */
    private static void announceTrial(ServerPlayer player, QuestData d, Quest q, List<Quest.Step> steps, int index) {
        String trial = steps.get(index).trial();
        List<String> order = d.planTrials(q.id().toString());
        int n = order.indexOf(trial) + 1;
        com.evandouard.hxh.exam.ExamSite site = null;
        for (int i = index; i < steps.size() && steps.get(i).trial().equals(trial) && site == null; i++)
            site = com.evandouard.hxh.exam.ExamSite.byId(steps.get(i).site());
        Component name = Component.translatable("exam.hxh.trial." + trial);
        Component msg = site == null
                ? Component.translatable("exam.hxh.trial_next", n, order.size(), name)
                : Component.translatable("exam.hxh.trial_next_at", n, order.size(), name,
                        Component.translatable("exam.hxh.site." + site.id()));
        player.sendSystemMessage(msg.copy().withStyle(ChatFormatting.AQUA));
    }

    private static void giveRewards(ServerPlayer player, QuestData d, Quest.Step step) {
        if (!step.rewardItem().isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(step.rewardItem());
            Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
        }
        if (step.rewardJenny() > 0) {
            d.jenny += step.rewardJenny();
            player.sendSystemMessage(Component.translatable("message.hxh.jenny_gained", step.rewardJenny(), d.jenny)
                    .withStyle(ChatFormatting.YELLOW));
        }
        // Capacités du Nen apprises auprès du maître
        for (String a : step.rewardLearn()) {
            com.evandouard.hxh.nen.NenLearning.Ability ability = com.evandouard.hxh.nen.NenLearning.Ability.byId(a);
            if (ability != null && com.evandouard.hxh.stats.HunterStats.get(player).learn(a)) {
                com.evandouard.hxh.network.Hud.toast(player, Component.translatable("hud.hxh.toast.learned"),
                        Component.translatable(ability.translationKey()), "★", com.evandouard.hxh.network.Hud.BLUE);
            }
        }
        if (step.rewardHunter()) {
            d.hunter = true;
            player.sendSystemMessage(Component.translatable("message.hxh.became_hunter").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    /** Envoie au client ce qu'il faut afficher dans le suivi de quête. */
    public static void sync(ServerPlayer player) {
        QuestData d = data(player);
        d.commit();
        // Toujours la quête suivie, même pendant le traitement d'une autre quête
        ResourceLocation trackedId = ResourceLocation.tryParse(d.tracked);
        if (!d.running.containsKey(d.tracked)) {
            trackedId = d.running.isEmpty() ? null : ResourceLocation.tryParse(d.running.keySet().iterator().next());
        }
        Quest q = trackedId == null ? null : QuestRegistry.get(trackedId);
        int[] st = q == null ? null : d.running.get(q.id().toString());
        List<Quest.Step> qs = q == null ? List.of() : steps(d, q);
        Quest.Step step = st == null || st[0] >= qs.size() ? null : qs.get(st[0]);
        if (q == null || step == null) {
            com.evandouard.hxh.network.HxHNetwork.sendTo(player, QuestStatusPayload.NONE);
            return;
        }
        int progress = switch (step.type()) {
            case "follow", "survive" -> st[1] / 20;
            case "quiz" -> step.answer().equals("silence") ? st[1] / 20 : 0;
            case "arena_floor" -> Math.min(d.arenaFloor, step.count());
            case "bring" -> Math.min(step.count(), player.getInventory().countItem(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(step.item()))));
            default -> st[1];
        };
        // Examen : trouver un guide (on dit où il attend)
        if (q.id().equals(QuestRegistry.HUNTER_EXAM) && step.npc().equals("navigator") && step.type().equals("talk")) {
            long nav = com.evandouard.hxh.exam.ExamSavedData.get(player.server.overworld()).navigator();
            if (nav != Long.MIN_VALUE) {
                BlockPos g = BlockPos.of(nav);
                com.evandouard.hxh.network.HxHNetwork.sendTo(player, new QuestStatusPayload(q.title(player),
                        Component.translatable("exam.hxh.guide_objective", g.getX(), g.getZ()), 0, 0));
                return;
            }
        }
        // Examen : point de départ donné par le guide, pas encore rejoint
        if (q.id().equals(QuestRegistry.HUNTER_EXAM) && d.examDeparture() != Long.MIN_VALUE
                && !com.evandouard.hxh.exam.ExamWorld.isExamLevel(player.level())) {
            BlockPos dep = BlockPos.of(d.examDeparture());
            com.evandouard.hxh.network.HxHNetwork.sendTo(player, new QuestStatusPayload(q.title(player),
                    Component.translatable("exam.hxh.departure_objective", dep.getX(), dep.getZ()), 0, 0));
            return;
        }
        // Inscription en attente : même objectif que le refus de l'arbitre
        String[] pending = isArenaQuest(q) ? com.evandouard.hxh.arena.ArenaManager.pendingObjective(d) : null;
        if (pending != null) {
            Component pend = pending[1].isEmpty() ? Component.translatable(pending[0]) : Component.translatable(pending[0], pending[1]);
            com.evandouard.hxh.network.HxHNetwork.sendTo(player, new QuestStatusPayload(q.title(player), pend, 0, 0));
            return;
        }
        com.evandouard.hxh.network.HxHNetwork.sendTo(player, new QuestStatusPayload(q.title(player), q.objective(step, player), progress, step.goal()));
    }

    /**
     * Valide l'étape « custom » en cours d'une quête (pour le code qui gérera plus tard ces conditions, ou
     * {@code /hunter quest validate}).
     * @return true si l'étape était bien une étape « custom » en cours
     */
    public static boolean completeCustom(ServerPlayer player, ResourceLocation questId) {
        QuestData d = data(player);
        if (!d.running.containsKey(questId.toString())) return false;
        d.focus(questId.toString());
        Quest.Step step = currentStep(d);
        boolean ok = step != null && step.type().equals("custom");
        if (ok) advance(player);
        d.refocus();
        sync(player);
        return ok;
    }

    /** Quête de la Tour Céleste (son objectif peut être remplacé par une inscription en attente). */
    public static boolean isArenaQuest(Quest q) {
        return q.id().getPath().equals("heavens_arena");
    }
}
