package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.world.HxHBiomeSource;
import com.evandouard.hxh.world.HxHTerrainDensity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Types de génération propres au monde HxH (densité du terrain et source de biomes lues depuis la carte). */
public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, HxHMod.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, HxHMod.MODID);

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, HxHMod.MODID);

    static {
        CHUNK_GENERATORS.register("world", () -> com.evandouard.hxh.world.HxHChunkGenerator.CODEC);
        DENSITY_FUNCTIONS.register("world_map", () -> HxHTerrainDensity.CODEC.codec());
        BIOME_SOURCES.register("world_map", () -> HxHBiomeSource.CODEC);
    }

    private ModWorldgen() {}
}
