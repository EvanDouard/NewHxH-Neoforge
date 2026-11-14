package com.evandouard.hxh.training;

import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Bain glacé : rester dedans entraîne la Volonté (+2 XP par seconde). Le froid gagne le corps
 * (givre à l'écran, ralentissement) sans jamais faire de dégâts. Marche pour toute créature.
 */
public class IceBathBlock extends Block {
    public static final double XP_PER_SECOND = 2.0;
    private static final VoxelShape SHAPE = Shapes.join(Block.box(0, 0, 0, 16, 10, 16),
            Block.box(2, 3, 2, 14, 10, 14), BooleanOp.ONLY_FIRST);

    public IceBathBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) soak(living);
    }

    /** Un tick dans le bain (public pour les tests). */
    public static void soak(LivingEntity entity) {
        if (entity.isSpectator()) return;
        if (entity.canFreeze()) {
            // le givre monte (le jeu en retire 2 par tick hors neige poudreuse) mais s'arrête juste avant les dégâts
            int cap = entity.getTicksRequiredToFreeze() - 1;
            entity.setTicksFrozen(Math.min(cap, entity.getTicksFrozen() + 4));
        }
        if (entity.tickCount % 20 != 0) return;
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
        HunterStats.addXp(entity, Stat.WILLPOWER, XP_PER_SECOND);
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.hxh.ice_bath",
                    Component.translatable(Stat.WILLPOWER.translationKey()), DumbbellItem.xpLabel(XP_PER_SECOND))
                    .withStyle(ChatFormatting.AQUA), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hxh.ice_bath", DumbbellItem.xpLabel(XP_PER_SECOND)).withStyle(ChatFormatting.GRAY));
    }
}
