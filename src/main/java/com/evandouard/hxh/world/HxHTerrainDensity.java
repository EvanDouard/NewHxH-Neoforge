package com.evandouard.hxh.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Densité du terrain du monde HxH : positive sous le relief de la carte, négative au-dessus. */
public final class HxHTerrainDensity implements DensityFunction.SimpleFunction {
    public static final HxHTerrainDensity INSTANCE = new HxHTerrainDensity();
    public static final KeyDispatchDataCodec<HxHTerrainDensity> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

    private HxHTerrainDensity() {}

    @Override
    public double compute(FunctionContext context) {
        double h = HxHWorldMap.get().height(context.blockX(), context.blockZ());
        return Mth.clamp((h - context.blockY()) / 6.0, -1.0, 1.0);
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
