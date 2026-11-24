package com.evandouard.hxh.beast;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Mike, le chien géant du domaine Zoldyck : il garde la montagne et attaque tout intrus. Il reste près de son poste.
 * On l'apaise en lui donnant de la viande : après trois offrandes, il considère le joueur comme un ami et ne l'attaque plus
 * (comme Gon et Killua). Modèle provisoire : loup agrandi ×3.
 */
public class MikeEntity extends Wolf {
    public static final String FRIEND_TAG = "hxh_mike_friend";
    private static final String FED_TAG = "hxh_mike_fed";
    public static final int OFFERINGS_FOR_FRIENDSHIP = 3;
    private static final int GUARD_RADIUS = 48;

    public MikeEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 150)
                .add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.SCALE, 3.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                p -> p instanceof Player pl && !pl.getPersistentData().getBoolean(FRIEND_TAG)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount == 1 && !hasRestriction()) restrictTo(blockPosition(), GUARD_RADIUS);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ItemTags.MEAT)) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player.getPersistentData().getBoolean(FRIEND_TAG)) return InteractionResult.CONSUME;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        heal(10f);
        if (getTarget() == player) setTarget(null);
        int fed = player.getPersistentData().getInt(FED_TAG) + 1;
        player.getPersistentData().putInt(FED_TAG, fed);
        if (fed >= OFFERINGS_FOR_FRIENDSHIP) {
            player.getPersistentData().putBoolean(FRIEND_TAG, true);
            if (player instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("message.hxh.mike.friend"), false);
        } else if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("message.hxh.mike.calmer", fed, OFFERINGS_FOR_FRIENDSHIP), true);
        }
        return InteractionResult.CONSUME;
    }
}
