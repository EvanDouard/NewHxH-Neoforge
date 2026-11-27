package com.evandouard.hxh.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Générateur du monde HxH : le terrain vient de la carte, comme un générateur « noise » ordinaire, mais
 * aucune structure vanilla n'est créée (villages de villageois, forteresses, épaves, portails en ruine…).
 * Les lieux du monde HxH sont posés par {@link WorldPlaces}, pas par le système de structures.
 */
public final class HxHChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<HxHChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(HxHChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(HxHChunkGenerator::generatorSettings)
    ).apply(i, i.stable(HxHChunkGenerator::new)));

    public HxHChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState, StructureManager structureManager,
                                 ChunkAccess chunk, StructureTemplateManager templateManager) {
        // Volontairement vide : pas de structures vanilla dans le monde HxH.
    }
}
