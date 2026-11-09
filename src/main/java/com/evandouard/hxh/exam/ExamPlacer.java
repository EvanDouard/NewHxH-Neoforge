package com.evandouard.hxh.exam;

import com.evandouard.hxh.npc.NpcEntity;
import com.evandouard.hxh.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * En attendant le port de Dolle : le Navigateur attend les candidats près du point d'apparition du monde
 * (une seule fois par monde). {@code /hunter exam where} dit où il est.
 */
public final class ExamPlacer {
    private ExamPlacer() {}

    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        ExamSavedData d = ExamSavedData.get(overworld);
        if (d.navigator != Long.MIN_VALUE) return;
        BlockPos spawn = overworld.getSharedSpawnPos();
        BlockPos pos = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.offset(6, 0, 4));
        NpcEntity npc = ModEntities.NPC.get().create(overworld);
        if (npc == null) return;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        npc.setNpcId("navigator");
        npc.setHome(pos);
        overworld.addFreshEntity(npc);
        d.navigator = pos.asLong();
        d.setDirty();
    }
}
