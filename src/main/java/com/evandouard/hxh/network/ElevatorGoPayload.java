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
 * Client → serveur : le joueur a choisi une destination dans l'ascenseur (−1 chambre, 0 hall, n tranche). Le serveur revérifie
 * (dans la Tour, pas en combat, étage autorisé) et doit être à côté d'un ascenseur.
 */
public record ElevatorGoPayload(int destination) implements CustomPacketPayload {
    public static final Type<ElevatorGoPayload> TYPE = new Type<>(HxHMod.id("elevator_go"));

    public static final StreamCodec<ByteBuf, ElevatorGoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ElevatorGoPayload::destination,
            ElevatorGoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ElevatorGoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ArenaManager.nearElevator(player)) return;
            String refusal = ArenaManager.goTo(player, payload.destination());
            if (refusal != null) player.sendSystemMessage(Component.translatable(refusal).withStyle(ChatFormatting.RED));
        });
    }
}
