package com.evandouard.hxh.meditation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Pierre de méditation (niveaux 1 à 5), au cœur d'un sanctuaire : méditer à côté entraîne bien plus la Volonté. */
public class MeditationStoneBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);
    private final int tier;

    public MeditationStoneBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hxh.meditation_stone", (int) MeditationShrines.WILL_MULT[tier - 1])
                .withStyle(ChatFormatting.GRAY));
    }
}
