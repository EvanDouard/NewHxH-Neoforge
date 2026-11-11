package com.evandouard.hxh.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Faux mur de la Tour de Trick : il ressemble aux pierres de la tour, mais un clic droit le fait disparaître quelques
 * secondes, le temps de passer au travers. Il revient tout seul (pas tant que quelqu'un est dedans). Les blocs voisins
 * du même genre s'ouvrent ensemble (une porte de deux blocs de haut s'ouvre d'un seul clic).
 */
public class IllusionWallBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** Durée d'ouverture (ticks). */
    public static final int OPEN_TICKS = 70;

    public IllusionWallBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(OPEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(OPEN)) return InteractionResult.PASS;
        if (!level.isClientSide()) open(level, pos, 0);
        return InteractionResult.SUCCESS;
    }

    /** Ouvre ce bloc et ses voisins faux murs (jusqu'à 6 blocs d'un même passage). */
    public static void open(Level level, BlockPos pos, int depth) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof IllusionWallBlock) || st.getValue(OPEN) || depth > 6) return;
        level.setBlock(pos, st.setValue(OPEN, true), 3);
        level.scheduleTick(pos, st.getBlock(), OPEN_TICKS);
        if (depth == 0) level.playSound(null, pos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.BLOCKS, 0.8f, 1.2f);
        for (BlockPos n : new BlockPos[]{pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()})
            open(level, n, depth + 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN)) return;
        // quelqu'un est encore dans le passage : on attend
        if (!level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(pos)).isEmpty()) {
            level.scheduleTick(pos, this, 10);
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, false), 3);
    }
}
