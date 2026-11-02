package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.block.WaterDivinationBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HxHMod.MODID);

    public static final DeferredBlock<WaterDivinationBlock> WATER_DIVINATION = BLOCKS.register("water_divination",
            () -> new WaterDivinationBlock(BlockBehaviour.Properties.of()
                    .strength(0.3f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    private ModBlocks() {}
}
