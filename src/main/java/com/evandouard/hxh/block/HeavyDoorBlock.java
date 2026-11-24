package com.evandouard.hxh.block;

import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Les sept portes de l'épreuve du domaine Zoldyck : la porte n pèse 2^n tonnes. Un clic droit la pousse si la Force du joueur suffit
 * (toute la plaque s'ouvre), puis elle se referme seule. Pousser une porte entraîne la Force.
 */
public class HeavyDoorBlock extends Block {
    public static final IntegerProperty DOOR = IntegerProperty.create("door", 1, 7);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    /** Niveau de Force nécessaire pour la porte n (index n - 1). */
    public static final int[] STRENGTH_NEEDED = {4, 10, 18, 28, 42, 60, 80};
    public static final int CLOSE_AFTER_TICKS = 100;
    private static final int MAX_PANEL = 80;

    public HeavyDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DOOR, 1).setValue(OPEN, false));
    }

    /** Poids affiché de la porte n, en tonnes. */
    public static int tons(int door) {
        return 1 << door;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOOR, OPEN);
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (state.getValue(OPEN)) return InteractionResult.CONSUME;
        int door = state.getValue(DOOR);
        int needed = STRENGTH_NEEDED[door - 1];
        int strength = HunterStats.get(player).level(Stat.STRENGTH);
        if (strength < needed) {
            player.displayClientMessage(Component.translatable("message.hxh.heavy_door.too_heavy", door, tons(door), strength, needed), true);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.5f);
            return InteractionResult.CONSUME;
        }
        setPanel((ServerLevel) level, pos, door, true);
        level.scheduleTick(pos, this, CLOSE_AFTER_TICKS);
        player.displayClientMessage(Component.translatable("message.hxh.heavy_door.opened", door, tons(door)), true);
        HunterStats.addXp(player, Stat.STRENGTH, door * 4);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this) || !state.getValue(OPEN)) return;
        // quelqu'un dans l'encadrement : on attend avant de refermer
        AABB zone = new AABB(pos).inflate(1.0, 1.0, 1.0);
        if (!level.getEntitiesOfClass(LivingEntity.class, zone).isEmpty()) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        setPanel(level, pos, state.getValue(DOOR), false);
    }

    /** Ouvre ou ferme toute la plaque (blocs contigus de la même porte). */
    private void setPanel(ServerLevel level, BlockPos start, int door, boolean open) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() <= MAX_PANEL) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!s.is(this) || s.getValue(DOOR) != door) continue;
            level.setBlock(p, s.setValue(OPEN, open), Block.UPDATE_ALL);
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }
        level.playSound(null, start, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.4f);
    }
}
