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
        return q == null || d.step >= q.steps().size() ? null : q.steps().get(d.step);
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
        d.running.put(id, new int[]{0, 0});
        if (d.tracked.isEmpty()) d.tracked = id;
        d.refocus();
        player.sendSystemMessage(Component.translatable("message.hxh.quest_started",
                Component.translatable(q.titleKey())).withStyle(ChatFormatting.GOLD));
        sync(player);
        return true;
    }

    private static void say(ServerPlayer player, NpcEntity npc, String line) {
        say(player, npc, Component.translatable(line));
    }

    private static void say(ServerPlayer player, NpcEntity npc, Component line) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh." + npc.npcId()).withStyle(ChatFormatting.YELLOW), line));
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
     * Examen raté (distancé au marathon…) : l'examen s'arrête, le prochain essai sera dans un jour Minecraft
     * et dans un autre centre d'examen.
     */
    public static void failExam(ServerPlayer player) {
        QuestData d = data(player);
        String exam = QuestRegistry.HUNTER_EXAM.toString();
        if (!d.running.containsKey(exam)) return;
        d.commit();
        d.running.remove(exam);
        if (d.active.equals(exam)) d.active = "";
        d.examRetryAt = player.level().getGameTime() + com.evandouard.hxh.HxHConfig.EXAM_RETRY_TICKS.get();
        d.examFailedCenter = d.examCenter;
        LOST_TICKS.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable("message.hxh.exam_failed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("message.hxh.exam_failed_detail").withStyle(ChatFormatting.GRAY));
        d.refocus();
        sync(player);
    }

    /** Clic droit sur un PNJ. */
    public static void onTalk(ServerPlayer player, NpcEntity npc) {
        QuestData d = data(player);
        // Le Navigateur lance l'Examen Hunter (sauf juste après un échec, ou dans le centre qui t'a recalé)
        if (npc.npcId().equals("navigator") && !d.hunter && !d.running.containsKey(QuestRegistry.HUNTER_EXAM.toString())
                && !d.completed.contains(QuestRegistry.HUNTER_EXAM.toString())) {
            long now = player.level().getGameTime();
            if (now < d.examRetryAt) {
                long minutes = Math.max(1, (d.examRetryAt - now) / 20 / 60);
                say(player, npc, Component.translatable("dialog.hxh.navigator.next_year", minutes));
                return;
            }
            if (npc.home().asLong() == d.examFailedCenter) {
                say(player, npc, Component.translatable("dialog.hxh.navigator.other_center"));
                return;
            }
            if (start(player, QuestRegistry.HUNTER_EXAM)) d.examCenter = npc.home().asLong();
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
            if (q != null && step != null && step.type().equals("talk") && step.npc().equals(npc.npcId())) {
                say(player, npc, "dialog.hxh." + q.id().getPath() + "." + step.id());
                advance(player);
                d.refocus();
                return;
            }
        }
        d.refocus();
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
        if (step.type().equals("arena_floor") && player.tickCount % 20 == 0 && d.arenaFloor >= step.count()) {
            advance(player);
            return;
        }
        // Inscription au 200e : validée même si elle a eu lieu avant d'arriver à cette étape
        if (step.type().equals("arena_register_200") && player.tickCount % 20 == 0 && d.registered200()) {
            advance(player);
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
    private static NpcEntity findNpc(ServerPlayer player, String npcId, double range) {
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
        d.step++;
        d.progress = 0;
        if (d.step >= q.steps().size()) {
            d.completed.add(d.active);
            d.running.remove(d.active);
            d.active = "";
            d.step = 0;
            player.sendSystemMessage(Component.translatable("message.hxh.quest_completed",
                    Component.translatable(q.titleKey())).withStyle(ChatFormatting.GOLD));
        } else {
            Quest.Step next = q.steps().get(d.step);
            player.sendSystemMessage(Component.translatable("message.hxh.quest_next",
                    Component.translatable(q.stepKey(next))).withStyle(ChatFormatting.AQUA));
            if (next.type().equals("follow")) {
                NpcEntity guide = findNpc(player, next.npc(), 96);
                if (guide != null) guide.resetRoute();
                LOST_TICKS.remove(player.getUUID());
            }
        }
        sync(player);
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
        Quest.Step step = st == null || st[0] >= q.steps().size() ? null : q.steps().get(st[0]);
        if (q == null || step == null) {
            com.evandouard.hxh.network.HxHNetwork.sendTo(player, new QuestStatusPayload("", "", "", 0, 0));
            return;
        }
        int progress = switch (step.type()) {
            case "follow" -> st[1] / 20;
            case "arena_floor" -> Math.min(d.arenaFloor, step.count());
            default -> st[1];
        };
        // Inscription en attente : même objectif que le refus de l'arbitre
        String[] pending = isArenaQuest(q) ? com.evandouard.hxh.arena.ArenaManager.pendingObjective(d) : null;
        if (pending != null) {
            com.evandouard.hxh.network.HxHNetwork.sendTo(player, new QuestStatusPayload(q.titleKey(), pending[0], pending[1], 0, 0));
            return;
        }
        com.evandouard.hxh.network.HxHNetwork.sendTo(player, new QuestStatusPayload(q.titleKey(), q.stepKey(step), "", progress, step.goal()));
    }

    /** Quête de la Tour Céleste (son objectif peut être remplacé par une inscription en attente). */
    public static boolean isArenaQuest(Quest q) {
        return q.id().getPath().equals("heavens_arena");
    }
}
