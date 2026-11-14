package com.evandouard.hxh.training;

import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Mannequin cogneur : toutes les 1,5 s il frappe ce qui se tient devant lui (n'importe quelle créature).
 * Encaisser entraîne l'Endurance (par les dégâts reçus) ; esquiver — accroupi ou bouclier levé au moment
 * du coup — entraîne la Vitesse. Il ne descend jamais une cible sous 2 cœurs.
 */
public class StrikingDummyBlock extends HorizontalDirectionalBlock {
    public static final int PERIOD = 30;
    public static final float DAMAGE = 2f;
    /** En dessous de cette vie, le mannequin retient son coup. */
    public static final float MIN_HEALTH = 4f;
    public static final double DODGE_XP = 3.0;
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public StrikingDummyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(StrikingDummyBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Il fait face à celui qui le pose. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !oldState.is(this)) level.scheduleTick(pos, this, PERIOD);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        strike(level, pos, state);
        level.scheduleTick(pos, this, PERIOD);
    }

    /** Zone frappée : le bloc devant le mannequin, sur deux de haut. */
    public static AABB strikeZone(BlockPos pos, Direction facing) {
        BlockPos front = pos.relative(facing);
        return new AABB(front).expandTowards(0, 1, 0);
    }

    /** Un coup du mannequin (public pour les tests). Renvoie le nombre de cibles touchées ou esquivées. */
    public static int strike(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, strikeZone(pos, facing),
                e -> e.isAlive() && !e.isSpectator());
        int n = 0;
        for (LivingEntity target : targets) {
            if (target.isShiftKeyDown() || target.isBlocking()) {
                HunterStats.addXp(target, Stat.SPEED, DODGE_XP);
                level.playSound(null, pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.BLOCKS, 0.6f, 1.4f);
                if (target instanceof Player p) {
                    p.displayClientMessage(Component.translatable("message.hxh.striking_dummy.dodge",
                            Component.translatable(Stat.SPEED.translationKey()), DumbbellItem.xpLabel(DODGE_XP))
                            .withStyle(ChatFormatting.AQUA), true);
                }
                n++;
            } else if (target.getHealth() > MIN_HEALTH) {
                target.hurt(level.damageSources().generic(), DAMAGE);
                target.knockback(0.3, -facing.getStepX(), -facing.getStepZ());
                level.playSound(null, pos, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.BLOCKS, 0.8f, 0.8f);
                if (target instanceof Player p) {
                    p.displayClientMessage(Component.translatable("message.hxh.striking_dummy.hit")
                            .withStyle(ChatFormatting.GOLD), true);
                }
                n++;
            }
        }
        return n;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hxh.striking_dummy").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hxh.striking_dummy.dodge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
