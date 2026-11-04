package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.arena.ArenaManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → serveur : le joueur a choisi un étage dans l'ascenseur. Le serveur revérifie
 * (dans la Tour, pas en combat, étage autorisé) et doit être à côté d'un ascenseur.
 */
public record ElevatorGoPayload(int floor) implements CustomPacketPayload {
    public static final Type<ElevatorGoPayload> TYPE = new Type<>(HxHMod.id("elevator_go"));

    public static final StreamCodec<ByteBuf, ElevatorGoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ElevatorGoPayload::floor,
            ElevatorGoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ElevatorGoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ArenaManager.nearElevator(player)) return;
            String refusal = ArenaManager.goToFloor(player, payload.floor());
            if (refusal != null) player.sendSystemMessage(Component.translatable(refusal).withStyle(ChatFormatting.RED));
        });
    }
}
