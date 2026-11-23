package com.evandouard.hxh.block;

import com.evandouard.hxh.airship.Airship;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Borne du dirigeable : clic droit pour ouvrir la carte du monde et réserver un billet. */
public class AirshipTerminalBlock extends HunterComputerBlock {
    public static final MapCodec<AirshipTerminalBlock> CODEC = simpleCodec(AirshipTerminalBlock::new);

    public AirshipTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5f, 1.0f);
            Airship.open(sp);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
