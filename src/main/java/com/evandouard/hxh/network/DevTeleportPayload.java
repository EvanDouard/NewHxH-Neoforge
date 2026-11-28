package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import com.evandouard.hxh.airship.Airship;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : bouton dev de la carte, « téléporte-moi à ce point du monde » (gratuit). Réservé aux opérateurs : le serveur vérifie la permission et les bornes. */
public record DevTeleportPayload(int x, int z) implements CustomPacketPayload {
    public static final Type<DevTeleportPayload> TYPE = new Type<>(HxHMod.id("dev_teleport"));
    public static final StreamCodec<ByteBuf, DevTeleportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DevTeleportPayload::x,
            ByteBufCodecs.VAR_INT, DevTeleportPayload::z,
            DevTeleportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DevTeleportPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.hxh.airship.dev_denied").withStyle(ChatFormatting.RED));
                return;
            }
            if (!Airship.devTeleport(player, payload.x(), payload.z()))
                player.sendSystemMessage(Component.translatable("message.hxh.airship.dev_out").withStyle(ChatFormatting.RED));
        });
    }
}
