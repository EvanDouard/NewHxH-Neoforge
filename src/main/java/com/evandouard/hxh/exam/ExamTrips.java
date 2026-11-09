package com.evandouard.hxh.exam;

import com.evandouard.hxh.quest.QuestData;
import com.evandouard.hxh.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Déplacements de l'Examen : d'un lieu à l'autre, et retour à l'endroit d'où l'on est parti (fin ou échec).
 */
public final class ExamTrips {
    /** Les tests en jeu restent sur place (les joueurs factices ne changent pas de dimension) sauf demande. */
    public static boolean forceInTests = false;

    private ExamTrips() {}

    private static boolean enabled(ServerPlayer player) {
        return forceInTests || !(player.server instanceof net.minecraft.gametest.framework.GameTestServer);
    }

    /** Emmène le joueur à l'arrivée du lieu (le pose d'abord s'il n'existe pas). */
    public static boolean sendTo(ServerPlayer player, ExamSite site) {
        if (!enabled(player)) return false;
        ServerLevel level = ExamWorld.level(player.server);
        if (level == null) return false;
        QuestData d = QuestManager.data(player);
        // On retient d'où l'on vient la première fois qu'on entre dans l'Examen
        if (!ExamWorld.isExamLevel(player.level())) {
            d.setExamReturn(player.level().dimension().location().toString(), player.blockPosition().asLong());
        }
        ExamWorld.Site s = ExamWorld.ensure(level, site);
        if (site == ExamSite.BISKA) ExamWorld.restockPrey(level);
        BlockPos a = s.arrival();
        player.teleportTo(level, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, s.yaw(), 0f);
        player.displayClientMessage(Component.translatable("exam.hxh.site." + site.id()), true);
        return true;
    }

    /** Ramène le joueur là d'où il était parti, s'il est dans l'Examen. */
    public static void returnHome(ServerPlayer player) {
        if (!enabled(player) || !ExamWorld.isExamLevel(player.level())) return;
        QuestData d = QuestManager.data(player);
        ResourceLocation dim = ResourceLocation.tryParse(d.examReturnDimension());
        ServerLevel target = dim == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (target == null || ExamWorld.isExamLevel(target)) target = player.server.overworld();
        BlockPos p = d.examReturnPos() == Long.MIN_VALUE ? target.getSharedSpawnPos() : BlockPos.of(d.examReturnPos());
        player.teleportTo(target, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, player.getYRot(), 0f);
    }

    public static boolean inExam(Level level) {
        return ExamWorld.isExamLevel(level);
    }
}
