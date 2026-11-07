package com.evandouard.hxh.training;

import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Haltères : maintenir le clic droit. Chaque seconde d'effort = une répétition qui rapporte de l'XP de Force
 * (selon la gamme) et fatigue un peu (faim).
 */
public class DumbbellItem extends Item {
    /** Durée d'une répétition, en ticks. */
    public static final int REP_TICKS = 20;
    /** XP de Force par répétition avec des haltères I. */
    public static final double BASE_XP = 3.0;

    private final TrainingTier tier;

    public DumbbellItem(TrainingTier tier, Properties properties) {
        super(properties.stacksTo(1).rarity(tier.rarity()));
        this.tier = tier;
    }

    public TrainingTier tier() {
        return tier;
    }

    public double xpPerRep() {
        return Math.round(BASE_XP * tier.multiplier());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide()) return;
        int elapsed = getUseDuration(stack, entity) - remainingUseDuration;
        if (elapsed <= 0 || elapsed % REP_TICKS != 0) return;
        HunterStats.addXp(entity, Stat.STRENGTH, xpPerRep());
        if (entity instanceof Player player) {
            player.causeFoodExhaustion(0.4f);
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("message.hxh.training.rep", elapsed / REP_TICKS,
                        Component.translatable("stat.hxh." + Stat.STRENGTH.id()), xpLabel(xpPerRep())).withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    static String xpLabel(double xp) {
        return xp == Math.floor(xp) ? String.valueOf((int) xp) : String.format(java.util.Locale.ROOT, "%.1f", xp).replace('.', ',');
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hxh.dumbbell", xpLabel(xpPerRep())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(tier.sourceKey()).withStyle(tier.color()));
    }
}
