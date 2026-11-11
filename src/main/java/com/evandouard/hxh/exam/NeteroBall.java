package com.evandouard.hxh.exam;

import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.evandouard.hxh.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Le « mini-examen » du dirigeable : prendre la balle des mains de Netero. On ne l'aura pas (Gon et Killua non plus),
 * mais chaque coup qui le force à se servir de sa main droite ou de sa jambe gauche compte. Étape
 * {@code "type": "ball"} : {@code count} touches à réussir en {@code seconds} secondes. Netero esquive la plupart des
 * tentatives en se déplaçant d'un pas invisible. Pas éliminatoire : le temps écoulé, l'Examen continue ; réussir
 * rapporte une prime. Ce n'est pas une épreuve comptée : une pause dans le dirigeable (on s'y soigne et on y
 * mange) où Netero attend qu'on vienne lui parler pour lancer son jeu, limité à 35 s.
 */
public final class NeteroBall {
    private NeteroBall() {}

    /**
     * Netero accélère à chaque touche : chance d'esquive et intervalle entre deux pas éclair (ticks), par nombre de
     * touches déjà réussies. Le départ vaut l'ancien « stade 3 » ; la fin est presque impossible à suivre.
     */
    static final float[] DODGE = {0.80f, 0.88f, 0.94f, 0.97f};
    static final int[] BLINK_EVERY = {16, 10, 6, 4};
    /** Pris de vitesse (candidat qui sprinte) : un peu moins d'esquive. */
    static final float SPRINT_BONUS = 0.08f;
    /** Netero ne laisse personne s'approcher : à moins de ce rayon, il s'éclipse de lui-même. */
    static final double PERSONAL_SPACE = 3.0;
    static final int PRIZE = 500;
    private static final Map<UUID, Integer> TICKS = new HashMap<>();

    private static int stage(ServerPlayer player) {
        return Math.min(DODGE.length - 1, Math.max(0, QuestManager.data(player).progress()));
    }

    public static boolean inBall(ServerPlayer player) {
        Quest.Step step = QuestManager.currentStep(QuestManager.data(player));
        return step != null && step.type().equals("ball");
    }

    /** Une tentative sur Netero (coup porté) : esquive ou touche. */
    public static void attempt(ServerPlayer player, NpcEntity netero) {
        float dodge = DODGE[stage(player)] - (player.isSprinting() ? SPRINT_BONUS : 0f);
        touch(player, netero, player.getRandom().nextFloat() < dodge);
    }

    public static void touch(ServerPlayer player, NpcEntity netero, boolean dodged) {
        QuestData d = QuestManager.data(player);
        Quest.Step step = QuestManager.currentStep(d);
        if (step == null || !step.type().equals("ball")) return;
        if (dodged) {
            blink(player, netero);
            return;
        }
        int hits = QuestManager.ballHit(player);
        player.sendSystemMessage(Component.translatable("exam.hxh.ball_touch", hits, step.count()).withStyle(ChatFormatting.AQUA));
        blink(player, netero);
    }

    /** Netero passe derrière le candidat, d'un pas que l'œil ne suit pas. */
    private static void blink(ServerPlayer player, NpcEntity netero) {
        if (!(netero.level() instanceof ServerLevel level)) return;
        // trait de vitesse façon manga là où il était (pas de particules vanilla)
        com.evandouard.hxh.fx.NenFx.slashAt(level, netero.position().add(0, 1, 0), 1.4f, 0xFFFFFF);
        // derrière le candidat ou sur un côté, à 3 ou 4 blocs
        double a = Math.atan2(-player.getLookAngle().z, -player.getLookAngle().x)
                + (player.getRandom().nextDouble() - 0.5) * Math.PI * 1.2;
        Vec3 to = ExamWorld.safeSpot(level, player.position(), a, 3 + player.getRandom().nextDouble());
        if (to != player.position()) netero.teleportTo(to.x, to.y, to.z);
    }

    /** Chaque tick de l'étape : le temps passe ; écoulé, Netero garde sa balle et l'Examen continue. */
    public static void tick(ServerPlayer player, Quest.Step step) {
        int t = TICKS.merge(player.getUUID(), 1, Integer::sum);
        NpcEntity netero = QuestManager.findNpc(player, "netero", 32);
        if (netero != null) {
            if (!netero.getMainHandItem().is(ModItems.NETERO_BALL.get()))
                netero.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NETERO_BALL.get()));
            // Il ne tient pas en place : un pas éclair de plus en plus souvent, et toujours si on l'approche
            int every = BLINK_EVERY[stage(player)];
            if (t % every == 0 && (netero.distanceTo(player) < PERSONAL_SPACE || player.getRandom().nextInt(3) == 0))
                blink(player, netero);
            netero.getLookControl().setLookAt(player);
        }
        if (t >= step.seconds() * 20) {
            TICKS.remove(player.getUUID());
            drop(player);
            player.sendSystemMessage(Component.translatable("exam.hxh.ball_time").withStyle(ChatFormatting.GRAY));
            QuestManager.advance(player);
        }
    }

    /** Fin du jeu (temps, victoire, étape passée) : Netero range sa balle. */
    public static void drop(ServerPlayer player) {
        NpcEntity netero = QuestManager.findNpc(player, "netero", 32);
        if (netero != null) netero.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    public static int secondsLeft(ServerPlayer player, Quest.Step step) {
        return Math.max(0, step.seconds() - TICKS.getOrDefault(player.getUUID(), 0) / 20);
    }

    /** Assez de touches : Netero rit et paie la prime. */
    public static void won(ServerPlayer player) {
        TICKS.remove(player.getUUID());
        drop(player);
        // la balle reste à Netero : seule la prime est gagnée
        QuestData d = QuestManager.data(player);
        d.addJenny(PRIZE);
        player.sendSystemMessage(Component.translatable("exam.hxh.ball_won", PRIZE).withStyle(ChatFormatting.GOLD));
    }

    /** La balle n'est jamais gardée : on la retire de l'inventaire du candidat. */
    public static void takeBack(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
            if (inv.getItem(i).is(ModItems.NETERO_BALL.get())) inv.setItem(i, ItemStack.EMPTY);
    }
}
