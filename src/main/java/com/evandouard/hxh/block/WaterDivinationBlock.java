package com.evandouard.hxh.block;

import com.evandouard.hxh.data.HunterData;
import com.evandouard.hxh.data.NenType;
import com.evandouard.hxh.stats.HunterStats;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Verre d'eau avec une feuille : la divination par l'eau révèle le type de Nen.
 * Le résultat dépend du type : l'eau déborde (Renforcement), change de couleur (Émission),
 * des impuretés apparaissent (Matérialisation), la feuille tourne (Manipulation)…
 */
public class WaterDivinationBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<WaterDivinationBlock> CODEC = simpleCodec(WaterDivinationBlock::new);
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 7, 11);

    public WaterDivinationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!level.isClientSide()) divine(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Fait la divination pour ce joueur. Révèle son type s'il maîtrise le Nen.
     * @return le type révélé, ou null si rien ne se passe
     */
    public static NenType divine(Level level, BlockPos pos, Player player) {
        HunterData data = HunterStats.get(player);
        if (!data.nenUnlocked() || data.nenType() == null) {
            player.displayClientMessage(Component.translatable("divination.hxh.nothing"), false);
            return null;
        }
        NenType type = data.nenType();
        data.setNenRevealed(true);
        player.displayClientMessage(Component.translatable("divination.hxh." + type.id()), false);
        player.displayClientMessage(Component.translatable("divination.hxh.result",
                Component.translatable(type.translationKey())), false);

        if (type == NenType.MANIPULATOR) {
            // La feuille bouge : elle tourne d'un quart de tour
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(FACING)) {
                level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
            }
        }
        if (level instanceof ServerLevel server) {
            ParticleOptions particle = switch (type) {
                case ENHANCER -> ParticleTypes.SPLASH;
                case EMITTER -> new DustParticleOptions(new Vector3f(0.9f, 0.2f, 0.6f), 1.2f);
                case CONJURER -> ParticleTypes.WHITE_ASH;
                case TRANSMUTER -> ParticleTypes.ENCHANT;
                case MANIPULATOR -> ParticleTypes.COMPOSTER;
                case SPECIALIST -> ParticleTypes.WITCH;
            };
            server.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    30, 0.15, 0.2, 0.15, 0.02);
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 1f);
        return type;
    }
}
