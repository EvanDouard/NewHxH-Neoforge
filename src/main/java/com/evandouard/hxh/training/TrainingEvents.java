package com.evandouard.hxh.training;

import com.evandouard.hxh.data.Stat;
import com.evandouard.hxh.stats.HunterStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tapis de course : pousse le joueur vers l'arrière et compte ses secondes de course. */
public final class TrainingEvents {
    private static final Map<UUID, Integer> RUN_TICKS = new HashMap<>();

    private TrainingEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(PlayerTickEvent.Post.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) tickTreadmill(p);
        });
    }

    /** Un tick de tapis pour ce joueur (public pour les tests). */
    public static void tickTreadmill(ServerPlayer player) {
        BlockState state = player.level().getBlockState(player.getOnPos());
        if (!(state.getBlock() instanceof TreadmillBlock treadmill) || player.isSpectator() || !player.onGround()) {
            RUN_TICKS.remove(player.getUUID());
            return;
        }
        // Le tapis emporte le joueur à l'opposé de sa direction de course
        Direction back = state.getValue(TreadmillBlock.FACING).getOpposite();
        double speed = treadmill.beltSpeed();
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x + back.getStepX() * speed, v.y, v.z + back.getStepZ() * speed);
        player.hurtMarked = true;

        TreadmillBlock.Mode mode = state.getValue(TreadmillBlock.MODE);
        boolean running = mode == TreadmillBlock.Mode.SPEED ? player.isSprinting() : !player.isSprinting();
        if (!running) {
            RUN_TICKS.remove(player.getUUID());
            return;
        }
        int ticks = RUN_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (ticks % 20 != 0) return;
        Stat stat = mode == TreadmillBlock.Mode.SPEED ? Stat.SPEED : Stat.ENDURANCE;
        HunterStats.addXp(player, stat, treadmill.xpPerSecond());
        player.causeFoodExhaustion(mode == TreadmillBlock.Mode.SPEED ? 0.5f : 0.3f);
        player.displayClientMessage(Component.translatable("message.hxh.training.run", ticks / 20,
                Component.translatable("stat.hxh." + stat.id()), DumbbellItem.xpLabel(treadmill.xpPerSecond()))
                .withStyle(ChatFormatting.GOLD), true);
    }
}
