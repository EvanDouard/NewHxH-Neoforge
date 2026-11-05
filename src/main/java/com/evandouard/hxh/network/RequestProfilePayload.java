package com.evandouard.hxh.network;

import com.evandouard.hxh.HxHMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → serveur : le joueur ouvre son carnet de Hunter ; le serveur répond avec {@link ProfilePayload}. */
public record RequestProfilePayload() implements CustomPacketPayload {
    public static final RequestProfilePayload INSTANCE = new RequestProfilePayload();
    public static final Type<RequestProfilePayload> TYPE = new Type<>(HxHMod.id("request_profile"));
    public static final StreamCodec<ByteBuf, RequestProfilePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestProfilePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) HxHNetwork.sendTo(p, ProfilePayload.of(p));
        });
    }
}
