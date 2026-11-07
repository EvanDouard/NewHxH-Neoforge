package com.evandouard.hxh.registry;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.marker.MarkerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HxHMod.MODID);

    /** Repères réglables (PNJ, points de parcours). */
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MarkerBlockEntity>> MARKER =
            BLOCK_ENTITIES.register("marker", () -> BlockEntityType.Builder.of(MarkerBlockEntity::new,
                    ModBlocks.MARKER_NPC_CHOICE.get(), ModBlocks.MARKER_WAYPOINT.get()).build(null));

    private ModBlockEntities() {}
}
