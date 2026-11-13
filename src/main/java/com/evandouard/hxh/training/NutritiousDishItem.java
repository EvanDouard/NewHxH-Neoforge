package com.evandouard.hxh.training;

import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Plat nutritif (gammes I à IV) : un vrai repas de Hunter, façon cuisine de Menchi. Le manger rapporte de l'XP de
 * Vitalité. Il nourrit beaucoup : on ne peut en manger qu'avec de la faim, ce qui limite l'entraînement à quelques
 * plats par jour de jeu. Gamme III : vendue par le Hunter gourmet ; gamme IV : sa quête du plat légendaire et les
 * enchères de York Shin.
 */
public class NutritiousDishItem extends Item {
    /** XP de Vitalité d'un plat I. */
    public static final double BASE_XP = 25.0;

    private final TrainingTier tier;

    public NutritiousDishItem(TrainingTier tier, Properties properties) {
        super(properties.stacksTo(16).rarity(tier.rarity()).food(food(tier)));
        this.tier = tier;
    }

    private static FoodProperties food(TrainingTier tier) {
        return new FoodProperties.Builder()
                .nutrition(6 + tier.number() * 2)
                .saturationModifier(0.6f + 0.1f * tier.number())
                .usingConvertsTo(Items.BOWL)
                .build();
    }

    public TrainingTier tier() {
        return tier;
    }

    public double xpPerDish() {
        return Math.round(BASE_XP * tier.multiplier());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            HunterStats.addXp(entity, Stat.VITALITY, xpPerDish());
            if (entity instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("message.hxh.training.dish",
                        Component.translatable("stat.hxh." + Stat.VITALITY.id()), DumbbellItem.xpLabel(xpPerDish()))
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hxh.dish", DumbbellItem.xpLabel(xpPerDish())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(tier == TrainingTier.III || tier == TrainingTier.IV
                ? "tooltip.hxh.dish.source_gourmet" : tier.sourceKey()).withStyle(tier.color()));
    }
}
