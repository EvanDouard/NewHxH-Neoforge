package com.evandouard.hxh.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/** Biomes du monde HxH, tirés de la carte : mer, plages, terre libre et une ambiance par région (lettres de {@code cells.txt}). */
public final class HxHBiomeSource extends BiomeSource {
    /** Biome d'un lieu : disque de {@code radius} blocs autour de (x, z), prioritaire sur la région (terre émergée seulement). */
    public record Place(int x, int z, int radius, Holder<Biome> biome) {
        public static final com.mojang.serialization.Codec<Place> CODEC = RecordCodecBuilder.create(i -> i.group(
                com.mojang.serialization.Codec.INT.fieldOf("x").forGetter(Place::x),
                com.mojang.serialization.Codec.INT.fieldOf("z").forGetter(Place::z),
                com.mojang.serialization.Codec.INT.fieldOf("radius").forGetter(Place::radius),
                Biome.CODEC.fieldOf("biome").forGetter(Place::biome)
        ).apply(i, Place::new));
    }

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
            Biome.CODEC.fieldOf("begerosse").forGetter(s -> s.begerosse),
            Place.CODEC.listOf().optionalFieldOf("places", List.of()).forGetter(s -> s.places)
    ).apply(i, HxHBiomeSource::new));

    private final Holder<Biome> ocean, deepOcean, beach, land, minbo, kukanyu, saherta, kakin, ochima, begerosse;
    private final List<Place> places;

    public HxHBiomeSource(Holder<Biome> ocean, Holder<Biome> deepOcean, Holder<Biome> beach, Holder<Biome> land, Holder<Biome> minbo,
                          Holder<Biome> kukanyu, Holder<Biome> saherta, Holder<Biome> kakin, Holder<Biome> ochima, Holder<Biome> begerosse) {
        this(ocean, deepOcean, beach, land, minbo, kukanyu, saherta, kakin, ochima, begerosse, List.of());
    }

    public HxHBiomeSource(Holder<Biome> ocean, Holder<Biome> deepOcean, Holder<Biome> beach, Holder<Biome> land, Holder<Biome> minbo,
                          Holder<Biome> kukanyu, Holder<Biome> saherta, Holder<Biome> kakin, Holder<Biome> ochima, Holder<Biome> begerosse,
                          List<Place> places) {
        this.places = places;
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
        return Stream.concat(Stream.of(ocean, deepOcean, beach, land, minbo, kukanyu, saherta, kakin, ochima, begerosse),
                places.stream().map(Place::biome));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int x = QuartPos.toBlock(quartX), z = QuartPos.toBlock(quartZ);
        HxHWorldMap map = HxHWorldMap.get();
        char c = map.cellAt(x, z);
        double h = map.height(x, z);
        if (!HxHWorldMap.isLand(c)) return h < 40 ? deepOcean : ocean;
        if (h < HxHWorldMap.SEA_LEVEL + 3) return beach;
        Holder<Biome> special = placeBiome(x, z);
        if (special != null) return special;
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

    /** Biome du lieu qui couvre (x, z), le plus proche en cas de recouvrement ; {@code null} hors de tout lieu. */
    public Holder<Biome> placeBiome(int x, int z) {
        Holder<Biome> best = null;
        long bestD = Long.MAX_VALUE;
        for (Place p : places) {
            long dx = x - p.x, dz = z - p.z, d = dx * dx + dz * dz;
            if (d <= (long) p.radius * p.radius && d < bestD) {
                best = p.biome;
                bestD = d;
            }
        }
        return best;
    }
}
