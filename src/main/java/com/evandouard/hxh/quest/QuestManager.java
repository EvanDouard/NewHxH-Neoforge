package com.evandouard.hxh.quest;

import com.evandouard.hxh.data.ModAttachments;
import com.evandouard.hxh.network.QuestStatusPayload;
import com.evandouard.hxh.npc.NpcEntity;
import net.minecraft.ChatFormatting;
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

    /** Démarre une quête si elle n'est ni en cours ni déjà terminée. */
    public static boolean start(ServerPlayer player, ResourceLocation questId) {
        QuestData d = data(player);
        Quest q = QuestRegistry.get(questId);
        if (q == null || !d.active.isEmpty() || d.completed.contains(questId.toString())) return false;
        d.active = questId.toString();
        d.step = 0;
        d.progress = 0;
        player.sendSystemMessage(Component.translatable("message.hxh.quest_started",
                Component.translatable(q.titleKey())).withStyle(ChatFormatting.GOLD));
        sync(player);
        return true;
    }

    private static void say(ServerPlayer player, NpcEntity npc, String line) {
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh." + npc.npcId()).withStyle(ChatFormatting.YELLOW),
                Component.translatable(line)));
    }

    /** Clic droit sur un PNJ. */
    public static void onTalk(ServerPlayer player, NpcEntity npc) {
        QuestData d = data(player);
        // Le Navigateur lance l'Examen Hunter
        if (d.active.isEmpty() && npc.npcId().equals("navigator") && !d.hunter
                && !d.completed.contains(QuestRegistry.HUNTER_EXAM.toString())) {
            start(player, QuestRegistry.HUNTER_EXAM);
        }
        Quest q = activeQuest(d);
        Quest.Step step = currentStep(d);
        if (q != null && step != null && step.type().equals("talk") && step.npc().equals(npc.npcId())) {
            say(player, npc, "dialog.hxh." + q.id().getPath() + "." + step.id());
            advance(player);
            return;
        }
        say(player, npc, "dialog.hxh." + npc.npcId() + ".idle");
    }

    /** Un mob tué par le joueur. */
    public static void onKill(ServerPlayer player, Entity killed) {
        QuestData d = data(player);
        Quest.Step step = currentStep(d);
        if (step == null || !step.type().equals("kill")) return;
        ResourceLocation type = EntityType.getKey(killed.getType());
        if (!type.toString().equals(step.entity())) return;
        d.progress++;
        if (d.progress >= step.count()) advance(player);
        else sync(player);
    }

    /** Chaque tick : étape « suivre un PNJ ». */
    public static void tick(ServerPlayer player) {
        QuestData d = data(player);
        Quest.Step step = currentStep(d);
        if (step == null || !step.type().equals("follow")) return;
        NpcEntity npc = findNpc(player, step.npc(), 64);
        if (npc == null) return;

        // Le PNJ avance vers des points successifs autour de chez lui : il faut le suivre
        if (npc.getNavigation().isDone()) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double radius = 12 + player.getRandom().nextDouble() * 10;
            Vec3 home = Vec3.atBottomCenterOf(npc.home().equals(net.minecraft.core.BlockPos.ZERO) ? npc.blockPosition() : npc.home());
            npc.walkTo(home.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), 1.1);
        }

        if (player.distanceTo(npc) <= step.maxDistance()) {
            d.progress++;
            if (d.progress >= step.seconds() * 20) {
                npc.getNavigation().stop();
                advance(player);
                return;
            }
        } else if (d.progress > 0) {
            d.progress = 0;
            player.displayClientMessage(Component.translatable("message.hxh.quest_left_behind").withStyle(ChatFormatting.RED), true);
            sync(player);
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
            d.active = "";
            d.step = 0;
            player.sendSystemMessage(Component.translatable("message.hxh.quest_completed",
                    Component.translatable(q.titleKey())).withStyle(ChatFormatting.GOLD));
        } else {
            player.sendSystemMessage(Component.translatable("message.hxh.quest_next",
                    Component.translatable(q.stepKey(q.steps().get(d.step)))).withStyle(ChatFormatting.AQUA));
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
        if (step.rewardHunter()) {
            d.hunter = true;
            player.sendSystemMessage(Component.translatable("message.hxh.became_hunter").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    /** Envoie au client ce qu'il faut afficher dans le suivi de quête. */
    public static void sync(ServerPlayer player) {
        QuestData d = data(player);
        Quest q = activeQuest(d);
        Quest.Step step = currentStep(d);
        if (q == null || step == null) {
            PacketDistributor.sendToPlayer(player, new QuestStatusPayload("", "", 0, 0));
            return;
        }
        int progress = step.type().equals("follow") ? d.progress / 20 : d.progress;
        PacketDistributor.sendToPlayer(player, new QuestStatusPayload(q.titleKey(), q.stepKey(step), progress, step.goal()));
    }
}
