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
    /** Position (absolue) du Navigateur dans le village de l'île de la Baleine, voir world_places/whale_island_village.json. */
    private static final int VILLAGE_X = 4354, VILLAGE_Z = 4143;

    private ExamPlacer() {}

    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        ExamSavedData d = ExamSavedData.get(overworld);
        // Monde HxH disponible : le Navigateur vit dans le village de l'île de la Baleine (une structure d'Evan le remplacera)
        ServerLevel hxh = event.getServer().getLevel(com.evandouard.hxh.airship.Airship.WORLD);
        if (hxh != null && !d.navigatorHxh) {
            if (placeInHxhWorld(overworld, hxh, d)) return;
        }
        if (d.navigator != Long.MIN_VALUE) return;
        BlockPos spawn = overworld.getSharedSpawnPos();
        BlockPos pos = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.offset(-14, 0, 6));
        NpcEntity npc = ModEntities.NPC.get().create(overworld);
        if (npc == null) return;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        npc.setNpcId("navigator");
        npc.setHome(pos);
        overworld.addFreshEntity(npc);
        d.navigator = pos.asLong();
        d.setDirty();
    }

    /** Pose de village de l'île de la Baleine (si besoin), retrouve le Navigateur qui y est et retire l'ancien, resté dans le monde normal. */
    private static boolean placeInHxhWorld(ServerLevel overworld, ServerLevel hxh, ExamSavedData d) {
        int ax = VILLAGE_X, az = VILLAGE_Z;
        if (!com.evandouard.hxh.world.WorldPlaces.ensureBuilt(hxh, ax, az)) return false;
        var box = new net.minecraft.world.phys.AABB(ax - 6, hxh.getMinBuildHeight(), az - 6, ax + 6, hxh.getMaxBuildHeight(), az + 6);
        NpcEntity guide = null;
        for (NpcEntity n : hxh.getEntitiesOfClass(NpcEntity.class, box)) if (n.npcId().equals("navigator")) guide = n;
        if (guide == null) return false;
        if (d.navigator != Long.MIN_VALUE) {
            BlockPos old = BlockPos.of(d.navigator);
            for (NpcEntity n : overworld.getEntitiesOfClass(NpcEntity.class, new net.minecraft.world.phys.AABB(old).inflate(24, 128, 24)))
                if (n.npcId().equals("navigator")) n.discard();
        }
        d.navigator = guide.blockPosition().asLong();
        d.navigatorHxh = true;
        d.setDirty();
        com.evandouard.hxh.HxHMod.LOGGER.info("[HxH] Le Navigateur attend dans le monde HxH, en {}", guide.blockPosition());
        return true;
    }
}
