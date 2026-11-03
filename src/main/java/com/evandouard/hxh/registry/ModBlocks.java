package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.block.ArenaBlock;
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

    /** Portail de la Tour Céleste, à poser dans le monde (en attendant la vraie tour). */
    public static final DeferredBlock<ArenaBlock> HEAVENS_ARENA_GATE = BLOCKS.register("heavens_arena_gate",
            () -> new ArenaBlock(ArenaBlock.Kind.GATE, BlockBehaviour.Properties.of()
                    .strength(3f).sound(SoundType.METAL).lightLevel(s -> 10)));
    public static final DeferredBlock<ArenaBlock> ARENA_ELEVATOR = BLOCKS.register("arena_elevator",
            () -> new ArenaBlock(ArenaBlock.Kind.ELEVATOR, BlockBehaviour.Properties.of()
                    .strength(-1f, 3600000f).sound(SoundType.METAL).noLootTable()));
    public static final DeferredBlock<ArenaBlock> ARENA_EXIT = BLOCKS.register("arena_exit",
            () -> new ArenaBlock(ArenaBlock.Kind.EXIT, BlockBehaviour.Properties.of()
                    .strength(-1f, 3600000f).sound(SoundType.WOOD).noLootTable()));

    private ModBlocks() {}
}
