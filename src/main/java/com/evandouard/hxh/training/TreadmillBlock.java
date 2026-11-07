package com.evandouard.hxh.training;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Tapis de course : il entraîne la Vitesse (en sprintant) ou l'Endurance (en marchant), jamais les deux.
 * Clic droit = changer de mode. Le tapis pousse vers l'arrière : il faut courir face à lui pour rester dessus.
 * Orienté vers le joueur qui le pose (il court dans le sens où il regardait).
 */
public class TreadmillBlock extends HorizontalDirectionalBlock {
    public enum Mode implements StringRepresentable {
        SPEED("speed"), ENDURANCE("endurance");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);
    /** XP par seconde de course avec un tapis I. */
    public static final double BASE_XP = 2.0;

    private final TrainingTier tier;

    public TreadmillBlock(TrainingTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.SPEED));
    }

    public TrainingTier tier() {
        return tier;
    }

    public double xpPerSecond() {
        return Math.round(BASE_XP * tier.multiplier());
    }

    /** Vitesse du tapis (blocs par tick) : un peu plus rapide sur les gammes hautes. */
    public double beltSpeed() {
        return 0.11 + 0.02 * (tier.number() - 1);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(p -> new TreadmillBlock(tier, p));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Mode next = state.getValue(MODE) == Mode.SPEED ? Mode.ENDURANCE : Mode.SPEED;
        level.setBlock(pos, state.setValue(MODE, next), 3);
        player.displayClientMessage(Component.translatable("message.hxh.treadmill.mode_" + next.getSerializedName())
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hxh.treadmill", DumbbellItem.xpLabel(xpPerSecond())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hxh.treadmill.mode").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(tier.sourceKey()).withStyle(tier.color()));
    }
}
