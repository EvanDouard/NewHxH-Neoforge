package com.evandouard.hxh.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

/** Sauvegarde une zone de n'importe quelle taille en structure (.nbt), sans la limite de 48 du bloc de structure. */
public final class StructureSaver {
    private StructureSaver() {}

    /** @return la taille sauvegardée, ou null si l'écriture du fichier a échoué */
    @Nullable
    public static Vec3i save(ServerLevel level, ResourceLocation id, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        Vec3i size = max.subtract(min).offset(1, 1, 1);
        StructureTemplateManager manager = level.getStructureManager();
        StructureTemplate template = manager.getOrCreate(id);
        // Les blocs « vide de structure » ne sont pas enregistrés (comme avec le bloc de structure)
        template.fillFromWorld(level, min, size, false, Blocks.STRUCTURE_VOID);
        template.setAuthor("hxh");
        return manager.save(id) ? size : null;
    }
}
