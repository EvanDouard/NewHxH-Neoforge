package com.evandouard.hxh.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/** Biomes du monde HxH, tirés de la carte : mer, plages, terre libre et une ambiance par région (lettres de {@code cells.txt}). */
public final class HxHBiomeSource extends BiomeSource {
    public static final MapCodec<HxHBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("ocean").forGetter(s -> s.ocean),
            Biome.CODEC.fieldOf("deep_ocean").forGetter(s -> s.deepOcean),
            Biome.CODEC.fieldOf("beach").forGetter(s -> s.beach),
            Biome.CODEC.fieldOf("land").forGetter(s -> s.land),
            Biome.CODEC.fieldOf("minbo").forGetter(s -> s.minbo),
            Biome.CODEC.fieldOf("kukanyu").forGetter(s -> s.kukanyu),
            Biome.CODEC.fieldOf("saherta").forGetter(s -> s.saherta),
            Biome.CODEC.fieldOf("kakin").forGetter(s -> s.kakin),
            Biome.CODEC.fieldOf("ochima").forGetter(s -> s.ochima),
            Biome.CODEC.fieldOf("begerosse").forGetter(s -> s.begerosse)
    ).apply(i, HxHBiomeSource::new));

    private final Holder<Biome> ocean, deepOcean, beach, land, minbo, kukanyu, saherta, kakin, ochima, begerosse;

    public HxHBiomeSource(Holder<Biome> ocean, Holder<Biome> deepOcean, Holder<Biome> beach, Holder<Biome> land, Holder<Biome> minbo,
                          Holder<Biome> kukanyu, Holder<Biome> saherta, Holder<Biome> kakin, Holder<Biome> ochima, Holder<Biome> begerosse) {
        this.ocean = ocean;
        this.deepOcean = deepOcean;
        this.beach = beach;
        this.land = land;
        this.minbo = minbo;
        this.kukanyu = kukanyu;
        this.saherta = saherta;
        this.kakin = kakin;
        this.ochima = ochima;
        this.begerosse = begerosse;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(ocean, deepOcean, beach, land, minbo, kukanyu, saherta, kakin, ochima, begerosse);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int x = QuartPos.toBlock(quartX), z = QuartPos.toBlock(quartZ);
        HxHWorldMap map = HxHWorldMap.get();
        char c = map.cellAt(x, z);
        double h = map.height(x, z);
        if (!HxHWorldMap.isLand(c)) return h < 40 ? deepOcean : ocean;
        if (h < HxHWorldMap.SEA_LEVEL + 3) return beach;
        return switch (c) {
            case 'm' -> minbo;
            case 'k' -> kukanyu;
            case 's' -> saherta;
            case 'K' -> kakin;
            case 'o' -> ochima;
            case 'b' -> begerosse;
            default -> land;
        };
    }
}
