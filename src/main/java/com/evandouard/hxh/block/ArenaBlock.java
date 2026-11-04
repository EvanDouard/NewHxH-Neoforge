package com.evandouard.hxh.block;

import com.evandouard.hxh.arena.ArenaManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Blocs de la Tour Céleste : le portail (dans le monde, pour entrer), l'ascenseur (lance un combat)
 * et la sortie du hall. L'ascenseur ouvre le choix de l'étage. Un clic droit suffit.
 */
public class ArenaBlock extends Block {
    public enum Kind { GATE, ELEVATOR, EXIT }

    private final Kind kind;

    public ArenaBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(p -> new ArenaBlock(kind, p));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        switch (kind) {
            case GATE -> ArenaManager.enter(sp);
            case ELEVATOR -> ArenaManager.useElevator(sp);
            case EXIT -> ArenaManager.leave(sp);
        }
        return InteractionResult.CONSUME;
    }
}
