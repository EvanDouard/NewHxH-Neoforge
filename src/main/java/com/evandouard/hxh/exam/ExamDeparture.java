package com.evandouard.hxh.exam;

import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.quest.Quest;
import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Point de départ de l'Examen dans le monde : le guide ne téléporte pas, il donne des coordonnées. Le candidat s'y
 * rend à pied (ou comme il veut) ; le passeur qui l'y attend l'embarque pour le lieu de l'épreuve (dimension de
 * l'Examen). Chaque départ est posé pour un joueur, à {@link #MIN_DISTANCE}–{@link #MAX_DISTANCE} blocs du guide.
 * Une structure d'Evan {@code hxh:exam/depart} (avec un repère PNJ « passeur ») remplacera le ponton provisoire.
 */
public final class ExamDeparture {
    private ExamDeparture() {}

    public static final int MIN_DISTANCE = 180, MAX_DISTANCE = 360;
    /** Distance à laquelle le passeur reconnaît le départ du joueur. */
    static final double MATCH = 12;

    /**
     * Pose un départ pour le joueur autour de {@code from} (le guide) et le retient. Renvoie sa position, ou null
     * (tests, pas de monde).
     */
    public static BlockPos create(ServerPlayer player, BlockPos from) {
        QuestData d = QuestManager.data(player);
        if (!ExamTrips.enabled(player)) {
            d.setExamDeparture(from.asLong());
            return from;
        }
        ServerLevel level = player.serverLevel();
        if (ExamWorld.isExamLevel(level)) level = player.server.overworld();
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        int dist = MIN_DISTANCE + player.getRandom().nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
        int x = from.getX() + (int) Math.round(Math.cos(angle) * dist);
        int z = from.getZ() + (int) Math.round(Math.sin(angle) * dist);
        level.getChunk(x >> 4, z >> 4); // génère le terrain pour connaître la surface
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        BlockPos npcPos = build(level, ground);
        NpcEntity npc = ModEntities.NPC.get().create(level);
        if (npc != null) {
            npc.moveTo(npcPos.getX() + 0.5, npcPos.getY(), npcPos.getZ() + 0.5, 0, 0);
            npc.setNpcId("passeur");
            npc.setHome(npcPos);
            level.addFreshEntity(npc);
        }
        d.setExamDeparture(npcPos.asLong());
        return npcPos;
    }

    /** Ponton provisoire : plancher 7 × 7, poteaux, lanternes, fanion ; renvoie la place du passeur. */
    private static BlockPos build(ServerLevel level, BlockPos ground) {
        BlockPos c = ground;
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            ExamWorld.set(level, c.offset(x, -1, z), Blocks.SPRUCE_PLANKS);
            for (int y = 0; y < 4; y++) ExamWorld.set(level, c.offset(x, y, z), Blocks.AIR);
        }
        for (int[] k : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            ExamWorld.set(level, c.offset(k[0], 0, k[1]), Blocks.SPRUCE_FENCE);
            ExamWorld.set(level, c.offset(k[0], 1, k[1]), Blocks.LANTERN);
        }
        for (int y = 0; y < 5; y++) ExamWorld.set(level, c.offset(0, y, -3), Blocks.SPRUCE_LOG);
        ExamWorld.set(level, c.offset(1, 4, -3), Blocks.RED_WOOL);
        ExamWorld.set(level, c.offset(2, 4, -3), Blocks.WHITE_WOOL);
        return c;
    }

    /** Le joueur parle au passeur : s'il est à son point de départ, il part pour le lieu de l'épreuve en cours. */
    public static boolean board(ServerPlayer player, NpcEntity passeur) {
        QuestData d = QuestManager.data(player);
        long dep = d.examDeparture();
        if (dep == Long.MIN_VALUE || BlockPos.of(dep).distSqr(passeur.home()) > MATCH * MATCH) {
            player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                    Component.translatable("npc.hxh.passeur").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("dialog.hxh.passeur.not_yours")));
            return false;
        }
        ExamSite site = pendingSite(d);
        if (site == null) return false;
        player.sendSystemMessage(Component.translatable("dialog.hxh.format",
                Component.translatable("npc.hxh.passeur").withStyle(ChatFormatting.YELLOW),
                Component.translatable("dialog.hxh.passeur.board", Component.translatable("exam.hxh.site." + site.id()))));
        int instance = d.examInstance(site.id());
        d.markExamVisited(site.id(), instance);
        d.setExamDeparture(Long.MIN_VALUE);
        ExamTrips.sendTo(player, site, instance);
        QuestManager.sync(player);
        return true;
    }

    /** Lieu de l'épreuve qui attend le joueur : celui de l'étape en cours (ou de la suivante qui en a un). */
    public static ExamSite pendingSite(QuestData d) {
        Quest q = com.evandouard.hxh.quest.QuestRegistry.get(com.evandouard.hxh.quest.QuestRegistry.HUNTER_EXAM);
        int[] st = d.stateOf(com.evandouard.hxh.quest.QuestRegistry.HUNTER_EXAM.toString());
        if (q == null || st == null) return null;
        List<Quest.Step> steps = QuestManager.steps(d, q);
        for (int i = st[0]; i < steps.size(); i++) {
            ExamSite s = ExamSite.byId(steps.get(i).site());
            if (s != null) return s;
        }
        return null;
    }
}
